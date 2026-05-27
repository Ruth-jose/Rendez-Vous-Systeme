package ht.rendezvous.gesifid.services;

import ht.rendezvous.gesifid.config.DatabaseConfig;
import ht.rendezvous.gesifid.dao.ClientDAO;
import ht.rendezvous.gesifid.dao.ProduitDAO;
import ht.rendezvous.gesifid.dao.VenteDAO;
import ht.rendezvous.gesifid.models.Client;
import ht.rendezvous.gesifid.models.LigneVente;
import ht.rendezvous.gesifid.models.Produit;
import ht.rendezvous.gesifid.models.Vente;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class VenteService {

    private final ProduitDAO produitDAO = new ProduitDAO();
    private final ClientDAO clientDAO = new ClientDAO();
    private final VenteDAO venteDAO = new VenteDAO();
    private final FideliteService fideliteService = new FideliteService();
    private final ImpressionService impressionService = new ImpressionService();

    /**
     * Effectue une transaction de vente complète avec les garanties ACID.
     * Met à jour les stocks, enregistre la vente, met à jour les dépenses du client,
     * et imprime le ticket thermique.
     * 
     * @param vente Objet Vente contenant les lignes
     * @return Vente enregistrée avec son ID de base de données
     * @throws SQLException Si une contrainte SQL (ex. rupture de stock) ou d'accès échoue.
     */
    public Vente enregistrerEtFacturer(Vente vente) throws SQLException {
        Connection conn = DatabaseConfig.getConnection();
        boolean initialAutoCommit = conn.getAutoCommit();

        try {
            // 1. Démarrer la transaction SQL
            conn.setAutoCommit(false);

            double totalBrut = 0.0;
            
            // 2. Vérifier et déduire les stocks de chaque produit
            for (LigneVente ligne : vente.getLignesVente()) {
                Produit produit = produitDAO.findById(ligne.getProduitId());
                if (produit == null) {
                    throw new SQLException("Produit inexistant (ID: " + ligne.getProduitId() + ").");
                }

                if (produit.getQuantiteStock() < ligne.getQuantite()) {
                    throw new SQLException("Rupture de stock pour l'article : " + produit.getNom() + 
                                           " (Disponible: " + produit.getQuantiteStock() + ", Demandé: " + ligne.getQuantite() + ").");
                }

                // Déduire le stock
                int nouveauStock = produit.getQuantiteStock() - ligne.getQuantite();
                produitDAO.updateStock(produit.getId(), nouveauStock);

                // Fixer le prix unitaire d'achat facturé sur cette ligne
                ligne.setPrixUnitaireFacture(produit.getPrixUnitaire());
                ligne.setNomProduit(produit.getNom());
                totalBrut += ligne.getQuantite() * ligne.getPrixUnitaireFacture();
            }

            vente.setTotalBrut(totalBrut);

            // 3. Appliquer la remise de fidélité si un client est enregistré
            double tauxRemise = 0.0;
            String nomClient = null;
            if (vente.getClientId() != null) {
                Client client = clientDAO.findById(vente.getClientId());
                if (client != null) {
                    nomClient = client.getNomComplet();
                    tauxRemise = fideliteService.calculerPourcentageRemise(client.getId());
                    double montantRemise = totalBrut * tauxRemise;
                    vente.setMontantRemise(montantRemise);
                    
                    // Mettre à jour l'historique d'achat cumulé du client
                    clientDAO.addToCumulativePurchases(client.getId(), totalBrut - montantRemise);
                }
            } else {
                vente.setMontantRemise(0.0);
            }

            // Calculer le total net de la facture
            vente.setTotalNet(totalBrut - vente.getMontantRemise());
            vente.setDateVente(LocalDateTime.now());

            // 4. Insérer l'entête de la vente
            venteDAO.insert(vente, conn);

            // 5. Insérer les lignes de la vente
            for (LigneVente ligne : vente.getLignesVente()) {
                ligne.setVenteId(vente.getId());
                venteDAO.insertLigne(ligne, conn);
            }

            // 6. Valider (Commit) de la transaction
            conn.commit();

            // 7. Lancement asynchrone de l'impression physique du ticket de caisse
            try {
                String ticketTexte = impressionService.genererTexteTicket(vente, nomClient, tauxRemise);
                impressionService.imprimerTicket(ticketTexte);
            } catch (Exception printException) {
                // L'échec de l'imprimante ne doit PAS annuler la vente en base de données.
                System.err.println("ERREUR IMPRESSION (Non bloquante) : " + printException.getMessage());
            }

            return vente;

        } catch (SQLException e) {
            // Annuler la transaction en cas d'erreur de stock ou d'écriture DB
            conn.rollback();
            throw e;
        } finally {
            // Restaurer l'état de l'auto-commit
            conn.setAutoCommit(initialAutoCommit);
        }
    }
}

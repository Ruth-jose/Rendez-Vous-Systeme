package ht.rendezvous.gesifid.services;

import ht.rendezvous.gesifid.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class FideliteService {

    /**
     * Calcule le pourcentage de remise éligible pour un client donné.
     * @param clientId ID du client en base
     * @return Taux de remise (ex: 0.05 pour 5%)
     */
    public double calculerPourcentageRemise(int clientId) {
        double totalAchats = 0.0;
        int sacsCimentTrimestre = 0;

        String queryAchats = "SELECT total_achats_cumules FROM client WHERE id = ?";
        
        // Calcule le volume cumulé d'achat de ciment au cours des 3 derniers mois
        String queryCiment = "SELECT SUM(lv.quantite) FROM ligne_vente lv " +
                             "JOIN vente v ON lv.vente_id = v.id " +
                             "JOIN produit p ON lv.produit_id = p.id " +
                             "WHERE v.client_id = ? " +
                             "AND p.nom ILIKE '%ciment%' " +
                             "AND v.date_vente >= CURRENT_DATE - INTERVAL '3 months'";

        try (Connection conn = DatabaseConfig.getConnection()) {
            
            // 1. Récupérer le montant cumulé historique
            try (PreparedStatement stmt = conn.prepareStatement(queryAchats)) {
                stmt.setInt(1, clientId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        totalAchats = rs.getDouble("total_achats_cumules");
                    }
                }
            }

            // 2. Récupérer la quantité de sacs de ciment
            try (PreparedStatement stmt = conn.prepareStatement(queryCiment)) {
                stmt.setInt(1, clientId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        sacsCimentTrimestre = rs.getInt(1);
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("Erreur lors du calcul de la fidélité : " + e.getMessage());
        }

        // Application des règles strictes définies dans le cahier des charges
        if (totalAchats >= 500000.0 || sacsCimentTrimestre >= 100) {
            return 0.05; // Palier 3 : 5% de remise
        } else if (totalAchats >= 150000.0) {
            return 0.02; // Palier 2 : 2% de remise
        }
        
        return 0.0; // Palier 1 : Aucun rabais (0%)
    }
}

package ht.rendezvous.gesifid.controllers;

import ht.rendezvous.gesifid.dao.ClientDAO;
import ht.rendezvous.gesifid.dao.ProduitDAO;
import ht.rendezvous.gesifid.models.Client;
import ht.rendezvous.gesifid.models.LigneVente;
import ht.rendezvous.gesifid.models.Produit;
import ht.rendezvous.gesifid.models.Vente;
import ht.rendezvous.gesifid.services.FideliteService;
import ht.rendezvous.gesifid.services.VenteService;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;

import java.sql.SQLException;

public class CaisseController {

    @FXML private TextField txtClientTel;
    @FXML private Label lblClientInfo;
    @FXML private TextField txtCodeBarre;
    @FXML private TextField txtQuantite;

    @FXML private TableView<LigneVente> tablePanier;
    @FXML private TableColumn<LigneVente, String> colCode;
    @FXML private TableColumn<LigneVente, String> colNom;
    @FXML private TableColumn<LigneVente, Double> colPrix;
    @FXML private TableColumn<LigneVente, Integer> colQuantite;
    @FXML private TableColumn<LigneVente, Double> colTotalLigne;

    @FXML private Label lblTotalBrut;
    @FXML private Label lblMontantRemise;
    @FXML private Label lblTotalNet;
    @FXML private ComboBox<String> comboPaiement;

    private final ProduitDAO produitDAO = new ProduitDAO();
    private final ClientDAO clientDAO = new ClientDAO();
    private final VenteService venteService = new VenteService();
    private final FideliteService fideliteService = new FideliteService();

    private final ObservableList<LigneVente> panier = FXCollections.observableArrayList();
    private Client clientSelectionne = null;

    @FXML
    public void initialize() {
        // Configuration des colonnes du panier
        colCode.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCodeProduitHelper()));
        colNom.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getNomProduit()));
        colPrix.setCellValueFactory(cell -> new SimpleDoubleProperty(cell.getValue().getPrixUnitaireFacture()).asObject());
        colQuantite.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getQuantite()).asObject());
        colTotalLigne.setCellValueFactory(cell -> new SimpleDoubleProperty(cell.getValue().getTotalLigne()).asObject());

        tablePanier.setItems(panier);

        // Formater les colonnes monétaires
        colPrix.setCellFactory(tc -> formatDoubleColumn());
        colTotalLigne.setCellFactory(tc -> formatDoubleColumn());

        // Configurer les modes de paiement
        comboPaiement.setItems(FXCollections.observableArrayList("CASH", "MONCASH", "VIREMENT", "CHEQUE"));
        comboPaiement.setValue("CASH");

        // Raccourci clavier sur la scène pour la touche F5
        txtCodeBarre.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.setOnKeyPressed(event -> {
                    if (event.getCode() == KeyCode.F5) {
                        validerEtImprimerAchat(null);
                    }
                });
            }
        });
    }

    @FXML
    void rechercherClient(ActionEvent event) {
        String tel = txtClientTel.getText().trim();
        if (tel.isEmpty()) {
            lblClientInfo.setText("Saisissez un numéro de téléphone.");
            return;
        }

        try {
            Client c = clientDAO.findByTelephone(tel);
            if (c != null) {
                clientSelectionne = c;
                double taux = fideliteService.calculerPourcentageRemise(c.getId());
                lblClientInfo.setText("Client : " + c.getNomComplet() + " (" + String.format("%.0f", taux * 100) + "% de remise)");
                lblClientInfo.setStyle("-fx-text-fill: #4ade80; -fx-font-weight: bold;");
                recalculerTotaux();
            } else {
                lblClientInfo.setText("Client introuvable.");
                lblClientInfo.setStyle("-fx-text-fill: #f87171; -fx-font-weight: bold;");
                clientSelectionne = null;
                recalculerTotaux();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            afficherAlerte("Erreur", "Impossible de chercher le client : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    void definirClientAnonyme(ActionEvent event) {
        clientSelectionne = null;
        txtClientTel.clear();
        lblClientInfo.setText("Client sélectionné : COMPTOIR (0% de remise)");
        lblClientInfo.setStyle("-fx-text-fill: #38bdf8; -fx-font-weight: bold;");
        recalculerTotaux();
    }

    @FXML
    void ajouterProduitParCode(ActionEvent event) {
        ajouterProduitPanier(null);
    }

    @FXML
    void ajouterProduitPanier(ActionEvent event) {
        String code = txtCodeBarre.getText().trim();
        String qteStr = txtQuantite.getText().trim();

        if (code.isEmpty()) return;

        int qte = 1;
        try {
            qte = Integer.parseInt(qteStr);
            if (qte <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            afficherAlerte("Quantité invalide", "Veuillez saisir une quantité entière positive.", Alert.AlertType.WARNING);
            return;
        }

        try {
            Produit produit = produitDAO.findByCodeBarre(code);
            if (produit == null) {
                afficherAlerte("Produit introuvable", "Aucun produit avec le code-barre " + code, Alert.AlertType.WARNING);
                return;
            }

            if (produit.getQuantiteStock() < qte) {
                afficherAlerte("Stock insuffisant", "Stock disponible : " + produit.getQuantiteStock() + " unités.", Alert.AlertType.WARNING);
                return;
            }

            // Vérifier si le produit est déjà dans le panier pour fusionner
            boolean produitExiste = false;
            for (LigneVente ligne : panier) {
                if (ligne.getProduitId() == produit.getId()) {
                    int nouvelleQte = ligne.getQuantite() + qte;
                    if (produit.getQuantiteStock() < nouvelleQte) {
                        afficherAlerte("Stock insuffisant", "Le stock cumulé disponible est insuffisant.", Alert.AlertType.WARNING);
                        return;
                    }
                    ligne.setQuantite(nouvelleQte);
                    produitExiste = true;
                    tablePanier.refresh();
                    break;
                }
            }

            if (!produitExiste) {
                LigneVente ligne = new LigneVente();
                ligne.setProduitId(produit.getId());
                ligne.setPrixUnitaireFacture(produit.getPrixUnitaire());
                ligne.setQuantite(qte);
                ligne.setNomProduit(produit.getNom());
                
                // Helper pour le code barre
                ligne.setCodeProduitHelper(produit.getCodeBarre());
                
                panier.add(ligne);
            }

            txtCodeBarre.clear();
            txtQuantite.setText("1");
            txtCodeBarre.requestFocus();

            recalculerTotaux();

        } catch (SQLException e) {
            e.printStackTrace();
            afficherAlerte("Erreur", "Erreur lors de la recherche du produit : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    void validerEtImprimerAchat(ActionEvent event) {
        if (panier.isEmpty()) {
            afficherAlerte("Panier vide", "Veuillez ajouter des articles avant de valider.", Alert.AlertType.WARNING);
            return;
        }

        Vente vente = new Vente();
        if (clientSelectionne != null) {
            vente.setClientId(clientSelectionne.getId());
        }
        vente.setModePaiement(comboPaiement.getValue());

        for (LigneVente ligne : panier) {
            vente.ajouterLigne(ligne);
        }

        try {
            venteService.enregistrerEtFacturer(vente);
            afficherAlerte("Vente Réussie", "La vente #" + vente.getId() + " a été enregistrée avec succès. Ticket imprimé.", Alert.AlertType.INFORMATION);
            
            // Réinitialiser la caisse
            panier.clear();
            definirClientAnonyme(null);
            recalculerTotaux();

        } catch (SQLException e) {
            e.printStackTrace();
            afficherAlerte("Échec de la transaction", "La vente a été annulée : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void recalculerTotaux() {
        double brut = 0.0;
        for (LigneVente ligne : panier) {
            brut += ligne.getTotalLigne();
        }

        double tauxRemise = 0.0;
        if (clientSelectionne != null) {
            tauxRemise = fideliteService.calculerPourcentageRemise(clientSelectionne.getId());
        }

        double remise = brut * tauxRemise;
        double net = brut - remise;

        lblTotalBrut.setText(String.format("%,.2f HTG", brut));
        lblMontantRemise.setText(String.format("%,.2f HTG", remise));
        lblTotalNet.setText(String.format("%,.2f HTG", net));
    }

    private void afficherAlerte(String titre, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private TableCell<LigneVente, Double> formatDoubleColumn() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Double val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) {
                    setText(null);
                } else {
                    setText(String.format("%,.2f HTG", val));
                }
            }
        };
    }

    // Ajout d'une propriété interne à LigneVente pour garder le code produit propre dans l'interface
    public static class LigneVenteAdapter extends LigneVente {
        private String codeProduitHelper;

        public String getCodeProduitHelper() {
            return codeProduitHelper;
        }

        public void setCodeProduitHelper(String val) {
            this.codeProduitHelper = val;
        }
    }
}

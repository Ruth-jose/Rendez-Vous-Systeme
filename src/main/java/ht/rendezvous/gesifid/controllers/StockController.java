package ht.rendezvous.gesifid.controllers;

import ht.rendezvous.gesifid.dao.ProduitDAO;
import ht.rendezvous.gesifid.models.Produit;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import ht.rendezvous.gesifid.services.ImpressionService;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

import java.sql.SQLException;

public class StockController {

    @FXML private TableView<Produit> tableProduits;
    @FXML private TableColumn<Produit, Integer> colId;
    @FXML private TableColumn<Produit, String> colCode;
    @FXML private TableColumn<Produit, String> colNom;
    @FXML private TableColumn<Produit, Double> colPrix;
    @FXML private TableColumn<Produit, Integer> colStock;
    @FXML private TableColumn<Produit, Integer> colAlerte;
    @FXML private TableColumn<Produit, String> colEtat;

    @FXML private TextField txtCodeBarre;
    @FXML private TextField txtNom;
    @FXML private TextArea txtDescription;
    @FXML private TextField txtPrix;
    @FXML private TextField txtStock;
    @FXML private TextField txtSeuilAlerte;

    private final ProduitDAO produitDAO = new ProduitDAO();
    private final ObservableList<Produit> listeProduits = FXCollections.observableArrayList();
    private Produit produitSelectionne = null;

    @FXML
    public void initialize() {
        // Configuration des colonnes
        colId.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getId()).asObject());
        colCode.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCodeBarre()));
        colNom.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getNom()));
        colPrix.setCellValueFactory(cell -> new SimpleDoubleProperty(cell.getValue().getPrixUnitaire()).asObject());
        colStock.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getQuantiteStock()).asObject());
        colAlerte.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getSeuilAlerte()).asObject());
        colEtat.setCellValueFactory(cell -> {
            boolean alerte = cell.getValue().estEnAlerteStock();
            return new SimpleStringProperty(alerte ? "ALERTE" : "NORMAL");
        });

        // Formater les colonnes monétaires
        colPrix.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Double val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) {
                    setText(null);
                } else {
                    setText(String.format("%,.2f HTG", val));
                }
            }
        });

        // Colorer les lignes du tableau selon le niveau de stock (Besoin B1)
        tableProduits.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Produit produit, boolean empty) {
                super.updateItem(produit, empty);
                if (empty || produit == null) {
                    setStyle("");
                    getStyleClass().removeAll("stock-critical", "stock-normal");
                } else {
                    getStyleClass().removeAll("stock-critical", "stock-normal");
                    if (produit.estEnAlerteStock()) {
                        getStyleClass().add("stock-critical");
                    } else {
                        getStyleClass().add("stock-normal");
                    }
                }
            }
        });

        // Charger les données
        chargerProduits();

        // Écouter les sélections du tableau
        tableProduits.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                remplirFormulaire(newSel);
            }
        });
    }

    private void chargerProduits() {
        try {
            listeProduits.clear();
            listeProduits.addAll(produitDAO.findAll());
            tableProduits.setItems(listeProduits);
        } catch (SQLException e) {
            e.printStackTrace();
            afficherAlerte("Erreur", "Impossible de charger les articles : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void remplirFormulaire(Produit produit) {
        produitSelectionne = produit;
        txtCodeBarre.setText(produit.getCodeBarre());
        txtNom.setText(produit.getNom());
        txtDescription.setText(produit.getDescription());
        txtPrix.setText(String.valueOf(produit.getPrixUnitaire()));
        txtStock.setText(String.valueOf(produit.getQuantiteStock()));
        txtSeuilAlerte.setText(String.valueOf(produit.getSeuilAlerte()));
    }

    @FXML
    void enregistrerProduit(ActionEvent event) {
        String code = txtCodeBarre.getText().trim();
        String nom = txtNom.getText().trim();
        String desc = txtDescription.getText().trim();
        String prixStr = txtPrix.getText().trim();
        String stockStr = txtStock.getText().trim();
        String alerteStr = txtSeuilAlerte.getText().trim();

        if (code.isEmpty() || nom.isEmpty() || prixStr.isEmpty()) {
            afficherAlerte("Formulaire incomplet", "Veuillez remplir les champs obligatoires (Code, Désignation, Prix).", Alert.AlertType.WARNING);
            return;
        }

        try {
            double prix = Double.parseDouble(prixStr);
            int stock = Integer.parseInt(stockStr);
            int alerte = Integer.parseInt(alerteStr);

            if (prix < 0 || stock < 0 || alerte < 0) {
                afficherAlerte("Valeurs négatives", "Les nombres doivent être positifs.", Alert.AlertType.WARNING);
                return;
            }

            if (produitSelectionne == null) {
                // Création
                Produit p = new Produit(0, code, nom, desc, prix, stock, alerte);
                produitDAO.insert(p);
            } else {
                // Modification
                produitSelectionne.setCodeBarre(code);
                produitSelectionne.setNom(nom);
                produitSelectionne.setDescription(desc);
                produitSelectionne.setPrixUnitaire(prix);
                produitSelectionne.setQuantiteStock(stock);
                produitSelectionne.setSeuilAlerte(alerte);
                produitDAO.update(produitSelectionne);
            }

            chargerProduits();
            reinitialiserFormulaire(null);
            afficherAlerte("Succès", "L'article a été enregistré avec succès.", Alert.AlertType.INFORMATION);

        } catch (NumberFormatException e) {
            afficherAlerte("Erreur de saisie", "Veuillez vérifier les formats numériques (Prix, Stock, Seuil).", Alert.AlertType.WARNING);
        } catch (SQLException e) {
            e.printStackTrace();
            afficherAlerte("Erreur DB", "Impossible d'enregistrer le produit : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    void reinitialiserFormulaire(ActionEvent event) {
        produitSelectionne = null;
        tableProduits.getSelectionModel().clearSelection();
        txtCodeBarre.clear();
        txtNom.clear();
        txtDescription.clear();
        txtPrix.clear();
        txtStock.setText("0");
        txtSeuilAlerte.setText("5");
    }

    @FXML
    void genererRapportApprovisionnement(ActionEvent event) {
        try {
            java.util.List<Produit> produits = produitDAO.findAll();
            java.util.List<Produit> alertes = produits.stream()
                    .filter(Produit::estEnAlerteStock)
                    .collect(java.util.stream.Collectors.toList());

            if (alertes.isEmpty()) {
                afficherAlerte("Stock Optimal", "Aucun produit n'est sous le seuil d'alerte. Le stock est optimal !", Alert.AlertType.INFORMATION);
                return;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("================================\n");
            sb.append("     BON D'APPROVISIONNEMENT    \n");
            sb.append("     ENTREPRISE RENDEZ-VOUS     \n");
            sb.append("   Date : ").append(java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append("\n");
            sb.append("================================\n");
            sb.append("Articles en rupture ou alerte :\n");
            sb.append("--------------------------------\n");

            double totalEstime = 0.0;
            for (Produit p : alertes) {
                int qteConseillee = (p.getSeuilAlerte() * 3) - p.getQuantiteStock();
                if (qteConseillee <= 0) {
                    qteConseillee = p.getSeuilAlerte() * 2;
                }
                
                double coutLigne = qteConseillee * p.getPrixUnitaire();
                totalEstime += coutLigne;

                sb.append(p.getNom()).append(" (#").append(p.getCodeBarre()).append(")\n");
                sb.append(String.format("  Actuel: %d | Seuil: %d\n", p.getQuantiteStock(), p.getSeuilAlerte()));
                sb.append(String.format("  A commander : %d unités\n", qteConseillee));
                sb.append(String.format("  Coût estimé : %,.2f HTG\n", coutLigne));
                sb.append("--------------------------------\n");
            }

            sb.append(String.format("TOTAL ESTIMÉ : %,.2f HTG\n", totalEstime));
            sb.append("================================\n");
            sb.append("  Généré automatiquement par\n");
            sb.append("       le système GESIFID\n");
            sb.append("================================\n");

            String rapport = sb.toString();

            Alert dialog = new Alert(Alert.AlertType.NONE);
            dialog.setTitle("Proposition d'Approvisionnement");
            dialog.setHeaderText("Articles à commander pour réapprovisionner");
            
            TextArea textArea = new TextArea(rapport);
            textArea.setEditable(false);
            textArea.setWrapText(true);
            textArea.setPrefWidth(450);
            textArea.setPrefHeight(350);
            
            dialog.getDialogPane().setContent(textArea);
            
            ButtonType btnImprimer = new ButtonType("🖨️ Imprimer", ButtonBar.ButtonData.OK_DONE);
            ButtonType btnEnregistrer = new ButtonType("💾 Enregistrer", ButtonBar.ButtonData.OTHER);
            ButtonType btnFermer = new ButtonType("Fermer", ButtonBar.ButtonData.CANCEL_CLOSE);
            
            dialog.getButtonTypes().setAll(btnImprimer, btnEnregistrer, btnFermer);
            
            java.util.Optional<ButtonType> result = dialog.showAndWait();
            if (result.isPresent()) {
                if (result.get() == btnImprimer) {
                    ImpressionService imp = new ImpressionService();
                    imp.imprimerTicket(rapport);
                    afficherAlerte("Impression Lancée", "Le bon de commande a été envoyé à l'imprimante.", Alert.AlertType.INFORMATION);
                } else if (result.get() == btnEnregistrer) {
                    FileChooser fileChooser = new FileChooser();
                    fileChooser.setTitle("Enregistrer le Bon d'Approvisionnement");
                    fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier Texte (*.txt)", "*.txt"));
                    fileChooser.setInitialFileName("bon_commande_" + java.time.LocalDate.now() + ".txt");
                    
                    Stage stage = (Stage) tableProduits.getScene().getWindow();
                    java.io.File file = fileChooser.showSaveDialog(stage);
                    if (file != null) {
                        try (FileOutputStream fos = new FileOutputStream(file);
                             OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
                             BufferedWriter writer = new BufferedWriter(osw)) {
                            writer.write(rapport);
                            afficherAlerte("Sauvegarde Réussie", "Le rapport a été enregistré sous :\n" + file.getAbsolutePath(), Alert.AlertType.INFORMATION);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            afficherAlerte("Erreur", "Impossible de sauvegarder le fichier : " + ex.getMessage(), Alert.AlertType.ERROR);
                        }
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            afficherAlerte("Erreur DB", "Impossible de générer le rapport : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void afficherAlerte(String titre, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

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

    private void afficherAlerte(String titre, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

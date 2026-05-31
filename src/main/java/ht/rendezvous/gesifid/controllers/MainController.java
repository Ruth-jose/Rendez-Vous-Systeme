package ht.rendezvous.gesifid.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;

import java.io.IOException;

public class MainController {

    @FXML
    private StackPane contentArea;

    @FXML
    private Button btnDashboard;
    @FXML
    private Button btnCaisse;
    @FXML
    private Button btnStock;
    @FXML
    private Button btnClients;
    @FXML
    private Button btnHistorique;
    @FXML
    private Button btnParametres;

    @FXML
    public void initialize() {
        // Charger le tableau de bord par défaut lors du démarrage
        chargerVue("/views/dashboard.fxml", btnDashboard);
    }

    @FXML
    void afficherDashboard(ActionEvent event) {
        chargerVue("/views/dashboard.fxml", btnDashboard);
    }

    @FXML
    void afficherCaisse(ActionEvent event) {
        chargerVue("/views/caisse.fxml", btnCaisse);
    }

    @FXML
    void afficherStock(ActionEvent event) {
        chargerVue("/views/stock.fxml", btnStock);
    }

    @FXML
    void afficherClients(ActionEvent event) {
        chargerVue("/views/client.fxml", btnClients);
    }

    @FXML
    void afficherHistorique(ActionEvent event) {
        chargerVue("/views/historique.fxml", btnHistorique);
    }

    @FXML
    void afficherParametres(ActionEvent event) {
        chargerVue("/views/parametres.fxml", btnParametres);
    }

    private void chargerVue(String fxmlPath, Button sourceButton) {
        try {
            // Charger le fichier FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();

            // Vider le conteneur principal et y ajouter la nouvelle vue
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);

            // Mettre à jour l'esthétique des boutons pour refléter la vue active
            desactiverTousLesBoutonsStyle();
            sourceButton.getStyleClass().add("sidebar-btn-active");

        } catch (IOException e) {
            System.err.println("Erreur lors du chargement de la vue : " + fxmlPath);
            e.printStackTrace();
        }
    }

    private void desactiverTousLesBoutonsStyle() {
        btnDashboard.getStyleClass().remove("sidebar-btn-active");
        btnCaisse.getStyleClass().remove("sidebar-btn-active");
        btnStock.getStyleClass().remove("sidebar-btn-active");
        btnClients.getStyleClass().remove("sidebar-btn-active");
        btnHistorique.getStyleClass().remove("sidebar-btn-active");
        btnParametres.getStyleClass().remove("sidebar-btn-active");
    }
}

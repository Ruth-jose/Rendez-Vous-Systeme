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
    private Button btnCaisse;
    @FXML
    private Button btnStock;
    @FXML
    private Button btnClients;

    @FXML
    public void initialize() {
        // Charger la vue caisse par défaut lors du démarrage
        chargerVue("/views/caisse.fxml", btnCaisse);
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
        btnCaisse.getStyleClass().remove("sidebar-btn-active");
        btnStock.getStyleClass().remove("sidebar-btn-active");
        btnClients.getStyleClass().remove("sidebar-btn-active");
    }
}

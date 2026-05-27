package ht.rendezvous.gesifid;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // Charger la structure FXML principale
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/views/main.fxml")));
            
            // Initialiser la scène
            Scene scene = new Scene(root);
            
            // Appliquer la feuille de style globale
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/styles.css")).toExternalForm());

            primaryStage.setTitle("GESIFID v1.0 - Entreprise Rendez-vous (Gestion & Fidélité)");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1000);
            primaryStage.setMinHeight(700);
            primaryStage.show();

        } catch (Exception e) {
            System.err.println("ERREUR FATALE : Impossible de démarrer l'application GESIFID.");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

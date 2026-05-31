package ht.rendezvous.gesifid.controllers;

import ht.rendezvous.gesifid.config.DatabaseConfig;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.OutputStreamWriter;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class ParametresController {

    @FXML private TextField txtCompanyName;
    @FXML private TextField txtCompanySlogan;
    @FXML private TextField txtCompanyAddress;
    @FXML private TextField txtCompanyPhone;
    @FXML private Label lblDbStatus;

    private static final String PROPERTIES_FILE = "database.properties";

    @FXML
    public void initialize() {
        // Charger les paramètres actuels
        chargerParametres();
        
        // Exécuter un test de connexion asynchrone lors du chargement
        testerConnexion(null);
    }

    private void chargerParametres() {
        File propFile = new File(PROPERTIES_FILE);
        Properties props = new Properties();

        // Charger les valeurs par défaut
        txtCompanyName.setText("ENTREPRISE RENDEZ-VOUS");
        txtCompanySlogan.setText("Bâtir l'avenir en confiance");
        txtCompanyAddress.setText("Cayes-Jacmel, Sud-Est, Haïti");
        txtCompanyPhone.setText("509-3777-6655");

        if (propFile.exists()) {
            try (FileInputStream fis = new FileInputStream(propFile);
                 java.io.InputStreamReader isr = new java.io.InputStreamReader(fis, java.nio.charset.StandardCharsets.UTF_8)) {
                props.load(isr);
                
                if (props.containsKey("company.name")) {
                    txtCompanyName.setText(props.getProperty("company.name"));
                }
                if (props.containsKey("company.slogan")) {
                    txtCompanySlogan.setText(props.getProperty("company.slogan"));
                }
                if (props.containsKey("company.address")) {
                    txtCompanyAddress.setText(props.getProperty("company.address"));
                }
                if (props.containsKey("company.phone")) {
                    txtCompanyPhone.setText(props.getProperty("company.phone"));
                }
            } catch (IOException e) {
                System.err.println("Impossible de charger les paramètres depuis " + PROPERTIES_FILE);
                e.printStackTrace();
            }
        }
    }

    @FXML
    void enregistrerConfig(ActionEvent event) {
        String name = txtCompanyName.getText().trim();
        String slogan = txtCompanySlogan.getText().trim();
        String address = txtCompanyAddress.getText().trim();
        String phone = txtCompanyPhone.getText().trim();

        if (name.isEmpty() || address.isEmpty()) {
            afficherAlerte("Formulaire incomplet", "Le nom et l'adresse de l'entreprise sont obligatoires.", AlertType.WARNING);
            return;
        }

        File propFile = new File(PROPERTIES_FILE);
        Properties props = new Properties();

        // 1. Lire les propriétés existantes pour ne pas écraser les clés de base de données
        if (propFile.exists()) {
            try (FileInputStream fis = new FileInputStream(propFile);
                 java.io.InputStreamReader isr = new java.io.InputStreamReader(fis, java.nio.charset.StandardCharsets.UTF_8)) {
                props.load(isr);
            } catch (IOException e) {
                System.err.println("Erreur de lecture lors de la sauvegarde : " + e.getMessage());
            }
        }

        // 2. Mettre à jour les propriétés de l'entreprise
        props.setProperty("company.name", name);
        props.setProperty("company.slogan", slogan);
        props.setProperty("company.address", address);
        props.setProperty("company.phone", phone);

        // 3. Enregistrer à nouveau sur le disque
        try (FileOutputStream fos = new FileOutputStream(propFile);
             java.io.OutputStreamWriter osw = new java.io.OutputStreamWriter(fos, java.nio.charset.StandardCharsets.UTF_8)) {
            props.store(osw, "=== CONFIGURATION DE LA BASE DE DONNEES & CONFIG TICKET - GESIFID ===");
            afficherAlerte("Configuration Sauvegardée", "Les en-têtes du ticket ont été mis à jour avec succès.", AlertType.INFORMATION);
        } catch (IOException e) {
            e.printStackTrace();
            afficherAlerte("Erreur de sauvegarde", "Échec d'enregistrement sur disque : " + e.getMessage(), AlertType.ERROR);
        }
    }

    @FXML
    void testerConnexion(ActionEvent event) {
        lblDbStatus.setText("Vérification en cours...");
        lblDbStatus.setStyle("-fx-text-fill: #fbbf24; -fx-font-weight: bold;");

        // Utiliser Platform.runLater ou un thread séparé pour éviter de figer l'interface si la DB rame
        new Thread(() -> {
            boolean connecte = false;
            String technicalError = "";

            try {
                // Tenter d'ouvrir une connexion
                Connection conn = DatabaseConfig.getConnection();
                if (conn != null && !conn.isClosed()) {
                    connecte = true;
                }
            } catch (SQLException e) {
                technicalError = e.getMessage();
            }

            final boolean success = connecte;
            final String errMessage = technicalError;

            Platform.runLater(() -> {
                if (success) {
                    lblDbStatus.setText("CONNECTÉ (PostgreSQL)");
                    lblDbStatus.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;"); // Vert émeraude
                } else {
                    lblDbStatus.setText("ERREUR CONNEXION");
                    lblDbStatus.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;"); // Rouge vif
                    if (event != null) {
                        // Si le test a été cliqué manuellement, afficher le message technique
                        afficherAlerte("Échec du Test", "La base de données PostgreSQL est injoignable.\n\nErreur : " + errMessage, AlertType.ERROR);
                    }
                }
            });
        }).start();
    }

    @FXML
    void sauvegarderBase(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Créer une sauvegarde SQL de la base de données");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier SQL (*.sql)", "*.sql"));
        fileChooser.setInitialFileName("gesifid_backup_" + java.time.LocalDate.now() + ".sql");

        Stage stage = (Stage) txtCompanyName.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            try (Connection conn = DatabaseConfig.getConnection();
                 FileOutputStream fos = new FileOutputStream(file);
                 OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
                 BufferedWriter writer = new BufferedWriter(osw)) {

                writer.write("-- ====================================================================\n");
                writer.write("-- SAUVEGARDE AUTOMATIQUE GESIFID\n");
                writer.write("-- Date : " + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) + "\n");
                writer.write("-- ====================================================================\n\n");

                // Nettoyage lors de la restauration
                writer.write("DELETE FROM ligne_vente;\n");
                writer.write("DELETE FROM vente;\n");
                writer.write("DELETE FROM client;\n");
                writer.write("DELETE FROM produit;\n\n");

                // 1. Exporter Produit
                try (java.sql.Statement stmt = conn.createStatement();
                     java.sql.ResultSet rs = stmt.executeQuery("SELECT id, code_barre, nom, description, prix_unitaire, quantite_stock, seuil_alerte FROM produit ORDER BY id")) {
                    while (rs.next()) {
                        int id = rs.getInt("id");
                        String code = rs.getString("code_barre");
                        String nom = rs.getString("nom");
                        String desc = rs.getString("description");
                        double prix = rs.getDouble("prix_unitaire");
                        int stock = rs.getInt("quantite_stock");
                        int alerte = rs.getInt("seuil_alerte");

                        writer.write(String.format("INSERT INTO produit (id, code_barre, nom, description, prix_unitaire, quantite_stock, seuil_alerte) VALUES (%d, %s, %s, %s, %s, %d, %d);\n",
                                id, escapeSql(code), escapeSql(nom), escapeSql(desc), String.valueOf(prix), stock, alerte));
                    }
                }

                // 2. Exporter Client
                try (java.sql.Statement stmt = conn.createStatement();
                     java.sql.ResultSet rs = stmt.executeQuery("SELECT id, nom, prenom, telephone, total_achats_cumules, date_inscription FROM client ORDER BY id")) {
                    while (rs.next()) {
                        int id = rs.getInt("id");
                        String nom = rs.getString("nom");
                        String prenom = rs.getString("prenom");
                        String tel = rs.getString("telephone");
                        double purchases = rs.getDouble("total_achats_cumules");
                        java.sql.Timestamp dateInscr = rs.getTimestamp("date_inscription");

                        writer.write(String.format("INSERT INTO client (id, nom, prenom, telephone, total_achats_cumules, date_inscription) VALUES (%d, %s, %s, %s, %s, %s);\n",
                                id, escapeSql(nom), escapeSql(prenom), escapeSql(tel), String.valueOf(purchases), dateInscr != null ? escapeSql(dateInscr.toString()) : "CURRENT_TIMESTAMP"));
                    }
                }

                // 3. Exporter Vente
                try (java.sql.Statement stmt = conn.createStatement();
                     java.sql.ResultSet rs = stmt.executeQuery("SELECT id, date_vente, client_id, total_brut, montant_remise, total_net, mode_paiement FROM vente ORDER BY id")) {
                    while (rs.next()) {
                        int id = rs.getInt("id");
                        java.sql.Timestamp dateVente = rs.getTimestamp("date_vente");
                        int clientId = rs.getInt("client_id");
                        boolean isClientNull = rs.wasNull();
                        double brut = rs.getDouble("total_brut");
                        double remise = rs.getDouble("montant_remise");
                        double net = rs.getDouble("total_net");
                        String mode = rs.getString("mode_paiement");

                        writer.write(String.format("INSERT INTO vente (id, date_vente, client_id, total_brut, montant_remise, total_net, mode_paiement) VALUES (%d, %s, %s, %s, %s, %s, %s);\n",
                                id, dateVente != null ? escapeSql(dateVente.toString()) : "CURRENT_TIMESTAMP", isClientNull ? "NULL" : String.valueOf(clientId), String.valueOf(brut), String.valueOf(remise), String.valueOf(net), escapeSql(mode)));
                    }
                }

                // 4. Exporter Ligne Vente
                try (java.sql.Statement stmt = conn.createStatement();
                     java.sql.ResultSet rs = stmt.executeQuery("SELECT id, vente_id, produit_id, quantite, prix_unitaire_facture FROM ligne_vente ORDER BY id")) {
                    while (rs.next()) {
                        int id = rs.getInt("id");
                        int venteId = rs.getInt("vente_id");
                        int prodId = rs.getInt("produit_id");
                        int qte = rs.getInt("quantite");
                        double prix = rs.getDouble("prix_unitaire_facture");

                        writer.write(String.format("INSERT INTO ligne_vente (id, vente_id, produit_id, quantite, prix_unitaire_facture) VALUES (%d, %d, %d, %d, %s);\n",
                                id, venteId, prodId, qte, String.valueOf(prix)));
                    }
                }

                // 5. Synchroniser les Séquences
                writer.write("\nSELECT setval('produit_id_seq', COALESCE((SELECT MAX(id) FROM produit), 1));\n");
                writer.write("SELECT setval('client_id_seq', COALESCE((SELECT MAX(id) FROM client), 1));\n");
                writer.write("SELECT setval('vente_id_seq', COALESCE((SELECT MAX(id) FROM vente), 1));\n");
                writer.write("SELECT setval('ligne_vente_id_seq', COALESCE((SELECT MAX(id) FROM ligne_vente), 1));\n");

                afficherAlerte("Sauvegarde Complétée", "La base de données a été sauvegardée avec succès.", AlertType.INFORMATION);

            } catch (Exception e) {
                e.printStackTrace();
                afficherAlerte("Erreur de Sauvegarde", "Impossible de sauvegarder les données : " + e.getMessage(), AlertType.ERROR);
            }
        }
    }

    @FXML
    void restaurerBase(ActionEvent event) {
        Alert confirm = new Alert(AlertType.CONFIRMATION);
        confirm.setTitle("Restauration de Base de Données");
        confirm.setHeaderText("⚠️ Avertissement de Restauration");
        confirm.setContentText("La restauration va ÉCRASER toutes les données actuelles de la base de données (produits, ventes, clients).\n\nSouhaitez-vous continuer ?");
        
        java.util.Optional<ButtonType> resultConfirm = confirm.showAndWait();
        if (resultConfirm.isEmpty() || resultConfirm.get() != ButtonType.OK) {
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner un fichier de sauvegarde SQL");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier SQL (*.sql)", "*.sql"));

        Stage stage = (Stage) txtCompanyName.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            try {
                StringBuilder sqlBuilder = new StringBuilder();
                try (FileInputStream fis = new FileInputStream(file);
                     InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
                     BufferedReader reader = new BufferedReader(isr)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.trim().startsWith("--")) {
                            continue;
                        }
                        sqlBuilder.append(line).append("\n");
                    }
                }

                String sqlScript = sqlBuilder.toString();
                Connection conn = DatabaseConfig.getConnection();
                boolean originalAutoCommit = conn.getAutoCommit();

                try {
                    conn.setAutoCommit(false);
                    try (java.sql.Statement stmt = conn.createStatement()) {
                        stmt.execute(sqlScript);
                    }
                    conn.commit();
                    afficherAlerte("Restauration Réussie", "Toutes les données ont été restaurées avec succès depuis le fichier de sauvegarde.", AlertType.INFORMATION);
                } catch (SQLException ex) {
                    conn.rollback();
                    throw ex;
                } finally {
                    conn.setAutoCommit(originalAutoCommit);
                }

            } catch (Exception e) {
                e.printStackTrace();
                afficherAlerte("Erreur de Restauration", "Échec de restauration de la base : " + e.getMessage(), AlertType.ERROR);
            }
        }
    }

    private String escapeSql(String s) {
        if (s == null) return "NULL";
        return "'" + s.replace("'", "''") + "'";
    }

    private void afficherAlerte(String titre, String message, AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

package ht.rendezvous.gesifid.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import javafx.application.Platform;
import javafx.scene.control.Alert;

/**
 * Singleton de configuration et de gestion des connexions à la base de données PostgreSQL.
 * Lit les identifiants depuis le fichier 'database.properties' externe.
 */
public class DatabaseConfig {

    private static String url = "jdbc:postgresql://localhost:5432/gesifid_db";
    private static String user = "postgres";
    private static String password = "postgres";

    private static Connection connection = null;

    static {
        // Charger les paramètres depuis database.properties s'il existe
        File propFile = new File("database.properties");
        if (propFile.exists()) {
            try (FileInputStream fis = new FileInputStream(propFile)) {
                Properties props = new Properties();
                props.load(fis);
                url = props.getProperty("db.url", url);
                user = props.getProperty("db.user", user);
                password = props.getProperty("db.password", password);
            } catch (IOException e) {
                System.err.println("Impossible de charger database.properties, utilisation des valeurs par défaut.");
            }
        } else {
            // Générer le fichier s'il n'existe pas pour faciliter la configuration utilisateur
            try (FileOutputStream fos = new FileOutputStream(propFile)) {
                Properties props = new Properties();
                props.setProperty("db.url", url);
                props.setProperty("db.user", user);
                props.setProperty("db.password", password);
                props.store(fos, "=== CONFIGURATION DE LA BASE DE DONNEES - GESIFID ===\n" +
                                 "Modifiez ces valeurs pour correspondre a votre configuration PostgreSQL locale.");
            } catch (IOException e) {
                System.err.println("Impossible de créer le fichier database.properties par défaut.");
            }
        }
    }

    // Constructeur privé pour empêcher l'instanciation
    private DatabaseConfig() {}

    /**
     * Récupère ou ouvre la connexion active à la base de données PostgreSQL.
     * @return Connection JDBC
     * @throws SQLException Si une erreur d'accès ou de pilote survient
     */
    public static synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                // Chargement explicite du pilote PostgreSQL
                Class.forName("org.postgresql.Driver");
                connection = DriverManager.getConnection(url, user, password);
            } catch (ClassNotFoundException e) {
                throw new SQLException("Le pilote JDBC PostgreSQL est introuvable. Ajoutez-le à votre classpath Maven.", e);
            } catch (SQLException e) {
                String userFriendlyMessage = 
                    "Impossible de se connecter à la base de données PostgreSQL.\n\n" +
                    "Erreur technique : " + e.getMessage() + "\n\n" +
                    "=== DÉPANNER LA CONNEXION ===\n" +
                    "1. Assurez-vous que PostgreSQL est démarré.\n" +
                    "2. Vérifiez que la base 'gesifid_db' est créée (voir database/schema.sql).\n" +
                    "3. Ouvrez le fichier 'database.properties' à la racine du projet et\n" +
                    "   mettez à jour le mot de passe ('db.password') avec le vôtre.\n" +
                    "4. Relancez l'application.";

                try {
                    // Tenter d'afficher une boîte de dialogue si l'UI JavaFX est démarrée
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Erreur Base de Données");
                        alert.setHeaderText("Échec de connexion à PostgreSQL");
                        alert.setContentText(userFriendlyMessage);
                        alert.getDialogPane().setPrefWidth(550);
                        alert.showAndWait();
                    });
                } catch (Exception ignored) {
                    // Hors thread JavaFX ou non démarré
                }

                throw new SQLException(userFriendlyMessage, e);
            }
        }
        return connection;
    }

    /**
     * Ferme proprement la connexion active si elle existe.
     */
    public static synchronized void closeConnection() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException e) {
                System.err.println("Erreur lors de la fermeture de la connexion DB : " + e.getMessage());
            } finally {
                connection = null;
            }
        }
    }
}

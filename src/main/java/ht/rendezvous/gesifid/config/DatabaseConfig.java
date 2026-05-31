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
            try (FileInputStream fis = new FileInputStream(propFile);
                 java.io.InputStreamReader isr = new java.io.InputStreamReader(fis, java.nio.charset.StandardCharsets.UTF_8)) {
                Properties props = new Properties();
                props.load(isr);
                url = props.getProperty("db.url", url);
                user = props.getProperty("db.user", user);
                password = props.getProperty("db.password", password);
            } catch (IOException e) {
                System.err.println("Impossible de charger database.properties, utilisation des valeurs par défaut.");
            }
        } else {
            // Générer le fichier s'il n'existe pas pour faciliter la configuration utilisateur
            try (FileOutputStream fos = new FileOutputStream(propFile);
                 java.io.OutputStreamWriter osw = new java.io.OutputStreamWriter(fos, java.nio.charset.StandardCharsets.UTF_8)) {
                Properties props = new Properties();
                props.setProperty("db.url", url);
                props.setProperty("db.user", user);
                props.setProperty("db.password", password);
                props.store(osw, "=== CONFIGURATION DE LA BASE DE DONNEES - GESIFID ===\n" +
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
                
                // Initialisation automatique si les tables n'existent pas encore
                initialiserBaseDeDonneesSiNecessaire(connection);
                
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
     * Vérifie si la table 'produit' existe. Si elle est absente, lit et exécute 'database/schema.sql'.
     */
    private static void initialiserBaseDeDonneesSiNecessaire(Connection conn) {
        try (java.sql.Statement stmt = conn.createStatement()) {
            boolean tableExiste = false;
            try {
                // Requête robuste pour vérifier la présence de la table produit
                try (java.sql.ResultSet rs = stmt.executeQuery(
                        "SELECT EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'produit')")) {
                    if (rs.next()) {
                        tableExiste = rs.getBoolean(1);
                    }
                }
            } catch (SQLException checkEx) {
                // En cas d'erreur de vérification, on assume qu'elle n'existe pas
                tableExiste = false;
            }

            if (!tableExiste) {
                System.out.println("GESIFID : La table 'produit' est absente. Initialisation de la base...");
                File schemaFile = new File("database/schema.sql");
                if (schemaFile.exists()) {
                    StringBuilder sql = new StringBuilder();
                    try (FileInputStream fis = new FileInputStream(schemaFile);
                         java.io.InputStreamReader isr = new java.io.InputStreamReader(fis, java.nio.charset.StandardCharsets.UTF_8);
                         java.io.BufferedReader reader = new java.io.BufferedReader(isr)) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            // Ignorer les commentaires monolignes SQL pour un traitement propre
                            if (line.trim().startsWith("--")) {
                                continue;
                            }
                            sql.append(line).append("\n");
                        }
                    }
                    
                    // Exécution du script DDL & DML d'initialisation
                    stmt.execute(sql.toString());
                    System.out.println("GESIFID : Base de données initialisée avec succès avec les données de test !");
                } else {
                    System.err.println("GESIFID [Alerte] : Le fichier de référence 'database/schema.sql' est introuvable à la racine.");
                }
            }
        } catch (Exception e) {
            System.err.println("GESIFID [Erreur] : Impossible d'initialiser automatiquement la base de données : " + e.getMessage());
            e.printStackTrace();
        }
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

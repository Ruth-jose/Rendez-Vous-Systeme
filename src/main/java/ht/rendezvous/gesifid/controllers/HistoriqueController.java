package ht.rendezvous.gesifid.controllers;

import ht.rendezvous.gesifid.dao.ClientDAO;
import ht.rendezvous.gesifid.dao.VenteDAO;
import ht.rendezvous.gesifid.models.Client;
import ht.rendezvous.gesifid.models.LigneVente;
import ht.rendezvous.gesifid.models.Vente;
import ht.rendezvous.gesifid.services.FideliteService;
import ht.rendezvous.gesifid.services.ImpressionService;
import ht.rendezvous.gesifid.services.VenteService;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

public class HistoriqueController {

    @FXML private TextField txtFiltreClient;
    @FXML private ComboBox<String> comboFiltrePaiement;

    @FXML private TableView<Vente> tableVentes;
    @FXML private TableColumn<Vente, Integer> colId;
    @FXML private TableColumn<Vente, String> colDate;
    @FXML private TableColumn<Vente, String> colClient;
    @FXML private TableColumn<Vente, Double> colBrut;
    @FXML private TableColumn<Vente, Double> colRemise;
    @FXML private TableColumn<Vente, Double> colNet;
    @FXML private TableColumn<Vente, String> colPaiement;

    @FXML private Label lblDetailId;
    @FXML private Label lblDetailDate;
    @FXML private Label lblDetailClient;
    @FXML private Label lblDetailPaiement;

    @FXML private TableView<LigneVente> tableDetailLignes;
    @FXML private TableColumn<LigneVente, String> colDetailNom;
    @FXML private TableColumn<LigneVente, Integer> colDetailQte;
    @FXML private TableColumn<LigneVente, Double> colDetailPrix;

    @FXML private Label lblDetailTotalNet;
    @FXML private Button btnReimprimer;
    @FXML private Button btnAnnuler;

    private final VenteDAO venteDAO = new VenteDAO();
    private final ClientDAO clientDAO = new ClientDAO();
    private final VenteService venteService = new VenteService();
    private final FideliteService fideliteService = new FideliteService();
    private final ImpressionService impressionService = new ImpressionService();

    private final ObservableList<Vente> listVentes = FXCollections.observableArrayList();
    private final Map<Integer, Client> clientsCache = new HashMap<>();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        // Configuration des colonnes de l'Historique
        colId.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getId()).asObject());
        colDate.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDateVente().format(formatter)));
        colClient.setCellValueFactory(cell -> {
            Integer clientId = cell.getValue().getClientId();
            if (clientId == null || clientId == 0) {
                return new SimpleStringProperty("COMPTOIR");
            }
            Client c = clientsCache.get(clientId);
            return new SimpleStringProperty(c != null ? c.getNomComplet() : "Client #" + clientId);
        });
        colBrut.setCellValueFactory(cell -> new SimpleDoubleProperty(cell.getValue().getTotalBrut()).asObject());
        colRemise.setCellValueFactory(cell -> new SimpleDoubleProperty(cell.getValue().getMontantRemise()).asObject());
        colNet.setCellValueFactory(cell -> new SimpleDoubleProperty(cell.getValue().getTotalNet()).asObject());
        colPaiement.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getModePaiement()));

        // Formater les colonnes monétaires
        colBrut.setCellFactory(tc -> formatDoubleColumn());
        colRemise.setCellFactory(tc -> formatDoubleColumn());
        colNet.setCellFactory(tc -> formatDoubleColumn());

        // Configuration des colonnes de Détail
        colDetailNom.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getNomProduit()));
        colDetailQte.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getQuantite()).asObject());
        colDetailPrix.setCellValueFactory(cell -> new SimpleDoubleProperty(cell.getValue().getPrixUnitaireFacture()).asObject());
        colDetailPrix.setCellFactory(tc -> formatDoubleColumn());

        // Configuration des filtres de recherche
        comboFiltrePaiement.setItems(FXCollections.observableArrayList("TOUT", "CASH", "MONCASH", "VIREMENT", "CHEQUE"));
        comboFiltrePaiement.setValue("TOUT");

        // Gérer le double-click ou la sélection
        tableVentes.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> afficherDetailsVente(newVal));

        // Raccourci clavier ergonomique : Appuyer sur SUPPR (DELETE) pour annuler rapidement la vente sélectionnée
        tableVentes.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.DELETE) {
                annulerTransaction(null);
            }
        });

        // Charger l'historique initial
        chargerHistorique(null);
    }

    @FXML
    void chargerHistorique(ActionEvent event) {
        try {
            // 1. Mettre à jour le cache des clients pour accélérer le rendu des colonnes
            List<Client> clientsList = clientDAO.findAll();
            clientsCache.clear();
            for (Client c : clientsList) {
                clientsCache.put(c.getId(), c);
            }

            // 2. Charger les ventes
            List<Vente> ventes = venteDAO.findAll();
            listVentes.setAll(ventes);

            // 3. Mettre en place le filtrage dynamique
            FilteredList<Vente> filteredData = new FilteredList<>(listVentes, p -> true);

            // Liaison des écouteurs de filtres
            txtFiltreClient.textProperty().addListener((observable, oldValue, newValue) -> {
                appliquerFiltres(filteredData);
            });

            comboFiltrePaiement.valueProperty().addListener((observable, oldValue, newValue) -> {
                appliquerFiltres(filteredData);
            });

            tableVentes.setItems(filteredData);
            afficherDetailsVente(null);

        } catch (SQLException e) {
            e.printStackTrace();
            afficherAlerte("Erreur", "Impossible de charger l'historique : " + e.getMessage(), AlertType.ERROR);
        }
    }

    private void appliquerFiltres(FilteredList<Vente> filteredData) {
        filteredData.setPredicate(vente -> {
            // Filtre par mode de paiement
            String modeSelectionne = comboFiltrePaiement.getValue();
            if (modeSelectionne != null && !modeSelectionne.equals("TOUT")) {
                if (!vente.getModePaiement().equals(modeSelectionne)) {
                    return false;
                }
            }

            // Filtre par téléphone client
            String telFiltre = txtFiltreClient.getText().trim();
            if (telFiltre != null && !telFiltre.isEmpty()) {
                Integer clientId = vente.getClientId();
                if (clientId == null || clientId == 0) {
                    return false; // Comptoir anonyme ne correspond pas à un téléphone
                }
                Client client = clientsCache.get(clientId);
                if (client == null || !client.getTelephone().contains(telFiltre)) {
                    return false;
                }
            }

            return true;
        });
    }

    private void afficherDetailsVente(Vente vente) {
        if (vente == null) {
            lblDetailId.setText("-");
            lblDetailDate.setText("-");
            lblDetailClient.setText("-");
            lblDetailPaiement.setText("-");
            lblDetailTotalNet.setText("0.00 HTG");
            tableDetailLignes.setItems(FXCollections.observableArrayList());
            btnReimprimer.setDisable(true);
            btnAnnuler.setDisable(true);
            return;
        }

        try {
            // Mettre à jour les informations textuelles
            lblDetailId.setText("#V-" + String.format("%06d", vente.getId()));
            lblDetailDate.setText(vente.getDateVente().format(formatter));
            
            Integer cId = vente.getClientId();
            if (cId != null && cId > 0) {
                Client c = clientsCache.get(cId);
                lblDetailClient.setText(c != null ? c.getNomComplet() + " (" + c.getTelephone() + ")" : "Client #" + cId);
            } else {
                lblDetailClient.setText("CLIENT COMPTOIR (ANONYME)");
            }
            lblDetailPaiement.setText(vente.getModePaiement());
            lblDetailTotalNet.setText(String.format("%,.2f HTG", vente.getTotalNet()));

            // Charger les articles de la facture
            List<LigneVente> lignes = venteDAO.findLignesByVenteId(vente.getId());
            tableDetailLignes.setItems(FXCollections.observableArrayList(lignes));

            // Associer les lignes chargées à la vente sélectionnée pour réimpression propre
            vente.setLignesVente(lignes);

            btnReimprimer.setDisable(false);
            btnAnnuler.setDisable(false);

        } catch (SQLException e) {
            e.printStackTrace();
            afficherAlerte("Erreur", "Impossible de charger les détails du ticket : " + e.getMessage(), AlertType.ERROR);
        }
    }

    @FXML
    void reimprimerReçu(ActionEvent event) {
        Vente vente = tableVentes.getSelectionModel().getSelectedItem();
        if (vente == null) return;

        try {
            String clientName = null;
            double tauxRemise = 0.0;

            if (vente.getClientId() != null && vente.getClientId() > 0) {
                Client c = clientsCache.get(vente.getClientId());
                if (c != null) {
                    clientName = c.getNomComplet();
                    tauxRemise = fideliteService.calculerPourcentageRemise(c.getId());
                }
            }

            // Générer le reçu brut
            String ticketTexte = impressionService.genererTexteTicket(vente, clientName, tauxRemise);
            
            // Envoyer à l'imprimante
            impressionService.imprimerTicket(ticketTexte);
            
            afficherAlerte("Réimpression Réussie", "Le ticket #V-" + vente.getId() + " a été renvoyé à l'imprimante.", AlertType.INFORMATION);

        } catch (Exception e) {
            e.printStackTrace();
            afficherAlerte("Erreur", "Échec de réimpression : " + e.getMessage(), AlertType.ERROR);
        }
    }

    @FXML
    void annulerTransaction(ActionEvent event) {
        Vente vente = tableVentes.getSelectionModel().getSelectedItem();
        if (vente == null) return;

        // Message d'avertissement robuste
        Alert confirmation = new Alert(AlertType.CONFIRMATION);
        confirmation.setTitle("Annulation Transactionnelle");
        confirmation.setHeaderText("Annuler la vente #" + vente.getId() + " ?");
        confirmation.setContentText("=== IMPACTS OPÉRATIONNELS ===\n" +
                                     "1. Les articles vendus seront réintégrés en stock.\n" +
                                     "2. Les dépenses fidélité du client seront recalculées.\n" +
                                     "3. La transaction sera définitivement effacée.\n\n" +
                                     "Souhaitez-vous valider cette annulation ?");
        
        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // Annuler la vente de manière transactionnelle
                venteService.annulerVente(vente.getId());
                
                // Actualiser
                chargerHistorique(null);
                
                afficherAlerte("Annulation Complétée", "La transaction a été annulée et le stock physique a été restauré.", AlertType.INFORMATION);

            } catch (SQLException e) {
                e.printStackTrace();
                afficherAlerte("Échec de l'annulation", "Erreur technique SQL : " + e.getMessage(), AlertType.ERROR);
            }
        }
    }

    @FXML
    void exporterCSV(ActionEvent event) {
        ObservableList<Vente> ventes = tableVentes.getItems();
        if (ventes.isEmpty()) {
            afficherAlerte("Exportation Impossible", "Il n'y a aucune vente affichée à exporter.", AlertType.WARNING);
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter le Journal de Caisse en CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier CSV (*.csv)", "*.csv"));
        fileChooser.setInitialFileName("journal_ventes_" + java.time.LocalDate.now() + ".csv");
        
        Stage stage = (Stage) txtFiltreClient.getScene().getWindow();
        java.io.File file = fileChooser.showSaveDialog(stage);
        
        if (file != null) {
            try (FileOutputStream fos = new FileOutputStream(file);
                 OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
                 BufferedWriter writer = new BufferedWriter(osw)) {
                
                // BOM UTF-8 pour Excel
                writer.write('\ufeff');
                
                // En-tête
                writer.write("N° Ticket;Date & Heure;Client;Total Brut (HTG);Remise (HTG);Total Net (HTG);Mode Paiement\n");
                
                for (Vente v : ventes) {
                    Integer clientId = v.getClientId();
                    String clientName = "COMPTOIR";
                    if (clientId != null && clientId > 0) {
                        Client c = clientsCache.get(clientId);
                        if (c != null) {
                            clientName = c.getNomComplet() + " (" + c.getTelephone() + ")";
                        } else {
                            clientName = "Client #" + clientId;
                        }
                    }
                    
                    String line = String.format("%d;%s;%s;%.2f;%.2f;%.2f;%s\n",
                            v.getId(),
                            v.getDateVente().format(formatter),
                            clientName.replace(";", ","),
                            v.getTotalBrut(),
                            v.getMontantRemise(),
                            v.getTotalNet(),
                            v.getModePaiement()
                    );
                    writer.write(line);
                }
                
                afficherAlerte("Exportation Réussie", "Le journal des ventes a été exporté avec succès dans :\n" + file.getAbsolutePath(), AlertType.INFORMATION);
                
            } catch (Exception e) {
                e.printStackTrace();
                afficherAlerte("Erreur d'Exportation", "Impossible d'enregistrer le fichier CSV : " + e.getMessage(), AlertType.ERROR);
            }
        }
    }

    private void afficherAlerte(String titre, String message, AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private <T> TableCell<T, Double> formatDoubleColumn() {
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
}

package ht.rendezvous.gesifid.controllers;

import ht.rendezvous.gesifid.dao.ClientDAO;
import ht.rendezvous.gesifid.models.Client;
import ht.rendezvous.gesifid.services.FideliteService;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.SQLException;

public class ClientController {

    @FXML private TableView<Client> tableClients;
    @FXML private TableColumn<Client, Integer> colId;
    @FXML private TableColumn<Client, String> colPrenom;
    @FXML private TableColumn<Client, String> colNom;
    @FXML private TableColumn<Client, String> colTelephone;
    @FXML private TableColumn<Client, Double> colAchats;
    @FXML private TableColumn<Client, String> colFidelite;

    @FXML private TextField txtPrenom;
    @FXML private TextField txtNom;
    @FXML private TextField txtTelephone;
    @FXML private TextField txtTotalAchats;

    private final ClientDAO clientDAO = new ClientDAO();
    private final FideliteService fideliteService = new FideliteService();
    private final ObservableList<Client> listeClients = FXCollections.observableArrayList();
    private Client clientSelectionne = null;

    @FXML
    public void initialize() {
        // Configuration des colonnes
        colId.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getId()).asObject());
        colPrenom.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getPrenom()));
        colNom.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getNom()));
        colTelephone.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getTelephone()));
        colAchats.setCellValueFactory(cell -> new SimpleDoubleProperty(cell.getValue().getTotalAchatsCumules()).asObject());
        colFidelite.setCellValueFactory(cell -> {
            double remise = fideliteService.calculerPourcentageRemise(cell.getValue().getId());
            return new SimpleStringProperty(String.format("%.0f%% de remise", remise * 100));
        });

        // Formater le montant cumulé
        colAchats.setCellFactory(tc -> new TableCell<>() {
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

        // Charger les données
        chargerClients();

        // Écouter les sélections du tableau
        tableClients.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                remplirFormulaire(newSel);
            }
        });
    }

    private void chargerClients() {
        try {
            listeClients.clear();
            listeClients.addAll(clientDAO.findAll());
            tableClients.setItems(listeClients);
        } catch (SQLException e) {
            e.printStackTrace();
            afficherAlerte("Erreur", "Impossible de charger les clients : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void remplirFormulaire(Client client) {
        clientSelectionne = client;
        txtPrenom.setText(client.getPrenom());
        txtNom.setText(client.getNom());
        txtTelephone.setText(client.getTelephone());
        txtTotalAchats.setText(String.format("%,.2f", client.getTotalAchatsCumules()));
    }

    @FXML
    void enregistrerClient(ActionEvent event) {
        String prenom = txtPrenom.getText().trim();
        String nom = txtNom.getText().trim();
        String tel = txtTelephone.getText().trim();

        if (prenom.isEmpty() || nom.isEmpty() || tel.isEmpty()) {
            afficherAlerte("Formulaire incomplet", "Veuillez remplir les champs obligatoires (Prénom, Nom, Téléphone).", Alert.AlertType.WARNING);
            return;
        }

        try {
            if (clientSelectionne == null) {
                // Création d'un nouveau profil
                Client c = new Client(0, nom, prenom, tel, 0.0, null);
                clientDAO.insert(c);
            } else {
                // Modification
                clientSelectionne.setPrenom(prenom);
                clientSelectionne.setNom(nom);
                clientSelectionne.setTelephone(tel);
                clientDAO.update(clientSelectionne);
            }

            chargerClients();
            reinitialiserFormulaire(null);
            afficherAlerte("Succès", "Le client a été enregistré avec succès.", Alert.AlertType.INFORMATION);

        } catch (SQLException e) {
            e.printStackTrace();
            afficherAlerte("Erreur DB", "Impossible d'enregistrer le client (Le numéro de téléphone est peut-être déjà utilisé) : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    void reinitialiserFormulaire(ActionEvent event) {
        clientSelectionne = null;
        tableClients.getSelectionModel().clearSelection();
        txtPrenom.clear();
        txtNom.clear();
        txtTelephone.clear();
        txtTotalAchats.setText("0.00");
    }

    private void afficherAlerte(String titre, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

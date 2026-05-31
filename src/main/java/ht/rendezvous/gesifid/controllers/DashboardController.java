package ht.rendezvous.gesifid.controllers;

import ht.rendezvous.gesifid.dao.ClientDAO;
import ht.rendezvous.gesifid.dao.ProduitDAO;
import ht.rendezvous.gesifid.dao.VenteDAO;
import ht.rendezvous.gesifid.models.Client;
import ht.rendezvous.gesifid.models.Produit;
import ht.rendezvous.gesifid.models.Vente;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class DashboardController {

    @FXML private Label lblChiffreAffaires;
    @FXML private Label lblTotalVentes;
    @FXML private Label lblPanierMoyen;
    @FXML private Label lblArticlesAlerte;

    @FXML private ComboBox<String> comboFiltrePeriode;

    @FXML private AreaChart<String, Number> chartChiffreAffaires;
    @FXML private CategoryAxis xAxisDate;
    @FXML private NumberAxis yAxisCA;
    @FXML private PieChart chartModesPaiement;

    @FXML private TableView<Produit> tableAlerteStock;
    @FXML private TableColumn<Produit, String> colAlerteCode;
    @FXML private TableColumn<Produit, String> colAlerteNom;
    @FXML private TableColumn<Produit, Integer> colAlerteStock;
    @FXML private TableColumn<Produit, Integer> colAlerteSeuil;

    @FXML private TableView<Client> tableTopClients;
    @FXML private TableColumn<Client, String> colClientNom;
    @FXML private TableColumn<Client, String> colClientTel;
    @FXML private TableColumn<Client, Double> colClientAchats;

    private final VenteDAO venteDAO = new VenteDAO();
    private final ProduitDAO produitDAO = new ProduitDAO();
    private final ClientDAO clientDAO = new ClientDAO();

    @FXML
    public void initialize() {
        // Configuration des tables
        colAlerteCode.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCodeBarre()));
        colAlerteNom.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getNom()));
        colAlerteStock.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getQuantiteStock()).asObject());
        colAlerteSeuil.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getSeuilAlerte()).asObject());

        colClientNom.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getNomComplet()));
        colClientTel.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getTelephone()));
        colClientAchats.setCellValueFactory(cell -> new SimpleDoubleProperty(cell.getValue().getTotalAchatsCumules()).asObject());
        colClientAchats.setCellFactory(tc -> formatDoubleColumn());

        // Configuration du filtrage par période
        comboFiltrePeriode.setItems(FXCollections.observableArrayList("Aujourd'hui", "Cette Semaine", "Ce Mois-ci", "Tout l'Historique"));
        comboFiltrePeriode.setValue("Tout l'Historique");
        comboFiltrePeriode.setOnAction(e -> chargerAnalyses());

        // Charger les données analytiques
        chargerAnalyses();
    }

    private void chargerAnalyses() {
        try {
            List<Vente> allVentes = venteDAO.findAll();
            List<Produit> allProduits = produitDAO.findAll();
            List<Client> allClients = clientDAO.findAll();

            // Filtrer dynamiquement les ventes selon la période sélectionnée
            String periode = comboFiltrePeriode.getValue();
            if (periode == null) periode = "Tout l'Historique";
            
            java.time.LocalDateTime maintenant = java.time.LocalDateTime.now();
            final String finalPeriode = periode;
            
            List<Vente> ventesFiltrees = allVentes.stream()
                    .filter(v -> {
                        if (v.getDateVente() == null) return false;
                        switch (finalPeriode) {
                            case "Aujourd'hui":
                                return v.getDateVente().toLocalDate().isEqual(maintenant.toLocalDate());
                            case "Cette Semaine":
                                return v.getDateVente().isAfter(maintenant.minusDays(7));
                            case "Ce Mois-ci":
                                return v.getDateVente().isAfter(maintenant.minusMonths(1));
                            default:
                                return true; // Tout l'Historique
                        }
                    })
                    .collect(Collectors.toList());

            // 1. Calculer les KPIs sur les ventes filtrées
            double caTotal = 0.0;
            for (Vente vente : ventesFiltrees) {
                caTotal += vente.getTotalNet();
            }
            int totalVentes = ventesFiltrees.size();
            double panierMoyen = totalVentes > 0 ? caTotal / totalVentes : 0.0;

            // Nombre de produits en alerte
            long countAlerte = allProduits.stream()
                    .filter(p -> p.getQuantiteStock() <= p.getSeuilAlerte())
                    .count();

            // Mettre à jour l'affichage des KPIs
            lblChiffreAffaires.setText(String.format("%,.2f HTG", caTotal));
            lblTotalVentes.setText(String.valueOf(totalVentes));
            lblPanierMoyen.setText(String.format("%,.2f HTG", panierMoyen));
            lblArticlesAlerte.setText(String.valueOf(countAlerte));

            // 2. Remplir la Table d'Alerte Stock (Produits en seuil critique)
            List<Produit> produitsCritiques = allProduits.stream()
                    .filter(p -> p.getQuantiteStock() <= p.getSeuilAlerte())
                    .collect(Collectors.toList());
            tableAlerteStock.setItems(FXCollections.observableArrayList(produitsCritiques));

            // 3. Remplir la Table des Meilleurs Clients
            List<Client> topClients = allClients.stream()
                    .sorted((c1, c2) -> Double.compare(c2.getTotalAchatsCumules(), c1.getTotalAchatsCumules()))
                    .limit(5)
                    .collect(Collectors.toList());
            tableTopClients.setItems(FXCollections.observableArrayList(topClients));

            // 4. Configurer le AreaChart de l'évolution du Chiffre d'Affaires
            chartChiffreAffaires.getData().clear();
            Map<String, Double> caParJour = new TreeMap<>();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");

            // Remplir la map avec les ventes (ordonnées chronologiquement par date)
            for (int i = ventesFiltrees.size() - 1; i >= 0; i--) {
                Vente vente = ventesFiltrees.get(i);
                String dateStr = vente.getDateVente().format(formatter);
                caParJour.put(dateStr, caParJour.getOrDefault(dateStr, 0.0) + vente.getTotalNet());
            }

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Chiffre d'Affaires");
            for (Map.Entry<String, Double> entry : caParJour.entrySet()) {
                series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
            }
            chartChiffreAffaires.getData().add(series);

            // 5. Configurer le PieChart des modes de paiement
            chartModesPaiement.getData().clear();
            Map<String, Integer> methodesCounts = new HashMap<>();
            for (Vente vente : ventesFiltrees) {
                String mode = vente.getModePaiement();
                methodesCounts.put(mode, methodesCounts.getOrDefault(mode, 0) + 1);
            }

            ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
            for (Map.Entry<String, Integer> entry : methodesCounts.entrySet()) {
                pieData.add(new PieChart.Data(entry.getKey() + " (" + entry.getValue() + ")", entry.getValue()));
            }
            chartModesPaiement.setData(pieData);

        } catch (SQLException e) {
            System.err.println("Erreur lors du chargement des statistiques du tableau de bord.");
            e.printStackTrace();
        }
    }

    private TableCell<Client, Double> formatDoubleColumn() {
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

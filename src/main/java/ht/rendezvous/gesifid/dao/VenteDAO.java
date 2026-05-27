package ht.rendezvous.gesifid.dao;

import ht.rendezvous.gesifid.config.DatabaseConfig;
import ht.rendezvous.gesifid.models.LigneVente;
import ht.rendezvous.gesifid.models.Vente;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class VenteDAO {

    public List<Vente> findAll() throws SQLException {
        List<Vente> ventes = new ArrayList<>();
        String sql = "SELECT * FROM vente ORDER BY date_vente DESC";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                ventes.add(mapResultSetToVente(rs));
            }
        }
        return ventes;
    }

    public void insert(Vente vente, Connection conn) throws SQLException {
        String sql = "INSERT INTO vente (client_id, total_brut, montant_remise, total_net, mode_paiement) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (vente.getClientId() != null) {
                pstmt.setInt(1, vente.getClientId());
            } else {
                pstmt.setNull(1, Types.INTEGER);
            }
            pstmt.setDouble(2, vente.getTotalBrut());
            pstmt.setDouble(3, vente.getMontantRemise());
            pstmt.setDouble(4, vente.getTotalNet());
            pstmt.setString(5, vente.getModePaiement());
            pstmt.executeUpdate();
            
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    vente.setId(rs.getInt(1));
                }
            }
        }
    }

    public void insertLigne(LigneVente ligne, Connection conn) throws SQLException {
        String sql = "INSERT INTO ligne_vente (vente_id, produit_id, quantite, prix_unitaire_facture) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, ligne.getVenteId());
            pstmt.setInt(2, ligne.getProduitId());
            pstmt.setInt(3, ligne.getQuantite());
            pstmt.setDouble(4, ligne.getPrixUnitaireFacture());
            pstmt.executeUpdate();
            
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    ligne.setId(rs.getInt(1));
                }
            }
        }
    }

    public List<LigneVente> findLignesByVenteId(int venteId) throws SQLException {
        List<LigneVente> lignes = new ArrayList<>();
        String sql = "SELECT lv.*, p.nom as nom_produit FROM ligne_vente lv " +
                     "JOIN produit p ON lv.produit_id = p.id " +
                     "WHERE lv.vente_id = ? ORDER BY lv.id ASC";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, venteId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    LigneVente lv = new LigneVente(
                            rs.getInt("id"),
                            rs.getInt("vente_id"),
                            rs.getInt("produit_id"),
                            rs.getInt("quantite"),
                            rs.getDouble("prix_unitaire_facture")
                    );
                    lv.setNomProduit(rs.getString("nom_produit"));
                    lignes.add(lv);
                }
            }
        }
        return lignes;
    }

    private Vente mapResultSetToVente(ResultSet rs) throws SQLException {
        Integer clientId = rs.getInt("client_id");
        if (rs.wasNull()) {
            clientId = null;
        }
        return new Vente(
                rs.getInt("id"),
                rs.getTimestamp("date_vente").toLocalDateTime(),
                clientId,
                rs.getDouble("total_brut"),
                rs.getDouble("montant_remise"),
                rs.getDouble("total_net"),
                rs.getString("mode_paiement")
        );
    }
}

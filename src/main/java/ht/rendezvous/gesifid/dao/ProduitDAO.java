package ht.rendezvous.gesifid.dao;

import ht.rendezvous.gesifid.config.DatabaseConfig;
import ht.rendezvous.gesifid.models.Produit;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProduitDAO {

    public List<Produit> findAll() throws SQLException {
        List<Produit> produits = new ArrayList<>();
        String sql = "SELECT * FROM produit ORDER BY nom ASC";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                produits.add(mapResultSetToProduit(rs));
            }
        }
        return produits;
    }

    public Produit findById(int id) throws SQLException {
        String sql = "SELECT * FROM produit WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToProduit(rs);
                }
            }
        }
        return null;
    }

    public Produit findByCodeBarre(String codeBarre) throws SQLException {
        String sql = "SELECT * FROM produit WHERE code_barre = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, codeBarre);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToProduit(rs);
                }
            }
        }
        return null;
    }

    public void insert(Produit produit) throws SQLException {
        String sql = "INSERT INTO produit (code_barre, nom, description, prix_unitaire, quantite_stock, seuil_alerte) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, produit.getCodeBarre());
            pstmt.setString(2, produit.getNom());
            pstmt.setString(3, produit.getDescription());
            pstmt.setDouble(4, produit.getPrixUnitaire());
            pstmt.setInt(5, produit.getQuantiteStock());
            pstmt.setInt(6, produit.getSeuilAlerte());
            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    produit.setId(rs.getInt(1));
                }
            }
        }
    }

    public void update(Produit produit) throws SQLException {
        String sql = "UPDATE produit SET code_barre = ?, nom = ?, description = ?, prix_unitaire = ?, quantite_stock = ?, seuil_alerte = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, produit.getCodeBarre());
            pstmt.setString(2, produit.getNom());
            pstmt.setString(3, produit.getDescription());
            pstmt.setDouble(4, produit.getPrixUnitaire());
            pstmt.setInt(5, produit.getQuantiteStock());
            pstmt.setInt(6, produit.getSeuilAlerte());
            pstmt.setInt(7, produit.getId());
            pstmt.executeUpdate();
        }
    }

    public void updateStock(int id, int nouvelleQuantite) throws SQLException {
        String sql = "UPDATE produit SET quantite_stock = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, nouvelleQuantite);
            pstmt.setInt(2, id);
            pstmt.executeUpdate();
        }
    }

    private Produit mapResultSetToProduit(ResultSet rs) throws SQLException {
        return new Produit(
                rs.getInt("id"),
                rs.getString("code_barre"),
                rs.getString("nom"),
                rs.getString("description"),
                rs.getDouble("prix_unitaire"),
                rs.getInt("quantite_stock"),
                rs.getInt("seuil_alerte")
        );
    }
}

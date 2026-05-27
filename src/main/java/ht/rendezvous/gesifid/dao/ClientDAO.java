package ht.rendezvous.gesifid.dao;

import ht.rendezvous.gesifid.config.DatabaseConfig;
import ht.rendezvous.gesifid.models.Client;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ClientDAO {

    public List<Client> findAll() throws SQLException {
        List<Client> clients = new ArrayList<>();
        String sql = "SELECT * FROM client ORDER BY nom ASC, prenom ASC";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                clients.add(mapResultSetToClient(rs));
            }
        }
        return clients;
    }

    public Client findById(int id) throws SQLException {
        String sql = "SELECT * FROM client WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToClient(rs);
                }
            }
        }
        return null;
    }

    public Client findByTelephone(String telephone) throws SQLException {
        String sql = "SELECT * FROM client WHERE telephone = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, telephone);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToClient(rs);
                }
            }
        }
        return null;
    }

    public void insert(Client client) throws SQLException {
        String sql = "INSERT INTO client (nom, prenom, telephone, total_achats_cumules) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, client.getNom());
            pstmt.setString(2, client.getPrenom());
            pstmt.setString(3, client.getTelephone());
            pstmt.setDouble(4, client.getTotalAchatsCumules());
            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    client.setId(rs.getInt(1));
                }
            }
        }
    }

    public void update(Client client) throws SQLException {
        String sql = "UPDATE client SET nom = ?, prenom = ?, telephone = ?, total_achats_cumules = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, client.getNom());
            pstmt.setString(2, client.getPrenom());
            pstmt.setString(3, client.getTelephone());
            pstmt.setDouble(4, client.getTotalAchatsCumules());
            pstmt.setInt(5, client.getId());
            pstmt.executeUpdate();
        }
    }

    public void addToCumulativePurchases(int id, double amount) throws SQLException {
        String sql = "UPDATE client SET total_achats_cumules = total_achats_cumules + ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, amount);
            pstmt.setInt(2, id);
            pstmt.executeUpdate();
        }
    }

    private Client mapResultSetToClient(ResultSet rs) throws SQLException {
        return new Client(
                rs.getInt("id"),
                rs.getString("nom"),
                rs.getString("prenom"),
                rs.getString("telephone"),
                rs.getDouble("total_achats_cumules"),
                rs.getTimestamp("date_inscription").toLocalDateTime()
        );
    }
}

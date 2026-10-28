package Lab2;

import Lab1.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class ProcessedRequestDAO {
    private final Connection conn;

    public ProcessedRequestDAO(Connection conn) {
        this.conn = conn;
    }

    // returns false if the reference ID is already stored
    public boolean create(String referenceId) throws SQLException {
        String sql = "INSERT INTO processed_requests (reference_id) VALUES (?) "
                + "ON CONFLICT (reference_id) DO NOTHING";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, referenceId);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean exists(String referenceId) throws SQLException {
        String sql = "SELECT 1 FROM processed_requests WHERE reference_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, referenceId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public List<String> findAll() throws SQLException {
        String sql = "SELECT reference_id FROM processed_requests ORDER BY processed_at DESC";
        List<String> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(rs.getString("reference_id"));
            }
        }
        return list;
    }

    public boolean update(String referenceId, Timestamp processedAt) throws SQLException {
        String sql = "UPDATE processed_requests SET processed_at = ? WHERE reference_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, processedAt);
            ps.setString(2, referenceId);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean delete(String referenceId) throws SQLException {
        String sql = "DELETE FROM processed_requests WHERE reference_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, referenceId);
            return ps.executeUpdate() == 1;
        }
    }
}
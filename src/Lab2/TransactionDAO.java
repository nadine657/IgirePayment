package Lab2;

import Lab1.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class TransactionDAO {
    private final Connection conn;

    public TransactionDAO(Connection conn) {
        this.conn = conn;
    }

    public boolean create(Transaction t) throws SQLException {
        String sql = "INSERT INTO transactions (id, account_id, reference_id, transaction_type, amount, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, t.getTransactionId());
            ps.setString(2, t.getAccountNumber());
            ps.setString(3, t.getReferenceId());
            ps.setString(4, t.getType().name());
            ps.setBigDecimal(5, t.getAmount());
            ps.setTimestamp(6, Timestamp.valueOf(t.getTimestamp()));
            return ps.executeUpdate() == 1;
        }
    }

    public Transaction findById(String id) throws SQLException {
        String sql = "SELECT * FROM transactions WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public Transaction findByReferenceId(String referenceId) throws SQLException {
        String sql = "SELECT * FROM transactions WHERE reference_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, referenceId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public List<Transaction> findByAccountId(String accountId) throws SQLException {
        String sql = "SELECT * FROM transactions WHERE account_id = ? ORDER BY created_at DESC";
        List<Transaction> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    public boolean update(Transaction t) throws SQLException {
        String sql = "UPDATE transactions SET transaction_type = ?, amount = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, t.getType().name());
            ps.setBigDecimal(2, t.getAmount());
            ps.setString(3, t.getTransactionId());
            return ps.executeUpdate() == 1;
        }
    }

    public boolean delete(String id) throws SQLException {
        String sql = "DELETE FROM transactions WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            return ps.executeUpdate() == 1;
        }
    }

    private Transaction map(ResultSet rs) throws SQLException {
        Transaction t = new Transaction();
        t.setTransactionId(rs.getString("id"));
        t.setAccountNumber(rs.getString("account_id"));
        t.setReferenceId(rs.getString("reference_id"));
        t.setType(TransactionType.valueOf(rs.getString("transaction_type")));
        t.setAmount(rs.getBigDecimal("amount"));
        t.setTimestamp(rs.getTimestamp("created_at").toLocalDateTime());
        return t;
    }

    public List<Transaction> findByCustomerId(String customerId) throws SQLException {
        String sql = "SELECT t.* FROM transactions t JOIN accounts a ON t.account_id = a.id "
                + "WHERE a.customer_id = ? ORDER BY t.created_at";
        List<Transaction> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    // each row: {type, count, total}
    public List<String[]> dailySummary(Timestamp start, Timestamp end) throws SQLException {
        String sql = "SELECT transaction_type, COUNT(*) AS total_count, SUM(amount) AS total_amount "
                + "FROM transactions WHERE created_at >= ? AND created_at < ? "
                + "GROUP BY transaction_type ORDER BY transaction_type";
        List<String[]> rows = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, start);
            ps.setTimestamp(2, end);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new String[]{rs.getString("transaction_type"),
                            String.valueOf(rs.getInt("total_count")),
                            rs.getBigDecimal("total_amount").toPlainString()});
                }
            }
        }
        return rows;
    }
}
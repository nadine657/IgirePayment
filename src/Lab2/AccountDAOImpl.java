package Lab2;

import Lab1.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AccountDAOImpl implements AccountDAO {
    private final Connection conn;

    public AccountDAOImpl(Connection conn) {
        this.conn = conn;
    }

    @Override
    public boolean create(Account a) throws SQLException {
        String type = (a instanceof SavingsAccount) ? "SAVINGS" : "WALLET";
        String sql = "INSERT INTO accounts (id, customer_id, account_type, balance) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, a.getAccountNumber());
            ps.setString(2, a.getCustomerId());
            ps.setString(3, type);
            ps.setBigDecimal(4, a.getBalance());
            return ps.executeUpdate() == 1;
        }
    }

    @Override
    public Account findById(String id) throws SQLException {
        String sql = "SELECT id, customer_id, account_type, balance FROM accounts WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    // locks the row until the transaction ends, so two requests cannot change the same balance together
    @Override
    public Account findActiveForUpdate(String id) throws SQLException {
        String sql = "SELECT id, customer_id, account_type, balance FROM accounts "
                + "WHERE id = ? AND status = 'ACTIVE' FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    @Override
    public List<Account> findByCustomerId(String customerId) throws SQLException {
        String sql = "SELECT id, customer_id, account_type, balance FROM accounts WHERE customer_id = ?";
        List<Account> list = new ArrayList<>();
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

    @Override
    public boolean updateBalance(String id, BigDecimal balance) throws SQLException {
        String sql = "UPDATE accounts SET balance = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, balance);
            ps.setString(2, id);
            return ps.executeUpdate() == 1;
        }
    }

    @Override
    public boolean updateStatus(String id, String status) throws SQLException {
        String sql = "UPDATE accounts SET status = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, id);
            return ps.executeUpdate() == 1;
        }
    }

    @Override
    public boolean delete(String id) throws SQLException {
        String sql = "DELETE FROM accounts WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            return ps.executeUpdate() == 1;
        }
    }

    @Override
    public int deleteInactive() throws SQLException {
        String sql = "DELETE FROM accounts WHERE status = 'INACTIVE' AND balance = 0";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            return ps.executeUpdate();
        }
    }

    private Account map(ResultSet rs) throws SQLException {
        Account a = "SAVINGS".equals(rs.getString("account_type")) ? new SavingsAccount() : new WalletAccount();
        a.setAccountNumber(rs.getString("id"));
        a.setCustomerId(rs.getString("customer_id"));
        a.setBalance(rs.getBigDecimal("balance"));
        return a;
    }

    @Override
    public boolean setPinHash(String id, String pinHash) throws SQLException {
        String sql = "UPDATE accounts SET pin_hash = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pinHash);
            ps.setString(2, id);
            return ps.executeUpdate() == 1;
        }
    }

    @Override
    public String getPinHash(String id) throws SQLException {
        String sql = "SELECT pin_hash FROM accounts WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("pin_hash") : null;
            }
        }
    }
}
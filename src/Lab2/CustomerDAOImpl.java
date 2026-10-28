package Lab2;

import Lab1.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CustomerDAOImpl implements CustomerDAO {
    private final Connection conn;

    public CustomerDAOImpl(Connection conn) {
        this.conn = conn;
    }

    @Override
    public boolean create(Customer c) throws SQLException {
        String sql = "INSERT INTO customers (id, full_name, email, phone_number) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, c.getCustomerId());
            ps.setString(2, c.getName());
            ps.setString(3, c.getEmail());
            ps.setString(4, c.getPhone());
            return ps.executeUpdate() == 1;
        }
    }

    @Override
    public Customer findById(String id) throws SQLException {
        String sql = "SELECT id, full_name, email, phone_number FROM customers WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    @Override
    public Customer findByEmail(String email) throws SQLException {
        String sql = "SELECT id, full_name, email, phone_number FROM customers WHERE email = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    @Override
    public List<Customer> findAll() throws SQLException {
        String sql = "SELECT id, full_name, email, phone_number FROM customers ORDER BY full_name";
        List<Customer> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    @Override
    public boolean update(Customer c) throws SQLException {
        String sql = "UPDATE customers SET full_name = ?, email = ?, phone_number = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, c.getName());
            ps.setString(2, c.getEmail());
            ps.setString(3, c.getPhone());
            ps.setString(4, c.getCustomerId());
            return ps.executeUpdate() == 1;
        }
    }

    @Override
    public boolean delete(String id) throws SQLException {
        String sql = "DELETE FROM customers WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            return ps.executeUpdate() == 1;
        }
    }

    private Customer map(ResultSet rs) throws SQLException {
        return new Customer(rs.getString("id"), rs.getString("full_name"),
                rs.getString("email"), rs.getString("phone_number"));
    }
}
package Lab2;

import Lab1.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

public interface CustomerDAO {
    boolean create(Customer c) throws SQLException;
    Customer findById(String id) throws SQLException;
    Customer findByEmail(String email) throws SQLException;
    List<Customer> findAll() throws SQLException;
    boolean update(Customer c) throws SQLException;
    boolean delete(String id) throws SQLException;
}
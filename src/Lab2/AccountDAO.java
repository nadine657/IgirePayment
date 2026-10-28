package Lab2;

import Lab1.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

public interface AccountDAO {
    boolean create(Account a) throws SQLException;
    Account findById(String id) throws SQLException;
    Account findActiveForUpdate(String id) throws SQLException;
    List<Account> findByCustomerId(String customerId) throws SQLException;
    boolean updateBalance(String id, BigDecimal balance) throws SQLException;
    boolean updateStatus(String id, String status) throws SQLException;
    boolean delete(String id) throws SQLException;
    int deleteInactive() throws SQLException;
    boolean setPinHash(String id, String pinHash) throws SQLException;
    String getPinHash(String id) throws SQLException;
}
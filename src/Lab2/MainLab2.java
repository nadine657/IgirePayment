package Lab2;

import Lab1.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class MainLab2 {

    public static class PaymentService {

        private interface SqlWork {
            void run(Connection conn) throws SQLException;
        }

        public void addCustomer(Customer customer) {
            try (Connection conn = DatabaseConnection.getConnection()) {
                new CustomerDAOImpl(conn).create(customer);
            } catch (SQLException e) {
                throw new IllegalStateException("Could not add customer: " + e.getMessage());
            }
        }

        public void updateCustomer(Customer customer) {
            try (Connection conn = DatabaseConnection.getConnection()) {
                if (!new CustomerDAOImpl(conn).update(customer)) {
                    throw new IllegalArgumentException("Customer not found");
                }
            } catch (SQLException e) {
                throw new IllegalStateException("Could not update customer: " + e.getMessage());
            }
        }

        public void createAccount(Account account) {
            try (Connection conn = DatabaseConnection.getConnection()) {
                new AccountDAOImpl(conn).create(account);
            } catch (SQLException e) {
                throw new IllegalStateException("Could not create account: " + e.getMessage());
            }
        }

        public void deactivateAccount(String accountId) {
            try (Connection conn = DatabaseConnection.getConnection()) {
                new AccountDAOImpl(conn).updateStatus(accountId, "INACTIVE");
            } catch (SQLException e) {
                throw new IllegalStateException("Could not deactivate account: " + e.getMessage());
            }
        }

        public int deleteInactiveAccounts() {
            try (Connection conn = DatabaseConnection.getConnection()) {
                return new AccountDAOImpl(conn).deleteInactive();
            } catch (SQLException e) {
                throw new IllegalStateException("Could not delete inactive accounts: " + e.getMessage());
            }
        }

        public List<Transaction> getTransactionHistory(String accountId) {
            try (Connection conn = DatabaseConnection.getConnection()) {
                return new TransactionDAO(conn).findByAccountId(accountId);
            } catch (SQLException e) {
                throw new IllegalStateException("Could not load history: " + e.getMessage());
            }
        }

        // returns true if processed, false if the reference ID was already used
        public boolean deposit(String referenceId, String accountId, BigDecimal amount) {
            return execute(referenceId, conn -> {
                AccountDAO accountDAO = new AccountDAOImpl(conn);
                Account account = loadActive(accountDAO, accountId);
                account.deposit(amount);
                accountDAO.updateBalance(accountId, account.getBalance());
                new TransactionDAO(conn).create(new Transaction(referenceId, accountId, amount, TransactionType.DEPOSIT));
            });
        }

        public boolean withdraw(String referenceId, String accountId, BigDecimal amount) {
            return execute(referenceId, conn -> {
                AccountDAO accountDAO = new AccountDAOImpl(conn);
                Account account = loadActive(accountDAO, accountId);
                account.withdraw(amount);
                accountDAO.updateBalance(accountId, account.getBalance());
                new TransactionDAO(conn).create(new Transaction(referenceId, accountId, amount, TransactionType.WITHDRAWAL));
            });
        }

        public boolean transfer(String referenceId, String fromId, String toId, BigDecimal amount) {
            if (fromId.equals(toId)) {
                throw new IllegalArgumentException("Cannot transfer to the same account");
            }
            return execute(referenceId, conn -> {
                AccountDAO accountDAO = new AccountDAOImpl(conn);
                TransactionDAO transactionDAO = new TransactionDAO(conn);

                // lock in a fixed order so two opposite transfers cannot deadlock
                String firstId = fromId.compareTo(toId) < 0 ? fromId : toId;
                String secondId = firstId.equals(fromId) ? toId : fromId;
                Account first = loadActive(accountDAO, firstId);
                Account second = loadActive(accountDAO, secondId);
                Account from = firstId.equals(fromId) ? first : second;
                Account to = firstId.equals(fromId) ? second : first;

                from.withdraw(amount);
                to.deposit(amount);
                accountDAO.updateBalance(fromId, from.getBalance());
                accountDAO.updateBalance(toId, to.getBalance());

                transactionDAO.create(new Transaction(referenceId + "-OUT", fromId, amount, TransactionType.WITHDRAWAL));
                transactionDAO.create(new Transaction(referenceId + "-IN", toId, amount, TransactionType.DEPOSIT));
            });
        }

        private Account loadActive(AccountDAO accountDAO, String accountId) throws SQLException {
            Account account = accountDAO.findActiveForUpdate(accountId);
            if (account == null) {
                throw new IllegalArgumentException("Account not found or inactive: " + accountId);
            }
            return account;
        }

        // idempotency: the reference ID and the money movement are saved together or not at all
        private boolean execute(String referenceId, SqlWork work) {
            if (referenceId == null || referenceId.trim().isEmpty()) {
                throw new IllegalArgumentException("Reference ID is required");
            }
            try (Connection conn = DatabaseConnection.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    ProcessedRequestDAO processedDAO = new ProcessedRequestDAO(conn);
                    if (processedDAO.exists(referenceId) || !processedDAO.create(referenceId)) {
                        conn.rollback();
                        return false;
                    }
                    work.run(conn);
                    conn.commit();
                    return true;
                } catch (SQLException | RuntimeException e) {
                    conn.rollback();
                    throw e;
                }
            } catch (SQLException e) {
                throw new IllegalStateException("Database error: " + e.getMessage());
            }
        }

        public Customer getCustomer(String customerId) {
            try (Connection conn = DatabaseConnection.getConnection()) {
                Customer customer = new CustomerDAOImpl(conn).findById(customerId);
                if (customer == null) {
                    throw new IllegalArgumentException("Invalid customer ID");
                }
                return customer;
            } catch (SQLException e) {
                throw new IllegalStateException("Database error: " + e.getMessage());
            }
        }

        public List<Account> getCustomerAccounts(String customerId) {
            try (Connection conn = DatabaseConnection.getConnection()) {
                if (new CustomerDAOImpl(conn).findById(customerId) == null) {
                    throw new IllegalArgumentException("Invalid customer ID");
                }
                return new AccountDAOImpl(conn).findByCustomerId(customerId);
            } catch (SQLException e) {
                throw new IllegalStateException("Database error: " + e.getMessage());
            }
        }

        public BigDecimal getBalance(String accountId) {
            try (Connection conn = DatabaseConnection.getConnection()) {
                Account account = new AccountDAOImpl(conn).findById(accountId);
                if (account == null) {
                    throw new IllegalArgumentException("Invalid account ID");
                }
                return account.getBalance();
            } catch (SQLException e) {
                throw new IllegalStateException("Database error: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        PaymentService service = new PaymentService();
        String run = String.valueOf(System.currentTimeMillis());

        String customerId = "2005";
        String walletId = "W2005";
        String savingsId = "S2005";

        // remove the previous test run so the program can be run again (accounts and transactions are deleted with it)
        try (Connection conn = DatabaseConnection.getConnection()) {
            new CustomerDAOImpl(conn).delete(customerId);
        } catch (SQLException e) {
            throw new IllegalStateException("Database error: " + e.getMessage());
        }

        Customer nadine = new Customer(customerId, "Nadine", "nadine@mail.com", "0796556854");
        service.addCustomer(nadine);

        service.createAccount(new WalletAccount(walletId, customerId, new BigDecimal("75000"), new BigDecimal("1000000")));
        service.createAccount(new SavingsAccount(savingsId, customerId, new BigDecimal("100000"),
                new BigDecimal("0.03"), new BigDecimal("0.01"), BigDecimal.ZERO, 3));

        String ref1 = "REF-" + run + "-1";
        System.out.println("Deposit first try: " + service.deposit(ref1, walletId, new BigDecimal("20000")));
        System.out.println("Deposit retry (same ref): " + service.deposit(ref1, walletId, new BigDecimal("20000")));

        System.out.println("Withdraw: " + service.withdraw("REF-" + run + "-2", walletId, new BigDecimal("5000")));
        System.out.println("Transfer: " + service.transfer("REF-" + run + "-3", walletId, savingsId, new BigDecimal("10000")));
        System.out.println("Transfer retry (same ref): " + service.transfer("REF-" + run + "-3", walletId, savingsId, new BigDecimal("10000")));

        try {
            service.withdraw("REF-" + run + "-4", walletId, new BigDecimal("999999999"));
        } catch (IllegalArgumentException e) {
            System.out.println("Rejected: " + e.getMessage());
        }

        System.out.println("\nWallet history:");
        service.getTransactionHistory(walletId).forEach(System.out::println);
        System.out.println("\nSavings history:");
        service.getTransactionHistory(savingsId).forEach(System.out::println);

        nadine.setName("Nadine");
        nadine.setPhone("0796556854");
        service.updateCustomer(nadine);

        service.deactivateAccount(savingsId);
        System.out.println("\nInactive accounts deleted: " + service.deleteInactiveAccounts());
    }
}
package Lab3;

import Lab1.*;
import Lab2.*;
import Lab2.MainLab2.PaymentService;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

public class Main {

    public static class AuthService {

        public static void checkPinFormat(String pin) {
            if (pin == null || !pin.matches("\\d{4}")) {
                throw new IllegalArgumentException("PIN must be exactly 4 digits");
            }
        }

        // the account ID is mixed in so two accounts with the same PIN get different hashes
        public static String hash(String accountId, String pin) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] bytes = digest.digest((accountId + ":" + pin).getBytes(StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                for (byte b : bytes) {
                    sb.append(String.format("%02x", b));
                }
                return sb.toString();
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("SHA-256 not available", e);
            }
        }

        public void createPin(String accountId, String pin) {
            checkPinFormat(pin);
            try (Connection conn = DatabaseConnection.getConnection()) {
                AccountDAO dao = new AccountDAOImpl(conn);
                if (dao.findById(accountId) == null) {
                    throw new IllegalArgumentException("Invalid account ID");
                }
                if (dao.getPinHash(accountId) != null) {
                    throw new IllegalArgumentException("A PIN is already set for this account. Use change PIN instead");
                }
                dao.setPinHash(accountId, hash(accountId, pin));
            } catch (SQLException e) {
                throw new IllegalStateException("Database error: " + e.getMessage());
            }
        }

        public boolean validatePin(String accountId, String pin) {
            try (Connection conn = DatabaseConnection.getConnection()) {
                AccountDAO dao = new AccountDAOImpl(conn);
                if (dao.findById(accountId) == null) {
                    throw new IllegalArgumentException("Invalid account ID");
                }
                String stored = dao.getPinHash(accountId);
                if (stored == null) {
                    throw new IllegalArgumentException("No PIN set for this account");
                }
                return pin != null && stored.equals(hash(accountId, pin));
            } catch (SQLException e) {
                throw new IllegalStateException("Database error: " + e.getMessage());
            }
        }

        public void changePin(String accountId, String oldPin, String newPin) {
            checkPinFormat(newPin);
            if (!validatePin(accountId, oldPin)) {
                throw new IllegalArgumentException("Invalid PIN");
            }
            try (Connection conn = DatabaseConnection.getConnection()) {
                new AccountDAOImpl(conn).setPinHash(accountId, hash(accountId, newPin));
            } catch (SQLException e) {
                throw new IllegalStateException("Database error: " + e.getMessage());
            }
        }

        public void createAccountWithPin(Account account, String pin) {
            checkPinFormat(pin);
            try (Connection conn = DatabaseConnection.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    if (new CustomerDAOImpl(conn).findById(account.getCustomerId()) == null) {
                        throw new IllegalArgumentException("Invalid customer ID");
                    }
                    AccountDAO accountDAO = new AccountDAOImpl(conn);
                    accountDAO.create(account);
                    accountDAO.setPinHash(account.getAccountNumber(),
                            hash(account.getAccountNumber(), pin));
                    conn.commit();
                } catch (SQLException | RuntimeException e) {
                    conn.rollback();
                    throw e;
                }
            } catch (SQLException e) {
                throw new IllegalStateException("Could not create account: " + e.getMessage());
            }
        }
    }

    public static class ReportService {

        public int exportToCsv(String accountId, String filePath) {
            List<Transaction> history;
            try (Connection conn = DatabaseConnection.getConnection()) {
                if (new AccountDAOImpl(conn).findById(accountId) == null) {
                    throw new IllegalArgumentException("Invalid account ID");
                }
                history = new TransactionDAO(conn).findByAccountId(accountId);
            } catch (SQLException e) {
                throw new IllegalStateException("Database error: " + e.getMessage());
            }

            try (PrintWriter out = new PrintWriter(new FileWriter(filePath))) {
                out.println("transaction_id,reference_id,account_id,type,amount,timestamp");
                for (Transaction t : history) {
                    out.println(t.getTransactionId() + "," + t.getReferenceId() + "," + t.getAccountNumber() + ","
                            + t.getType() + "," + t.getAmount().toPlainString() + "," + t.getTimestamp());
                }
            } catch (IOException e) {
                throw new IllegalStateException("Could not write file: " + e.getMessage());
            }
            return history.size();
        }

        public List<String[]> dailySummary(LocalDate date) {
            try (Connection conn = DatabaseConnection.getConnection()) {
                Timestamp start = Timestamp.valueOf(date.atStartOfDay());
                Timestamp end = Timestamp.valueOf(date.plusDays(1).atStartOfDay());
                return new TransactionDAO(conn).dailySummary(start, end);
            } catch (SQLException e) {
                throw new IllegalStateException("Database error: " + e.getMessage());
            }
        }

        public List<Transaction> customerStatement(String customerId) {
            try (Connection conn = DatabaseConnection.getConnection()) {
                if (new CustomerDAOImpl(conn).findById(customerId) == null) {
                    throw new IllegalArgumentException("Invalid customer ID");
                }
                return new TransactionDAO(conn).findByCustomerId(customerId);
            } catch (SQLException e) {
                throw new IllegalStateException("Database error: " + e.getMessage());
            }
        }
    }

    private static final Scanner in = new Scanner(System.in);
    private static final PaymentService service = new PaymentService();
    private static final AuthService auth = new AuthService();
    private static final ReportService reports = new ReportService();

    public static void main(String[] args) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            System.out.println("Connected to the database.");
        } catch (SQLException e) {
            System.out.println("Cannot connect to the database: " + e.getMessage());
            return;
        }

        System.out.println("=== IgirePay Payment Management ===");
        while (true) {
            System.out.println("\n--- Main Menu ---");
            System.out.println("1. Customer Management");
            System.out.println("2. Account Management");
            System.out.println("3. Transaction Management");
            System.out.println("4. Reports");
            System.out.println("5. Security (PIN)");
            System.out.println("0. Exit");
            String choice = ask("Choose");
            switch (choice) {
                case "1": customerMenu(); break;
                case "2": accountMenu(); break;
                case "3": transactionMenu(); break;
                case "4": reportMenu(); break;
                case "5": securityMenu(); break;
                case "0":
                    System.out.println("Goodbye.");
                    return;
                default: System.out.println("Invalid choice");
            }
        }
    }

    // ---------- menus ----------

    private static void customerMenu() {
        while (true) {
            System.out.println("\n--- Customer Management ---");
            System.out.println("1. Register customer");
            System.out.println("2. Update customer information");
            System.out.println("3. View customer accounts");
            System.out.println("0. Back");
            switch (ask("Choose")) {
                case "1": safely(Main::registerCustomer); break;
                case "2": safely(Main::updateCustomer); break;
                case "3": safely(Main::viewCustomerAccounts); break;
                case "0": return;
                default: System.out.println("Invalid choice");
            }
        }
    }

    private static void accountMenu() {
        while (true) {
            System.out.println("\n--- Account Management ---");
            System.out.println("1. Create wallet account");
            System.out.println("2. Create savings account");
            System.out.println("3. View account balance");
            System.out.println("4. Deactivate account");
            System.out.println("5. Delete inactive accounts");
            System.out.println("0. Back");
            switch (ask("Choose")) {
                case "1": safely(() -> createAccount(false)); break;
                case "2": safely(() -> createAccount(true)); break;
                case "3": safely(Main::viewBalance); break;
                case "4": safely(Main::deactivateAccount); break;
                case "5": safely(Main::deleteInactiveAccounts); break;
                case "0": return;
                default: System.out.println("Invalid choice");
            }
        }
    }

    private static void transactionMenu() {
        while (true) {
            System.out.println("\n--- Transaction Management ---");
            System.out.println("1. Deposit money");
            System.out.println("2. Withdraw money");
            System.out.println("3. Transfer money");
            System.out.println("4. View transaction history");
            System.out.println("0. Back");
            switch (ask("Choose")) {
                case "1": safely(Main::deposit); break;
                case "2": safely(Main::withdraw); break;
                case "3": safely(Main::transfer); break;
                case "4": safely(Main::viewHistory); break;
                case "0": return;
                default: System.out.println("Invalid choice");
            }
        }
    }

    private static void reportMenu() {
        while (true) {
            System.out.println("\n--- Reports ---");
            System.out.println("1. Export transaction history to CSV");
            System.out.println("2. Daily transaction summary");
            System.out.println("3. Customer transaction statement");
            System.out.println("0. Back");
            switch (ask("Choose")) {
                case "1": safely(Main::exportCsv); break;
                case "2": safely(Main::dailySummary); break;
                case "3": safely(Main::customerStatement); break;
                case "0": return;
                default: System.out.println("Invalid choice");
            }
        }
    }

    private static void securityMenu() {
        while (true) {
            System.out.println("\n--- Security (PIN) ---");
            System.out.println("1. Create PIN");
            System.out.println("2. Validate PIN");
            System.out.println("3. Change PIN");
            System.out.println("0. Back");
            switch (ask("Choose")) {
                case "1": safely(Main::createPin); break;
                case "2": safely(Main::validatePin); break;
                case "3": safely(Main::changePin); break;
                case "0": return;
                default: System.out.println("Invalid choice");
            }
        }
    }

    // ---------- customers ----------

    private static void registerCustomer() {
        String name = ask("Full name");
        String email = ask("Email");
        String phone = ask("Phone number");
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Name is required");
        }
        if (!email.contains("@")) {
            throw new IllegalArgumentException("Invalid email address");
        }
        String id = "C" + shortId();
        service.addCustomer(new Customer(id, name, email, phone));
        System.out.println("Customer registered. Customer ID: " + id);
    }

    private static void updateCustomer() {
        Customer c = service.getCustomer(ask("Customer ID"));
        String name = ask("New full name (blank to keep \"" + c.getName() + "\")");
        String email = ask("New email (blank to keep \"" + c.getEmail() + "\")");
        String phone = ask("New phone (blank to keep \"" + c.getPhone() + "\")");
        if (!email.isEmpty() && !email.contains("@")) {
            throw new IllegalArgumentException("Invalid email address");
        }
        if (!name.isEmpty()) c.setName(name);
        if (!email.isEmpty()) c.setEmail(email);
        if (!phone.isEmpty()) c.setPhone(phone);
        service.updateCustomer(c);
        System.out.println("Customer updated.");
    }

    private static void viewCustomerAccounts() {
        List<Account> accounts = service.getCustomerAccounts(ask("Customer ID"));
        if (accounts.isEmpty()) {
            System.out.println("This customer has no accounts.");
        } else {
            accounts.forEach(System.out::println);
        }
    }

    // ---------- accounts ----------

    private static void createAccount(boolean savings) {
        String customerId = ask("Customer ID");
        BigDecimal opening = readAmount("Opening balance", true);
        String pin = ask("Choose a 4-digit PIN");
        String id = (savings ? "S" : "W") + shortId();
        Account account = savings
                ? new SavingsAccount(id, customerId, opening, new BigDecimal("0.03"),
                new BigDecimal("0.01"), BigDecimal.ZERO, 3)
                : new WalletAccount(id, customerId, opening, new BigDecimal("1000000"));
        auth.createAccountWithPin(account, pin);
        System.out.println((savings ? "Savings" : "Wallet") + " account created. Account ID: " + id);
    }

    private static void viewBalance() {
        String accountId = ask("Account ID");
        requirePin(accountId);
        System.out.println("Balance: " + service.getBalance(accountId));
    }

    private static void deactivateAccount() {
        String accountId = ask("Account ID");
        requirePin(accountId);
        service.deactivateAccount(accountId);
        System.out.println("Account marked as inactive.");
    }

    private static void deleteInactiveAccounts() {
        int deleted = service.deleteInactiveAccounts();
        System.out.println(deleted + " inactive account(s) deleted (accounts with a balance are kept).");
    }

    // ---------- transactions ----------

    private static void deposit() {
        String accountId = ask("Account ID");
        BigDecimal amount = readAmount("Amount", false);
        String ref = ask("Reference ID");
        boolean done = service.deposit(ref, accountId, amount);
        printResult(done, "Deposit successful.");
    }

    private static void withdraw() {
        String accountId = ask("Account ID");
        requirePin(accountId);
        BigDecimal amount = readAmount("Amount", false);
        String ref = ask("Reference ID");
        boolean done = service.withdraw(ref, accountId, amount);
        printResult(done, "Withdrawal successful.");
    }

    private static void transfer() {
        String fromId = ask("From account ID");
        requirePin(fromId);
        String toId = ask("To account ID");
        BigDecimal amount = readAmount("Amount", false);
        String ref = ask("Reference ID");
        boolean done = service.transfer(ref, fromId, toId, amount);
        printResult(done, "Transfer successful.");
    }

    private static void viewHistory() {
        String accountId = ask("Account ID");
        requirePin(accountId);
        List<Transaction> history = service.getTransactionHistory(accountId);
        if (history.isEmpty()) {
            System.out.println("No transactions found.");
            return;
        }
        for (Transaction t : history) {
            System.out.printf("%s | %-10s | %12s | %s%n",
                    t.getTimestamp(), t.getType(), t.getAmount().toPlainString(), t.getReferenceId());
        }
    }

    private static void printResult(boolean done, String successMessage) {
        if (done) {
            System.out.println(successMessage);
        } else {
            System.out.println("Duplicate request: this reference ID was already processed. Nothing was changed.");
        }
    }

    // ---------- reports ----------

    private static void exportCsv() {
        String accountId = ask("Account ID");
        requirePin(accountId);
        String file = ask("File name (blank for " + accountId + "_history.csv)");
        if (file.isEmpty()) {
            file = accountId + "_history.csv";
        }
        int count = reports.exportToCsv(accountId, file);
        System.out.println(count + " transaction(s) exported to " + file);
    }

    private static void dailySummary() {
        String text = ask("Date (yyyy-MM-dd, blank for today)");
        LocalDate date;
        try {
            date = text.isEmpty() ? LocalDate.now() : LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date. Use yyyy-MM-dd");
        }
        List<String[]> rows = reports.dailySummary(date);
        if (rows.isEmpty()) {
            System.out.println("No transactions on " + date);
            return;
        }
        System.out.println("Summary for " + date);
        System.out.printf("%-12s %8s %15s%n", "Type", "Count", "Total");
        for (String[] r : rows) {
            System.out.printf("%-12s %8s %15s%n", r[0], r[1], r[2]);
        }
    }

    private static void customerStatement() {
        String customerId = ask("Customer ID");
        List<Transaction> list = reports.customerStatement(customerId);
        if (list.isEmpty()) {
            System.out.println("No transactions found for this customer.");
            return;
        }
        BigDecimal deposits = BigDecimal.ZERO;
        BigDecimal withdrawals = BigDecimal.ZERO;
        System.out.println("Statement for customer " + customerId);
        for (Transaction t : list) {
            System.out.printf("%s | %-12s | %-10s | %12s | %s%n", t.getTimestamp(), t.getAccountNumber(),
                    t.getType(), t.getAmount().toPlainString(), t.getReferenceId());
            if (t.getType() == TransactionType.DEPOSIT) {
                deposits = deposits.add(t.getAmount());
            } else {
                withdrawals = withdrawals.add(t.getAmount());
            }
        }
        System.out.println("Total deposits:    " + deposits.toPlainString());
        System.out.println("Total withdrawals: " + withdrawals.toPlainString());
    }

    // ---------- security ----------

    private static void createPin() {
        String accountId = ask("Account ID");
        String pin = ask("New 4-digit PIN");
        auth.createPin(accountId, pin);
        System.out.println("PIN created.");
    }

    private static void validatePin() {
        String accountId = ask("Account ID");
        String pin = ask("PIN");
        System.out.println(auth.validatePin(accountId, pin) ? "PIN is correct." : "PIN is wrong.");
    }

    private static void changePin() {
        String accountId = ask("Account ID");
        String oldPin = ask("Current PIN");
        String newPin = ask("New 4-digit PIN");
        auth.changePin(accountId, oldPin, newPin);
        System.out.println("PIN changed.");
    }

    // ---------- helpers ----------

    private static void requirePin(String accountId) {
        String pin = ask("PIN");
        if (!auth.validatePin(accountId, pin)) {
            throw new IllegalArgumentException("Invalid PIN");
        }
    }

    private static BigDecimal readAmount(String prompt, boolean allowZero) {
        String text = ask(prompt);
        BigDecimal amount;
        try {
            amount = new BigDecimal(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid amount: enter a number");
        }
        if (amount.scale() > 2) {
            throw new IllegalArgumentException("Invalid amount: maximum 2 decimal places");
        }
        if (amount.signum() < 0 || (!allowZero && amount.signum() == 0)) {
            throw new IllegalArgumentException("Invalid amount: must be greater than zero");
        }
        return amount;
    }

    private static String ask(String prompt) {
        System.out.print(prompt + ": ");
        return in.nextLine().trim();
    }

    private static String shortId() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private static void safely(Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println("Error: " + e.getMessage());
        } catch (RuntimeException e) {
            System.out.println("Unexpected error: " + e.getMessage());
        }
    }
}
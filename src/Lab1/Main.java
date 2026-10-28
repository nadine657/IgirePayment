package Lab1;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Main {

    public static class PaymentManager {
        private Map<String, Customer> customers = new HashMap<>();
        private Map<String, Account> accounts = new HashMap<>();
        private List<Transaction> transactionHistory = new ArrayList<>();
        private Set<String> processedReferenceIds = new HashSet<>();
        private List<String> failedTransactionLogs = new ArrayList<>();

        public void addCustomer(Customer customer) {
            customers.put(customer.getCustomerId(), customer);
        }

        public void addAccount(Customer customer, Account account) {
            customer.addAccount(account);
            accounts.put(account.getAccountNumber(), account);
        }

        public boolean process(String referenceId, String accountNumber, BigDecimal amount, TransactionType type) {
            if (processedReferenceIds.contains(referenceId)) {
                failedTransactionLogs.add("DUPLICATE | ref=" + referenceId + " | account=" + accountNumber);
                return false;
            }

            Account account = accounts.get(accountNumber);
            if (account == null) {
                failedTransactionLogs.add("FAILED | ref=" + referenceId + " | account not found: " + accountNumber);
                return false;
            }

            Transaction transaction = new Transaction(referenceId, accountNumber, amount, type);
            try {
                account.processTransaction(transaction);
            } catch (IllegalArgumentException e) {
                failedTransactionLogs.add("FAILED | ref=" + referenceId + " | " + e.getMessage());
                return false;
            }

            processedReferenceIds.add(referenceId);
            transactionHistory.add(transaction);
            return true;
        }

        public Map<String, Customer> getCustomers() { return customers; }
        public Map<String, Account> getAccounts() { return accounts; }
        public List<Transaction> getTransactionHistory() { return transactionHistory; }
        public Set<String> getProcessedReferenceIds() { return processedReferenceIds; }
        public List<String> getFailedTransactionLogs() { return failedTransactionLogs; }
    }

    public static void main(String[] args) {
        PaymentManager manager = new PaymentManager();

        Customer nadine = new Customer("2005", "Nadine", "nadine@mail.com", "0796556854");
        manager.addCustomer(nadine);

        WalletAccount wallet = new WalletAccount("W001", "2005", new BigDecimal("75000"), new BigDecimal("1000000"));
        SavingsAccount savings = new SavingsAccount("S001", "2005", new BigDecimal("100000"),
                new BigDecimal("0.03"), new BigDecimal("0.01"), new BigDecimal("5000"), 2);
        manager.addAccount(nadine, wallet);
        manager.addAccount(nadine, savings);

        System.out.println(manager.process("REF-001", "W001", new BigDecimal("20000"), TransactionType.WITHDRAWAL));
        // client retries the same request after a timeout
        System.out.println(manager.process("REF-001", "W001", new BigDecimal("20000"), TransactionType.WITHDRAWAL));
        System.out.println(manager.process("REF-002", "W001", new BigDecimal("99999"), TransactionType.WITHDRAWAL));
        System.out.println(manager.process("REF-003", "S001", new BigDecimal("10000"), TransactionType.WITHDRAWAL));
        System.out.println(manager.process("REF-004", "S001", new BigDecimal("10000"), TransactionType.DEPOSIT));
        System.out.println(manager.process("REF-005", "S001", new BigDecimal("1000"), TransactionType.WITHDRAWAL));
        System.out.println(manager.process("REF-006", "S001", new BigDecimal("1000"), TransactionType.WITHDRAWAL));

        System.out.println("\n" + nadine);
        System.out.println(wallet);
        System.out.println(savings);
        System.out.println("\nHistory:");
        manager.getTransactionHistory().forEach(System.out::println);
        System.out.println("\nProcessed refs: " + manager.getProcessedReferenceIds());
        System.out.println("\nFailed logs:");
        manager.getFailedTransactionLogs().forEach(System.out::println);
    }
}
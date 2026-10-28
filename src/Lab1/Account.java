package Lab1;

import java.math.BigDecimal;

public abstract class Account {
    private String accountNumber;
    private String customerId;
    private BigDecimal balance;

    public Account() {
        this.balance = BigDecimal.ZERO;
    }

    public Account(String accountNumber, String customerId, BigDecimal balance) {
        this.accountNumber = accountNumber;
        this.customerId = customerId;
        this.balance = balance;
    }

    public void deposit(BigDecimal amount) {
        checkAmount(amount);
        balance = balance.add(amount);
    }

    public abstract void withdraw(BigDecimal amount);

    public void processTransaction(Transaction transaction) {
        switch (transaction.getType()) {
            case DEPOSIT:
                deposit(transaction.getAmount());
                break;
            case WITHDRAWAL:
                withdraw(transaction.getAmount());
                break;
            default:
                throw new IllegalArgumentException("Unsupported transaction type");
        }
    }

    protected void checkAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
    }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    @Override
    public String toString() {
        return "Account{number='" + accountNumber + "', customerId='" + customerId + "', balance=" + balance + "}";
    }
}
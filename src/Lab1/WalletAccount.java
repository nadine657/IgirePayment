package Lab1;

import java.math.BigDecimal;

public class WalletAccount extends Account {
    private BigDecimal transferLimit;

    public WalletAccount() {
        super();
        this.transferLimit = new BigDecimal("1000000");
    }

    public WalletAccount(String accountNumber, String customerId, BigDecimal balance, BigDecimal transferLimit) {
        super(accountNumber, customerId, balance);
        this.transferLimit = transferLimit;
    }

    @Override
    public void withdraw(BigDecimal amount) {
        checkAmount(amount);
        if (amount.compareTo(transferLimit) > 0) {
            throw new IllegalArgumentException("Amount exceeds wallet transfer limit of " + transferLimit);
        }
        if (getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient funds");
        }
        setBalance(getBalance().subtract(amount));
    }

    @Override
    public void processTransaction(Transaction transaction) {
        // wallet payments are instant, no extra restrictions
        super.processTransaction(transaction);
    }

    public BigDecimal getTransferLimit() { return transferLimit; }
    public void setTransferLimit(BigDecimal transferLimit) { this.transferLimit = transferLimit; }

    @Override
    public String toString() {
        return "WalletAccount{number='" + getAccountNumber() + "', customerId='" + getCustomerId()
                + "', balance=" + getBalance() + ", transferLimit=" + transferLimit + "}";
    }
}
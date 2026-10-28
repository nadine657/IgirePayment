package Lab1;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class Transaction {
    private String transactionId;
    private String referenceId;
    private String accountNumber;
    private BigDecimal amount;
    private TransactionType type;
    private LocalDateTime timestamp;

    public Transaction() {
    }

    public Transaction(String referenceId, String accountNumber, BigDecimal amount, TransactionType type) {
        this.transactionId = UUID.randomUUID().toString();
        this.referenceId = referenceId;
        this.accountNumber = accountNumber;
        this.amount = amount;
        this.type = type;
        this.timestamp = LocalDateTime.now();
    }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    @Override
    public String toString() {
        return "Transaction{id='" + transactionId + "', ref='" + referenceId + "', account='" + accountNumber
                + "', amount=" + amount + ", type=" + type + ", time=" + timestamp + "}";
    }
}
package Lab1;

import java.math.BigDecimal;

public class SavingsAccount extends Account {
    private BigDecimal interestRate;
    private BigDecimal withdrawalFeeRate;
    private BigDecimal minimumBalance;
    private int maxWithdrawals;
    private int withdrawalCount;

    public SavingsAccount() {
        super();
        this.interestRate = new BigDecimal("0.03");
        this.withdrawalFeeRate = new BigDecimal("0.01");
        this.minimumBalance = BigDecimal.ZERO;
        this.maxWithdrawals = 3;
    }

    public SavingsAccount(String accountNumber, String customerId, BigDecimal balance,
                          BigDecimal interestRate, BigDecimal withdrawalFeeRate,
                          BigDecimal minimumBalance, int maxWithdrawals) {
        super(accountNumber, customerId, balance);
        this.interestRate = interestRate;
        this.withdrawalFeeRate = withdrawalFeeRate;
        this.minimumBalance = minimumBalance;
        this.maxWithdrawals = maxWithdrawals;
    }

    @Override
    public void withdraw(BigDecimal amount) {
        checkAmount(amount);
        BigDecimal fee = amount.multiply(withdrawalFeeRate);
        BigDecimal total = amount.add(fee);
        if (getBalance().subtract(total).compareTo(minimumBalance) < 0) {
            throw new IllegalArgumentException("Withdrawal would take the balance below the minimum of " + minimumBalance);
        }
        setBalance(getBalance().subtract(total));
    }

    @Override
    public void processTransaction(Transaction transaction) {
        if (transaction.getType() == TransactionType.WITHDRAWAL) {
            if (withdrawalCount >= maxWithdrawals) {
                throw new IllegalArgumentException("Withdrawal limit reached (" + maxWithdrawals + ")");
            }
            super.processTransaction(transaction);
            withdrawalCount++;
        } else {
            super.processTransaction(transaction);
        }
    }

    public BigDecimal getInterestRate() { return interestRate; }
    public void setInterestRate(BigDecimal interestRate) { this.interestRate = interestRate; }

    public BigDecimal getWithdrawalFeeRate() { return withdrawalFeeRate; }
    public void setWithdrawalFeeRate(BigDecimal withdrawalFeeRate) { this.withdrawalFeeRate = withdrawalFeeRate; }

    public BigDecimal getMinimumBalance() { return minimumBalance; }
    public void setMinimumBalance(BigDecimal minimumBalance) { this.minimumBalance = minimumBalance; }

    public int getMaxWithdrawals() { return maxWithdrawals; }
    public void setMaxWithdrawals(int maxWithdrawals) { this.maxWithdrawals = maxWithdrawals; }

    public int getWithdrawalCount() { return withdrawalCount; }
    public void setWithdrawalCount(int withdrawalCount) { this.withdrawalCount = withdrawalCount; }

    @Override
    public String toString() {
        return "SavingsAccount{number='" + getAccountNumber() + "', customerId='" + getCustomerId()
                + "', balance=" + getBalance() + ", interestRate=" + interestRate
                + ", feeRate=" + withdrawalFeeRate + ", minBalance=" + minimumBalance
                + ", withdrawals=" + withdrawalCount + "/" + maxWithdrawals + "}";
    }
}
package org.katacode.pipeline.scenario;

/**
 * Real-world rich data payload representing a financial transactional event.
 */
public final class TransactionExchange {
    private final String transactionId;
    private final String accountCode;
    private final double amount;
    private final long timestamp;

    public TransactionExchange(String transactionId, String accountCode, double amount, long timestamp) {
        this.transactionId = transactionId;
        this.accountCode = accountCode;
        this.amount = amount;
        this.timestamp = timestamp;
    }

    public String getTransactionId() { return transactionId; }
    public String getAccountCode() { return accountCode; }
    public double getAmount() { return amount; }
    public long getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return String.format("TransactionExchange[ID=%s, Account=%s, Amount=$%.2f]", 
                transactionId, accountCode, amount);
    }

}

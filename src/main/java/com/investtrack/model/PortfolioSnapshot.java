package com.investtrack.model;

import java.time.LocalDateTime;

/**
 * Represents a snapshot of the portfolio's value at a specific point in time.
 * Used for tracking performance history and generating charts.
 */
public class PortfolioSnapshot {
    private final double totalValue;
    private final double cashBalance;
    private final LocalDateTime timestamp;
    
    /**
     * Creates a snapshot of the portfolio at the current time.
     * 
     * @param totalValue The total value of all holdings
     * @param cashBalance The available cash balance
     */
    public PortfolioSnapshot(double totalValue, double cashBalance) {
        this.totalValue = totalValue;
        this.cashBalance = cashBalance;
        this.timestamp = LocalDateTime.now();
    }
    
    /**
     * Gets the total assets value (holdings + cash) at the time of the snapshot
     * @return The total assets value
     */
    public double getTotalAssetValue() {
        return totalValue + cashBalance;
    }
    
    /**
     * Gets the timestamp when this snapshot was created
     * @return The timestamp of this snapshot
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    /**
     * Gets the total value of all holdings (excluding cash)
     * @return The total holdings value
     */
    public double getTotalValue() {
        return totalValue;
    }
    
    /**
     * Gets the cash balance at the time of this snapshot
     * @return The cash balance
     */
    public double getCashBalance() {
        return cashBalance;
    }
}

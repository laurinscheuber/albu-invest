package com.investtrack.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Represents the entire investment portfolio, containing a collection of {@link Holding} objects.
 * Provides methods to manage holdings and calculate the total portfolio value.
 */
public class Portfolio {

    /** The list storing all the individual holdings in the portfolio. */
    private final List<Holding> holdings;
    
    /** The cash balance available for investing. */
    private double cashBalance;
    
    /** The initial cash balance for resets. */
    private static final double INITIAL_CASH_BALANCE = 100_000_000.0; // 100 million

    /**
     * Constructs a new, empty Portfolio.
     * Initializes the internal list of holdings.
     */
    public Portfolio() {
        this.holdings = new ArrayList<>();
        this.cashBalance = INITIAL_CASH_BALANCE; // Set initial cash balance
    }

    /**
     * Returns an unmodifiable view of the holdings list.
     * This prevents external code from directly modifying the internal list.
     * Use {@link #addHolding(Holding)}, {@link #removeHolding(Holding)}, etc., for modifications.
     *
     * @return An unmodifiable {@link List} of {@link Holding} objects.
     */
    public List<Holding> getHoldings() {
        return Collections.unmodifiableList(holdings);
    }

    /**
     * Adds a new holding to the portfolio.
     * If the holding is null, the method does nothing.
     *
     * @param holding The {@link Holding} to add.
     */
    public void addHolding(Holding holding) {
        if (holding != null) {
            // Optional: Check if a holding with the same ID already exists
            if (holdings.stream().noneMatch(h -> h.getId().equals(holding.getId()))) {
                 this.holdings.add(holding);
            } else {
                // Handle duplicate ID case if necessary (e.g., log warning, throw exception)
                System.err.println("Warning: Attempted to add a holding with duplicate ID: " + holding.getId());
            }
        }
    }

    /**
     * Removes a specific holding from the portfolio based on its object reference or equality (ID).
     *
     * @param holding The {@link Holding} to remove.
     * @return {@code true} if the holding was found and removed, {@code false} otherwise.
     */
    public boolean removeHolding(Holding holding) {
        if (holding == null) {
            return false;
        }
        // Remove based on object equality (uses Holding.equals, which checks ID)
        return this.holdings.remove(holding);
    }

    /**
     * Removes a holding from the portfolio based on its unique ID.
     *
     * @param holdingId The unique ID string of the holding to remove.
     * @return {@code true} if a holding with the given ID was found and removed, {@code false} otherwise.
     */
    public boolean removeHoldingById(String holdingId) {
         if (holdingId == null || holdingId.trim().isEmpty()) {
            return false;
        }
        return this.holdings.removeIf(h -> h.getId().equals(holdingId));
    }


    /**
     * Finds a holding by its unique ID.
     *
     * @param holdingId The ID of the holding to find.
     * @return An {@link Optional} containing the {@link Holding} if found, or an empty Optional otherwise.
     */
     public Optional<Holding> findHoldingById(String holdingId) {
        if (holdingId == null) {
            return Optional.empty();
        }
        return this.holdings.stream()
                            .filter(h -> h.getId().equals(holdingId))
                            .findFirst();
    }


    /**
     * Calculates the total current value of all holdings in the portfolio.
     * It sums the {@link Holding#getCurrentValue()} for every holding.
     *
     * @return The total value as a double. Returns 0.0 if the portfolio is empty.
     */
    public double getTotalValue() {
        return holdings.stream()
                       .mapToDouble(Holding::getCurrentValue) // Use method reference
                       .sum();
    }

    /**
     * Clears all holdings from the portfolio.
     */
    public void clear() {
        this.holdings.clear();
    }
    
    /**
     * Resets the portfolio to its initial state with no holdings
     * and the initial cash balance of 100 million.
     */
    public void reset() {
        this.holdings.clear();
        this.cashBalance = INITIAL_CASH_BALANCE;
    }
    
    /**
     * Gets the current cash balance.
     * @return The available cash balance.
     */
    public double getCashBalance() {
        return cashBalance;
    }
    
    /**
     * Reduces the cash balance when purchasing assets.
     * @param amount The amount to deduct.
     * @return true if sufficient funds are available, false otherwise.
     */
    public boolean deductCash(double amount) {
        if (amount > cashBalance) {
            return false; // Insufficient funds
        }
        cashBalance -= amount;
        return true;
    }
    
    /**
     * Adds cash to the balance when selling assets.
     * @param amount The amount to add.
     */
    public void addCash(double amount) {
        cashBalance += amount;
    }
}

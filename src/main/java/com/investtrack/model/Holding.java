package com.investtrack.model;

import java.util.UUID;
import java.util.Objects;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

/**
 * Represents a single investment holding within the portfolio.
 * Each holding has details like symbol, name, quantity, price, and asset type.
 * It includes methods to calculate its current value.
 */
public class Holding {
    /** A unique identifier for the holding, generated automatically. */
    private final String id;
    /** The ticker symbol or identifier for the asset (e.g., AAPL, GOOGL). */
    private String symbol;
    /** The full name of the asset (e.g., Apple Inc., Google LLC). */
    private String name;
    /** The number of units held (e.g., shares, coins). */
    private double quantity;
    /** The current market price per unit. */
    private double pricePerUnit;
    /** The original purchase price per unit. */
    private double purchasePricePerUnit;
    /** The type of asset, categorized using the {@link AssetType} enum. */
    private AssetType assetType;
    /** History of price changes for this holding */
    private final List<PricePoint> priceHistory;

    /**
     * Default constructor. Initializes a holding with a unique ID.
     * Used primarily by frameworks like Gson for deserialization.
     */
    public Holding() {
        this.id = UUID.randomUUID().toString();
        this.priceHistory = new ArrayList<>();
    }

    /**
     * Constructs a new Holding with specified details.
     *
     * @param symbol The asset's ticker symbol.
     * @param name The asset's full name.
     * @param quantity The quantity held.
     * @param pricePerUnit The price per unit.
     * @param assetType The type of the asset.
     */
    public Holding(String symbol, String name, double quantity, double pricePerUnit, AssetType assetType) {
        this(); // Call default constructor to set ID
        this.symbol = symbol;
        this.name = name;
        this.quantity = quantity;
        this.pricePerUnit = pricePerUnit;
        this.purchasePricePerUnit = pricePerUnit; // Store original purchase price
        this.assetType = assetType;
        
        // Add initial price point to history
        addPricePoint(pricePerUnit);
    }

    /**
     * Add a price point to the history
     * @param price The price to record
     */
    public void addPricePoint(double price) {
        priceHistory.add(new PricePoint(price));
    }

    // --- Getters ---

    /** @return The unique identifier of this holding. */
    public String getId() { return id; }
    /** @return The ticker symbol of the asset. */
    public String getSymbol() { return symbol; }
    /** @return The full name of the asset. */
    public String getName() { return name; }
    /** @return The quantity of the asset held. */
    public double getQuantity() { return quantity; }
    /** @return The current price per unit of the asset. */
    public double getPricePerUnit() { return pricePerUnit; }
    /** @return The original purchase price per unit. */
    public double getPurchasePricePerUnit() { return purchasePricePerUnit; }
    /** @return The type of the asset (e.g., STOCK, BOND). */
    public AssetType getAssetType() { return assetType; }
    /** @return The price history of this asset */
    public List<PricePoint> getPriceHistory() { return priceHistory; }

    // --- Setters ---
    // Necessary for editing holdings and for frameworks like JavaFX TableView and Gson.

    /** Sets the ticker symbol. @param symbol The new symbol. */
    public void setSymbol(String symbol) { this.symbol = symbol; }
    /** Sets the full name. @param name The new name. */
    public void setName(String name) { this.name = name; }
    /** Sets the quantity. @param quantity The new quantity. */
    public void setQuantity(double quantity) { this.quantity = quantity; }
    /** 
     * Sets the current price per unit and records it in price history. 
     * @param pricePerUnit The new price per unit. 
     */
    public void setPricePerUnit(double pricePerUnit) { 
        this.pricePerUnit = pricePerUnit; 
        addPricePoint(pricePerUnit);
    }
    /** Sets the purchase price per unit. @param purchasePricePerUnit The purchase price. */
    public void setPurchasePricePerUnit(double purchasePricePerUnit) { this.purchasePricePerUnit = purchasePricePerUnit; }
    /** Sets the asset type. @param assetType The new asset type. */
    public void setAssetType(AssetType assetType) { this.assetType = assetType; }

    /**
     * Calculates the current total value of this holding.
     * @return The total value (quantity * price per unit).
     */
    public double getCurrentValue() {
        return quantity * pricePerUnit;
    }
    
    /**
     * Calculates the original purchase value of this holding.
     * @return The total purchase value (quantity * purchase price per unit).
     */
    public double getPurchaseValue() {
        return quantity * purchasePricePerUnit;
    }
    
    /**
     * Calculates the profit or loss for this holding.
     * @return The profit/loss amount (positive for profit, negative for loss)
     */
    public double getProfitLoss() {
        return getCurrentValue() - getPurchaseValue();
    }
    
    /**
     * Calculates the profit or loss percentage for this holding.
     * @return The profit/loss percentage
     */
    public double getProfitLossPercentage() {
        if (getPurchaseValue() == 0) return 0;
        return (getProfitLoss() / getPurchaseValue()) * 100;
    }
    
    /**
     * Gets the price change since purchase.
     * @return The difference between current price and purchase price
     */
    public double getPriceChange() {
        return pricePerUnit - purchasePricePerUnit;
    }
    
    /**
     * Gets the price change percentage since purchase.
     * @return The percentage change in price
     */
    public double getPriceChangePercentage() {
        if (purchasePricePerUnit == 0) return 0;
        return (getPriceChange() / purchasePricePerUnit) * 100;
    }

    // --- Object Overrides ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Holding)) return false;
        Holding holding = (Holding) o;
        return Objects.equals(id, holding.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return symbol + " - " + quantity + " units @ " + pricePerUnit;
    }
    
    /**
     * Inner class to represent a price point in time
     */
    public static class PricePoint {
        private final double price;
        private final LocalDateTime timestamp;
        
        public PricePoint(double price) {
            this.price = price;
            this.timestamp = LocalDateTime.now();
        }
        
        public double getPrice() {
            return price;
        }
        
        public LocalDateTime getTimestamp() {
            return timestamp;
        }
    }
}

package com.investtrack.model;

import java.util.UUID;
import java.util.Objects;

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
    /** The purchase price or current market price per unit. */
    private double pricePerUnit;
    /** The type of asset, categorized using the {@link AssetType} enum. */
    private AssetType assetType;

    /**
     * Default constructor. Initializes a holding with a unique ID.
     * Used primarily by frameworks like Gson for deserialization.
     */
    public Holding() {
        this.id = UUID.randomUUID().toString();
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
        this.assetType = assetType;
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
    /** @return The price per unit of the asset. */
    public double getPricePerUnit() { return pricePerUnit; }
    /** @return The type of the asset (e.g., STOCK, BOND). */
    public AssetType getAssetType() { return assetType; }

    // --- Setters ---
    // Necessary for editing holdings and for frameworks like JavaFX TableView and Gson.

    /** Sets the ticker symbol. @param symbol The new symbol. */
    public void setSymbol(String symbol) { this.symbol = symbol; }
    /** Sets the full name. @param name The new name. */
    public void setName(String name) { this.name = name; }
    /** Sets the quantity held. @param quantity The new quantity. */
    public void setQuantity(double quantity) { this.quantity = quantity; }
    /** Sets the price per unit. @param pricePerUnit The new price. */
    public void setPricePerUnit(double pricePerUnit) { this.pricePerUnit = pricePerUnit; }
    /** Sets the asset type. @param assetType The new asset type. */
    public void setAssetType(AssetType assetType) { this.assetType = assetType; }

    // --- Calculated Properties ---

    /**
     * Calculates the current total value of this holding.
     * @return The result of quantity multiplied by pricePerUnit.
     */
    public double getCurrentValue() {
        return quantity * pricePerUnit;
    }

    // --- Object Methods ---

    /**
     * Provides a string representation of the holding, typically including symbol and name.
     * @return A string summary of the holding.
     */
    @Override
    public String toString() {
        return "Holding{" +
               "id='" + id + '\'' + // Use single quotes within double quotes
               ", symbol='" + symbol + '\'' +
               ", name='" + name + '\'' +
               ", quantity=" + quantity +
               ", pricePerUnit=" + pricePerUnit +
               ", assetType=" + assetType +
               '}'; // Closing brace
    }

    /**
     * Checks if this holding is equal to another object.
     * Equality is based solely on the unique {@code id}.
     * @param o The object to compare with.
     * @return {@code true} if the objects are the same holding, {@code false} otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Holding holding = (Holding) o;
        return Objects.equals(id, holding.id); // Equality based on ID
    }

    /**
     * Generates a hash code for this holding.
     * Based solely on the unique {@code id}.
     * @return The hash code.
     */
    @Override
    public int hashCode() {
        return Objects.hash(id); // Hash code based on ID
    }
}

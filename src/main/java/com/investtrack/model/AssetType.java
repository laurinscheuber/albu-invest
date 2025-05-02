package com.investtrack.model;

/**
 * Enumerates the supported asset types within the portfolio.
 * This enum defines the different categories of investments that can be tracked.
 */
public enum AssetType {
    /** Represents common stocks traded on exchanges. */
    STOCK,
    /** Represents debt securities issued by governments or corporations. */
    BOND,
    /** Represents Exchange-Traded Funds, which track indices or sectors. */
    ETF,
    /** Represents mutual funds managed by investment companies. */
    FUND,
    /** Represents cryptocurrencies like Bitcoin or Ethereum. */
    CRYPTO,
    /** Represents any other type of asset not covered by the specific categories. */
    OTHER
}

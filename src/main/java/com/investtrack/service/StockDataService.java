package com.investtrack.service;

import com.investtrack.model.AssetType;
import com.investtrack.model.Holding;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Service class that provides predefined stock data and simulates price fluctuations.
 * It also tracks the initial purchase price to calculate performance metrics.
 */
public class StockDataService {
    // List of sample stocks with initial data
    private static final List<StockData> PREDEFINED_STOCKS = Arrays.asList(
            // US Tech
            new StockData("AAPL", "Apple Inc.", 175.25, AssetType.STOCK, "US Tech"),
            new StockData("MSFT", "Microsoft Corporation", 348.10, AssetType.STOCK, "US Tech"),
            new StockData("GOOGL", "Alphabet Inc.", 151.32, AssetType.STOCK, "US Tech"),
            new StockData("AMZN", "Amazon.com Inc.", 145.86, AssetType.STOCK, "US Tech"),
            new StockData("META", "Meta Platforms Inc.", 425.90, AssetType.STOCK, "US Tech"),
            new StockData("TSLA", "Tesla Inc.", 194.05, AssetType.STOCK, "US Tech"),
            new StockData("NVDA", "NVIDIA Corporation", 875.28, AssetType.STOCK, "US Tech"),
            new StockData("INTC", "Intel Corporation", 35.22, AssetType.STOCK, "US Tech"),
            new StockData("AMD", "Advanced Micro Devices", 158.76, AssetType.STOCK, "US Tech"),
            new StockData("CRM", "Salesforce Inc.", 275.50, AssetType.STOCK, "US Tech"),
            
            // US Financial
            new StockData("JPM", "JPMorgan Chase & Co.", 197.60, AssetType.STOCK, "US Financial"),
            new StockData("BAC", "Bank of America Corp", 38.25, AssetType.STOCK, "US Financial"),
            new StockData("WFC", "Wells Fargo & Co", 57.80, AssetType.STOCK, "US Financial"),
            new StockData("GS", "Goldman Sachs Group Inc", 456.72, AssetType.STOCK, "US Financial"),
            new StockData("MS", "Morgan Stanley", 98.35, AssetType.STOCK, "US Financial"),
            
            // US Healthcare
            new StockData("JNJ", "Johnson & Johnson", 147.62, AssetType.STOCK, "US Healthcare"),
            new StockData("PFE", "Pfizer Inc.", 28.15, AssetType.STOCK, "US Healthcare"),
            new StockData("MRK", "Merck & Co Inc.", 125.40, AssetType.STOCK, "US Healthcare"),
            new StockData("ABBV", "AbbVie Inc.", 185.27, AssetType.STOCK, "US Healthcare"),
            new StockData("UNH", "UnitedHealth Group Inc.", 520.15, AssetType.STOCK, "US Healthcare"),
            
            // European Stocks
            new StockData("NESN.SW", "Nestlé SA", 95.14, AssetType.STOCK, "European"),
            new StockData("ASML.AS", "ASML Holding", 878.30, AssetType.STOCK, "European"),
            new StockData("BAYN.DE", "Bayer AG", 27.89, AssetType.STOCK, "European"),
            new StockData("MC.PA", "LVMH", 725.60, AssetType.STOCK, "European"),
            new StockData("SAN.MC", "Banco Santander", 4.38, AssetType.STOCK, "European"),
            
            // Asian Stocks
            new StockData("9988.HK", "Alibaba Group", 87.20, AssetType.STOCK, "Asian"),
            new StockData("9984.T", "SoftBank Group", 8450.00, AssetType.STOCK, "Asian"),
            new StockData("005930.KS", "Samsung Electronics", 65700.00, AssetType.STOCK, "Asian"),
            new StockData("7203.T", "Toyota Motor", 2576.00, AssetType.STOCK, "Asian"),
            new StockData("000660.KS", "SK Hynix", 178000.00, AssetType.STOCK, "Asian"),
            
            // Payment Processors
            new StockData("V", "Visa Inc.", 274.45, AssetType.STOCK, "Payment"),
            new StockData("MA", "Mastercard Inc.", 454.75, AssetType.STOCK, "Payment"),
            new StockData("PYPL", "PayPal Holdings Inc.", 64.20, AssetType.STOCK, "Payment"),
            new StockData("AXP", "American Express Co.", 235.40, AssetType.STOCK, "Payment")
    );
    
    // Liste der vordefinierten Kryptowährungen
    private static final List<StockData> PREDEFINED_CRYPTO = Arrays.asList(
            // Major Cryptocurrencies
            new StockData("BTC", "Bitcoin", 68450.75, AssetType.CRYPTO, "Major"),
            new StockData("ETH", "Ethereum", 3580.25, AssetType.CRYPTO, "Major"),
            new StockData("BNB", "Binance Coin", 580.35, AssetType.CRYPTO, "Major"),
            new StockData("SOL", "Solana", 157.45, AssetType.CRYPTO, "Major"),
            new StockData("XRP", "Ripple", 0.55, AssetType.CRYPTO, "Major"),
            
            // DeFi Tokens
            new StockData("UNI", "Uniswap", 9.75, AssetType.CRYPTO, "DeFi"),
            new StockData("AAVE", "Aave", 95.35, AssetType.CRYPTO, "DeFi"),
            new StockData("MKR", "Maker", 1960.25, AssetType.CRYPTO, "DeFi"),
            new StockData("COMP", "Compound", 53.42, AssetType.CRYPTO, "DeFi"),
            new StockData("CRV", "Curve DAO Token", 0.57, AssetType.CRYPTO, "DeFi"),
            
            // Alt Coins
            new StockData("ADA", "Cardano", 0.45, AssetType.CRYPTO, "Alt Coins"),
            new StockData("DOT", "Polkadot", 6.85, AssetType.CRYPTO, "Alt Coins"),
            new StockData("AVAX", "Avalanche", 32.50, AssetType.CRYPTO, "Alt Coins"),
            new StockData("MATIC", "Polygon", 0.68, AssetType.CRYPTO, "Alt Coins"),
            new StockData("LINK", "Chainlink", 13.75, AssetType.CRYPTO, "Alt Coins"),
            
            // Meme Coins
            new StockData("DOGE", "Dogecoin", 0.12, AssetType.CRYPTO, "Meme"),
            new StockData("SHIB", "Shiba Inu", 0.000018, AssetType.CRYPTO, "Meme"),
            new StockData("PEPE", "Pepe", 0.0000097, AssetType.CRYPTO, "Meme"),
            new StockData("FLOKI", "Floki Inu", 0.00016, AssetType.CRYPTO, "Meme"),
            new StockData("BONK", "Bonk", 0.0000281, AssetType.CRYPTO, "Meme")
    );
    
    // Liste der vordefinierten Fonds
    private static final List<StockData> PREDEFINED_FUNDS = Arrays.asList(
            // Index Funds
            new StockData("VTSAX", "Vanguard Total Stock Market Index", 125.45, AssetType.FUND, "US Index"),
            new StockData("VFIAX", "Vanguard 500 Index Fund", 475.80, AssetType.FUND, "US Index"),
            new StockData("FXAIX", "Fidelity 500 Index Fund", 173.50, AssetType.FUND, "US Index"),
            new StockData("VTIAX", "Vanguard Total International Stock Index", 32.65, AssetType.FUND, "International"),
            new StockData("FSMAX", "Fidelity Extended Market Index", 75.85, AssetType.FUND, "US Index"),
            
            // Bond Funds
            new StockData("VBTLX", "Vanguard Total Bond Market Index", 10.45, AssetType.FUND, "Bonds"),
            new StockData("PTTRX", "PIMCO Total Return Fund", 10.15, AssetType.FUND, "Bonds"),
            new StockData("VWEHX", "Vanguard High-Yield Corporate", 5.35, AssetType.FUND, "Bonds"),
            new StockData("VGSLX", "Vanguard Real Estate Index", 120.35, AssetType.FUND, "Real Estate"),
            
            // Sector & Specialized
            new StockData("FDIVX", "Fidelity Diversified International Fund", 45.35, AssetType.FUND, "International"),
            new StockData("VGHCX", "Vanguard Health Care Fund", 237.42, AssetType.FUND, "Sector"),
            new StockData("VWUSX", "Vanguard U.S. Growth Fund", 152.67, AssetType.FUND, "Growth"),
            new StockData("VWNFX", "Vanguard Windsor Fund", 78.35, AssetType.FUND, "Value"),
            new StockData("DODGX", "Dodge & Cox Stock Fund", 245.65, AssetType.FUND, "Value"),
            new StockData("TRBCX", "T. Rowe Price Blue Chip Growth Fund", 169.75, AssetType.FUND, "Growth")
    );
    
    // Liste der vordefinierten ETFs
    private static final List<StockData> PREDEFINED_ETFS = Arrays.asList(
            // US Market ETFs
            new StockData("SPY", "SPDR S&P 500 ETF Trust", 478.25, AssetType.ETF, "US Market"),
            new StockData("VOO", "Vanguard S&P 500 ETF", 475.85, AssetType.ETF, "US Market"),
            new StockData("VTI", "Vanguard Total Stock Market ETF", 245.35, AssetType.ETF, "US Market"),
            new StockData("IVV", "iShares Core S&P 500 ETF", 475.85, AssetType.ETF, "US Market"),
            new StockData("QQQ", "Invesco QQQ Trust", 425.65, AssetType.ETF, "US Tech"),
            
            // International ETFs
            new StockData("VEA", "Vanguard FTSE Developed Markets ETF", 48.65, AssetType.ETF, "International"),
            new StockData("VWO", "Vanguard FTSE Emerging Markets ETF", 42.30, AssetType.ETF, "International"),
            new StockData("IEFA", "iShares Core MSCI EAFE ETF", 72.50, AssetType.ETF, "International"),
            new StockData("IEMG", "iShares Core MSCI Emerging Markets ETF", 52.35, AssetType.ETF, "International"),
            new StockData("EFA", "iShares MSCI EAFE ETF", 78.45, AssetType.ETF, "International"),
            
            // Bond ETFs
            new StockData("AGG", "iShares Core U.S. Aggregate Bond ETF", 98.75, AssetType.ETF, "Bonds"),
            new StockData("BND", "Vanguard Total Bond Market ETF", 72.56, AssetType.ETF, "Bonds"),
            new StockData("LQD", "iShares iBoxx $ Investment Grade Corp Bond ETF", 108.45, AssetType.ETF, "Bonds"),
            
            // Sector ETFs
            new StockData("XLF", "Financial Select Sector SPDR Fund", 39.75, AssetType.ETF, "Sector"),
            new StockData("XLK", "Technology Select Sector SPDR Fund", 195.65, AssetType.ETF, "Sector"),
            new StockData("XLE", "Energy Select Sector SPDR Fund", 86.35, AssetType.ETF, "Sector"),
            new StockData("XLV", "Health Care Select Sector SPDR Fund", 133.45, AssetType.ETF, "Sector"),
            new StockData("XLY", "Consumer Discretionary Select SPDR Fund", 175.36, AssetType.ETF, "Sector"),
            
            // Specialized ETFs
            new StockData("ARKK", "ARK Innovation ETF", 48.75, AssetType.ETF, "Specialized"),
            new StockData("ICLN", "iShares Global Clean Energy ETF", 16.85, AssetType.ETF, "Specialized"),
            new StockData("REET", "iShares Global REIT ETF", 23.65, AssetType.ETF, "Real Estate"),
            new StockData("VNQ", "Vanguard Real Estate ETF", 84.35, AssetType.ETF, "Real Estate"),
            new StockData("GLD", "SPDR Gold Shares", 194.65, AssetType.ETF, "Commodities"),
            new StockData("SLV", "iShares Silver Trust", 25.45, AssetType.ETF, "Commodities")
    );
    
    // Kombinierte Liste aller vordefinierten Assets
    private static final List<StockData> ALL_PREDEFINED_ASSETS = new ArrayList<>();
    
    static {
        ALL_PREDEFINED_ASSETS.addAll(PREDEFINED_STOCKS);
        ALL_PREDEFINED_ASSETS.addAll(PREDEFINED_CRYPTO);
        ALL_PREDEFINED_ASSETS.addAll(PREDEFINED_FUNDS);
        ALL_PREDEFINED_ASSETS.addAll(PREDEFINED_ETFS);
    }

    // Map to track price history (purchase price, updates, etc.)
    private final Map<String, PriceHistory> priceHistoryMap = new HashMap<>();
    
    // Callback for UI updates when prices change
    private Consumer<List<StockData>> priceUpdateCallback;
    
    // Timer for simulating price changes
    private ScheduledExecutorService scheduler;
    
    // Random for price fluctuations
    private final Random random = new Random();
    
    // Singleton instance
    private static StockDataService instance;
    
    // Private constructor for singleton
    private StockDataService() {
        // Initialize price history for all predefined stocks
        for (StockData stock : ALL_PREDEFINED_ASSETS) {
            priceHistoryMap.put(stock.getSymbol(), new PriceHistory(stock.getCurrentPrice()));
        }
    }
    
    /**
     * Get singleton instance
     */
    public static synchronized StockDataService getInstance() {
        if (instance == null) {
            instance = new StockDataService();
        }
        return instance;
    }

    /**
     * Gets all available predefined stocks
     */
    public List<StockData> getPredefinedStocks() {
        return new ArrayList<>(ALL_PREDEFINED_ASSETS);
    }
    
    /**
     * Gets predefined assets filtered by type
     */
    public List<StockData> getPredefinedAssetsByType(AssetType type) {
        return ALL_PREDEFINED_ASSETS.stream()
                .filter(asset -> asset.getAssetType() == type)
                .collect(Collectors.toList());
    }
    
    /**
     * Gets all available groups for a specific asset type
     */
    public List<String> getAssetGroups(AssetType type) {
        return ALL_PREDEFINED_ASSETS.stream()
                .filter(asset -> asset.getAssetType() == type)
                .map(StockData::getGroup)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }
    
    /**
     * Gets predefined assets filtered by type and group
     */
    public List<StockData> getPredefinedAssetsByTypeAndGroup(AssetType type, String group) {
        return ALL_PREDEFINED_ASSETS.stream()
                .filter(asset -> asset.getAssetType() == type && asset.getGroup().equals(group))
                .collect(Collectors.toList());
    }
    
    /**
     * Create a holding from predefined stock data
     */
    public Holding createHoldingFromStock(String symbol, double quantity) {
        Optional<StockData> stockOpt = ALL_PREDEFINED_ASSETS.stream()
                .filter(s -> s.getSymbol().equals(symbol))
                .findFirst();
                
        if (stockOpt.isPresent()) {
            StockData stock = stockOpt.get();
            return new Holding(
                stock.getSymbol(),
                stock.getName(),
                quantity,
                stock.getCurrentPrice(),
                stock.getAssetType()
            );
        }
        
        return null; // Stock not found
    }
    
    /**
     * Get performance data for a specific stock
     */
    public PerformanceData getPerformanceData(String symbol) {
        PriceHistory history = priceHistoryMap.get(symbol);
        if (history == null) {
            return null;
        }
        
        StockData stock = ALL_PREDEFINED_ASSETS.stream()
                .filter(s -> s.getSymbol().equals(symbol))
                .findFirst()
                .orElse(null);
                
        if (stock == null) {
            return null;
        }
        
        double currentPrice = stock.getCurrentPrice();
        double purchasePrice = history.getPurchasePrice();
        double priceChange = currentPrice - purchasePrice;
        double percentChange = (priceChange / purchasePrice) * 100;
        
        return new PerformanceData(
            symbol,
            purchasePrice,
            currentPrice,
            priceChange,
            percentChange,
            history.getHighestPrice(),
            history.getLowestPrice(),
            history.getLastUpdateTime()
        );
    }
    
    /**
     * Start simulating price changes
     */
    public void startPriceSimulation(Consumer<List<StockData>> updateCallback) {
        this.priceUpdateCallback = updateCallback;
        
        // Create a scheduler with a single thread
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        
        // Schedule price updates every 5 seconds
        this.scheduler.scheduleAtFixedRate(
            this::updatePrices,
            5,   // Initial delay
            5,   // Period
            TimeUnit.SECONDS
        );
    }
    
    /**
     * Stop the price simulation
     */
    public void stopPriceSimulation() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }
    
    /**
     * Update prices with random fluctuations
     */
    private void updatePrices() {
        // Skip update if no callback is registered
        if (priceUpdateCallback == null) {
            return;
        }
        
        // Update each asset's price with small random change
        for (StockData asset : ALL_PREDEFINED_ASSETS) {
            double currentPrice = asset.getCurrentPrice();
            
            // Different volatility based on asset type
            double volatility;
            
            // Apply super high volatility to meme coins
            if (asset.getAssetType() == AssetType.CRYPTO && "Meme".equals(asset.getGroup())) {
                volatility = 25.0; // Much higher volatility for meme coins (25%)
            } else {
                switch (asset.getAssetType()) {
                    case CRYPTO:
                        volatility = 7.5; // Regular crypto volatility 
                        break;
                    case ETF:
                    case FUND:
                        volatility = 1.5; // Funds and ETFs are less volatile
                        break;
                    default:
                        volatility = 2.5; // Standard for stocks
                }
            }
            
            // Generate random percent change between -volatility% and +volatility%
            double percentChange = (random.nextDouble() * volatility * 2.0) - volatility;  
            double changeAmount = currentPrice * (percentChange / 100.0);
            
            // Update the price
            double newPrice = Math.max(0.01, currentPrice + changeAmount);
            asset.setCurrentPrice(newPrice);
            
            // Update price history
            PriceHistory history = priceHistoryMap.get(asset.getSymbol());
            if (history != null) {
                history.updatePrice(newPrice);
            }
        }
        
        // Notify callback with updated assets
        priceUpdateCallback.accept(ALL_PREDEFINED_ASSETS);
    }
    
    /**
     * Static data class for a stock
     */
    public static class StockData {
        private final String symbol;
        private final String name;
        private double currentPrice;
        private final AssetType assetType;
        private final String group;
        
        public StockData(String symbol, String name, double initialPrice, AssetType assetType, String group) {
            this.symbol = symbol;
            this.name = name;
            this.currentPrice = initialPrice;
            this.assetType = assetType;
            this.group = group;
        }
        
        public String getSymbol() { return symbol; }
        public String getName() { return name; }
        public double getCurrentPrice() { return currentPrice; }
        public AssetType getAssetType() { return assetType; }
        public String getGroup() { return group; }
        public void setCurrentPrice(double price) { this.currentPrice = price; }
    }
    
    /**
     * Class to track price history for a stock
     */
    private static class PriceHistory {
        private final double purchasePrice;
        private double highestPrice;
        private double lowestPrice;
        private Date lastUpdateTime;
        
        public PriceHistory(double initialPrice) {
            this.purchasePrice = initialPrice;
            this.highestPrice = initialPrice;
            this.lowestPrice = initialPrice;
            this.lastUpdateTime = new Date();
        }
        
        public void updatePrice(double newPrice) {
            if (newPrice > highestPrice) {
                highestPrice = newPrice;
            }
            if (newPrice < lowestPrice) {
                lowestPrice = newPrice;
            }
            lastUpdateTime = new Date();
        }
        
        public double getPurchasePrice() { return purchasePrice; }
        public double getHighestPrice() { return highestPrice; }
        public double getLowestPrice() { return lowestPrice; }
        public Date getLastUpdateTime() { return lastUpdateTime; }
    }
    
    /**
     * Performance metrics for a stock
     */
    public static class PerformanceData {
        private final String symbol;
        private final double purchasePrice;
        private final double currentPrice;
        private final double priceChange;
        private final double percentChange;
        private final double highestPrice;
        private final double lowestPrice;
        private final Date lastUpdateTime;
        
        public PerformanceData(String symbol, double purchasePrice, double currentPrice, 
                              double priceChange, double percentChange, 
                              double highestPrice, double lowestPrice, Date lastUpdateTime) {
            this.symbol = symbol;
            this.purchasePrice = purchasePrice;
            this.currentPrice = currentPrice;
            this.priceChange = priceChange;
            this.percentChange = percentChange;
            this.highestPrice = highestPrice;
            this.lowestPrice = lowestPrice;
            this.lastUpdateTime = lastUpdateTime;
        }
        
        public String getSymbol() { return symbol; }
        public double getPurchasePrice() { return purchasePrice; }
        public double getCurrentPrice() { return currentPrice; }
        public double getPriceChange() { return priceChange; }
        public double getPercentChange() { return percentChange; }
        public double getHighestPrice() { return highestPrice; }
        public double getLowestPrice() { return lowestPrice; }
        public Date getLastUpdateTime() { return lastUpdateTime; }
        
        public boolean isPositivePerformance() {
            return priceChange >= 0;
        }
    }
}

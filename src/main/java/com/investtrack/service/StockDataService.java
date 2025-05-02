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
            new StockData("AAPL", "Apple Inc.", 175.25, AssetType.STOCK),
            new StockData("MSFT", "Microsoft Corporation", 348.10, AssetType.STOCK),
            new StockData("GOOGL", "Alphabet Inc.", 151.32, AssetType.STOCK),
            new StockData("AMZN", "Amazon.com Inc.", 145.86, AssetType.STOCK),
            new StockData("META", "Meta Platforms Inc.", 425.90, AssetType.STOCK),
            new StockData("TSLA", "Tesla Inc.", 194.05, AssetType.STOCK),
            new StockData("NVDA", "NVIDIA Corporation", 875.28, AssetType.STOCK),
            new StockData("JPM", "JPMorgan Chase & Co.", 197.60, AssetType.STOCK),
            new StockData("JNJ", "Johnson & Johnson", 147.62, AssetType.STOCK),
            new StockData("V", "Visa Inc.", 274.45, AssetType.STOCK)
    );
    
    // Liste der vordefinierten Kryptowährungen
    private static final List<StockData> PREDEFINED_CRYPTO = Arrays.asList(
            new StockData("BTC", "Bitcoin", 68450.75, AssetType.CRYPTO),
            new StockData("ETH", "Ethereum", 3580.25, AssetType.CRYPTO),
            new StockData("BNB", "Binance Coin", 580.35, AssetType.CRYPTO),
            new StockData("SOL", "Solana", 157.45, AssetType.CRYPTO),
            new StockData("ADA", "Cardano", 0.45, AssetType.CRYPTO),
            new StockData("DOT", "Polkadot", 6.85, AssetType.CRYPTO),
            new StockData("XRP", "Ripple", 0.55, AssetType.CRYPTO),
            new StockData("DOGE", "Dogecoin", 0.12, AssetType.CRYPTO),
            new StockData("AVAX", "Avalanche", 32.50, AssetType.CRYPTO),
            new StockData("LINK", "Chainlink", 13.75, AssetType.CRYPTO)
    );
    
    // Liste der vordefinierten Fonds
    private static final List<StockData> PREDEFINED_FUNDS = Arrays.asList(
            new StockData("VTSAX", "Vanguard Total Stock Market Index", 125.45, AssetType.FUND),
            new StockData("VFIAX", "Vanguard 500 Index Fund", 475.80, AssetType.FUND),
            new StockData("VBTLX", "Vanguard Total Bond Market Index", 10.45, AssetType.FUND),
            new StockData("VTIAX", "Vanguard Total International Stock Index", 32.65, AssetType.FUND),
            new StockData("VGSLX", "Vanguard Real Estate Index", 120.35, AssetType.FUND),
            new StockData("FXAIX", "Fidelity 500 Index Fund", 173.50, AssetType.FUND),
            new StockData("FSMAX", "Fidelity Extended Market Index", 75.85, AssetType.FUND),
            new StockData("FDIVX", "Fidelity Diversified International Fund", 45.35, AssetType.FUND),
            new StockData("PTTRX", "PIMCO Total Return Fund", 10.15, AssetType.FUND),
            new StockData("DODGX", "Dodge & Cox Stock Fund", 245.65, AssetType.FUND)
    );
    
    // Liste der vordefinierten ETFs
    private static final List<StockData> PREDEFINED_ETFS = Arrays.asList(
            new StockData("SPY", "SPDR S&P 500 ETF Trust", 478.25, AssetType.ETF),
            new StockData("QQQ", "Invesco QQQ Trust", 425.65, AssetType.ETF),
            new StockData("VTI", "Vanguard Total Stock Market ETF", 245.35, AssetType.ETF),
            new StockData("VOO", "Vanguard S&P 500 ETF", 475.85, AssetType.ETF),
            new StockData("AGG", "iShares Core U.S. Aggregate Bond ETF", 98.75, AssetType.ETF),
            new StockData("VEA", "Vanguard FTSE Developed Markets ETF", 48.65, AssetType.ETF),
            new StockData("VWO", "Vanguard FTSE Emerging Markets ETF", 42.30, AssetType.ETF),
            new StockData("IVV", "iShares Core S&P 500 ETF", 475.85, AssetType.ETF),
            new StockData("IEFA", "iShares Core MSCI EAFE ETF", 72.50, AssetType.ETF),
            new StockData("IEMG", "iShares Core MSCI Emerging Markets ETF", 52.35, AssetType.ETF)
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
            switch (asset.getAssetType()) {
                case CRYPTO:
                    volatility = 7.5; // Kryptowährungen sind volatiler
                    break;
                case ETF:
                case FUND:
                    volatility = 1.5; // Fonds und ETFs sind weniger volatil
                    break;
                default:
                    volatility = 2.5; // Standard für Aktien
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
        
        public StockData(String symbol, String name, double initialPrice, AssetType assetType) {
            this.symbol = symbol;
            this.name = name;
            this.currentPrice = initialPrice;
            this.assetType = assetType;
        }
        
        public String getSymbol() { return symbol; }
        public String getName() { return name; }
        public double getCurrentPrice() { return currentPrice; }
        public AssetType getAssetType() { return assetType; }
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

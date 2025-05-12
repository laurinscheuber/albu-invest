package com.investtrack.view;

import com.investtrack.model.AssetType;
import com.investtrack.model.Holding;
import com.investtrack.model.Portfolio;
import com.investtrack.model.PortfolioSnapshot;
import com.investtrack.persistence.PortfolioRepository;
import com.investtrack.service.StockDataService;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.nio.file.Paths;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.HashMap;

/**
 * Controller für das Hauptfenster (MainView.fxml).
 */
public class MainController {

    // --- FXML Injizierte Felder ---
    @FXML private TableView<Holding> holdingsTable;
    @FXML private TableColumn<Holding, String> colSymbol;
    @FXML private TableColumn<Holding, String> colName;
    @FXML private TableColumn<Holding, Double> colQty;
    @FXML private TableColumn<Holding, Double> colBuyPrice;  // Kaufpreis
    @FXML private TableColumn<Holding, Double> colPrice;     // Aktueller Preis
    @FXML private TableColumn<Holding, Double> colPriceChange; // Preisdifferenz
    @FXML private TableColumn<Holding, Double> colPriceChangePct; // Prozentuale Preisänderung
    @FXML private TableColumn<Holding, String> colValue;
    @FXML private TableColumn<Holding, Double> colProfitLoss; // Gewinn/Verlust für den Bestand
    @FXML private TableColumn<Holding, AssetType> colType;
    @FXML private TableColumn<Holding, Holding> colChart;    // Mini-Chart für jeden Bestand
    
    // Dashboard-Labels
    @FXML private Label lblTotal;            // Gesamtwert der Bestände
    @FXML private Label lblCashBalance;      // Verfügbares Bargeld
    @FXML private Label lblTotalAssets;      // Gesamtvermögen (Bestände + Bargeld)
    @FXML private Label lblProfitLoss;       // Gewinn/Verlust in CHF
    @FXML private Label lblProfitLossPercent; // Gewinn/Verlust in Prozent
    @FXML private Label lblStatus;           // Statusmeldung
    @FXML private Label lblLastUpdate;       // Zeitpunkt der letzten Aktualisierung
    
    // Mini-Performance-Chart
    @FXML private LineChart<String, Number> miniChart;
    
    // Neue Dashboard-Charts
    @FXML private PieChart allocationChart;
    @FXML private LineChart<String, Number> assetBreakdownChart;
    @FXML private LineChart<String, Number> extendedPerformanceChart;
    
    // Buttons
    @FXML private Button btnSell;
    @FXML private Button btnToggleTheme;  // Theme toggle button

    // --- Theme state (removed) ---

    // --- Datenmodell & Persistenz ---
    private PortfolioRepository repo;
    private Portfolio portfolio;
    private ObservableList<Holding> data;
    
    // --- Verfolgung der letzten Aktualisierungszeit ---
    private LocalDateTime lastUpdateTime;

    // --- Stock Data Service für die Preissimulation ---
    private final StockDataService stockDataService = StockDataService.getInstance();
    
    // --- Formatierung ---
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.getDefault());
    private static final NumberFormat PERCENT_FORMAT = NumberFormat.getPercentInstance(Locale.getDefault());
    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getNumberInstance(Locale.getDefault());
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    /**
     * Initialisiert den Controller.
     * Diese Methode wird automatisch aufgerufen, nachdem die FXML-Datei geladen wurde.
     */
    @FXML
    private void initialize() {
        // Anpassen der Zahlenformate
        PERCENT_FORMAT.setMinimumFractionDigits(2);
        NUMBER_FORMAT.setMinimumFractionDigits(2);
        NUMBER_FORMAT.setMaximumFractionDigits(6);
        
        // 1. Repository initialisieren und Daten laden
        repo = new PortfolioRepository(Paths.get(System.getProperty("user.home"), "investtrack.json"));
        portfolio = repo.load();
        data = FXCollections.observableArrayList(portfolio.getHoldings());
        
        // 2. Tabellenspalten konfigurieren
        configureTableColumns();
        
        // 3. Sortierung einrichten
        SortedList<Holding> sortedData = new SortedList<>(data);
        sortedData.comparatorProperty().bind(holdingsTable.comparatorProperty());
        holdingsTable.setItems(sortedData);
        
        // 4. Auswahlmodus festlegen
        holdingsTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        
        // 5. Initiale UI-Aktualisierung
        updateDashboard();
        updateEditDeleteButtonState();
        
        // Charts initialisieren
        initializeMiniChart();
        initializeAllocationChart();
        initializeAssetBreakdownChart();

        // 6. Listener für Auswahländerungen hinzufügen
        holdingsTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> updateEditDeleteButtonState()
        );

        // Remove theme toggle button functionality
        if (btnToggleTheme != null) {
            btnToggleTheme.setVisible(false);
        }

        // 7. Listener für Datenänderungen hinzufügen
        data.addListener((javafx.collections.ListChangeListener.Change<? extends Holding> c) -> {
            boolean changed = false;
            while (c.next()) {
                if (c.wasAdded() || c.wasRemoved() || c.wasUpdated()) {
                    changed = true;
                    // Behandlung von Hinzufügungen
                    if(c.wasAdded()){
                        c.getAddedSubList().forEach(h -> {
                            if(portfolio.findHoldingById(h.getId()).isEmpty()){
                                portfolio.addHolding(h);
                            }
                        });
                    }
                    // Behandlung von Entfernungen
                    if(c.wasRemoved()){
                         c.getRemoved().forEach(h -> portfolio.removeHoldingById(h.getId()));
                    }
                }
            }
            if (changed) {
                repo.save(portfolio);
                updateDashboard();
                holdingsTable.refresh();
            }
        });
        
        // 8. Preissimulation für Aktienbestände starten
        startStockPriceSimulation();
        
        // Anfängliche Aktualisierungszeit aufzeichnen
        lastUpdateTime = LocalDateTime.now();
        updateLastUpdateTime();
    }
    
    /**
     * Initializes the mini performance chart in the dashboard header with improved styling and zoom function
     */
    private void initializeMiniChart() {
        // Clear existing data
        miniChart.getData().clear();

        // Create series for portfolio value
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Portfolio Value");

        // Add historical data if available
        if (!portfolio.getPerformanceHistory().isEmpty()) {
            // Use the last 5 snapshots for better visible fluctuations (zoomed view)
            List<PortfolioSnapshot> history = portfolio.getPerformanceHistory();
            int startIndex = Math.max(0, history.size() - 5);

            double minValue = Double.MAX_VALUE;
            double maxValue = Double.MIN_VALUE;

            for (int i = startIndex; i < history.size(); i++) {
                PortfolioSnapshot snapshot = history.get(i);
                String timeLabel = snapshot.getTimestamp().format(TIME_FORMATTER);
                double value = snapshot.getTotalAssetValue();
                series.getData().add(new XYChart.Data<>(timeLabel, value));

                // Track min/max values for better Y-axis scaling
                minValue = Math.min(minValue, value);
                maxValue = Math.max(maxValue, value);
            }

            // Custom Y-axis range to improve visibility of fluctuations
            NumberAxis yAxis = (NumberAxis) miniChart.getYAxis();
            if (minValue != Double.MAX_VALUE && maxValue != Double.MIN_VALUE) {
                // Add 5% padding for better visibility
                double range = maxValue - minValue;
                double padding = range * 0.05;
                yAxis.setAutoRanging(false);
                yAxis.setLowerBound(Math.max(0, minValue - padding)); // Never go below zero
                yAxis.setUpperBound(maxValue + padding);
                yAxis.setTickUnit(range / 4); // Reasonable number of ticks
                yAxis.setMinorTickVisible(false);
            }
        } else {
            // Add current point if no history exists
            series.getData().add(new XYChart.Data<>("Now", portfolio.getTotalAssetValue()));
        }

        // Add series to chart
        miniChart.getData().add(series);

        // Modern chart styling
        miniChart.setAnimated(false);
        miniChart.setCreateSymbols(false);
        miniChart.setLegendVisible(false);

        // Background styling
        miniChart.lookup(".chart-plot-background").setStyle("-fx-background-color: transparent;");

        // Apply style class to data
        for (XYChart.Data<String, Number> data : series.getData()) {
            if (data.getNode() != null) {
                data.getNode().getStyleClass().add("chart-line-symbol");
            }
        }

        // Zoom functionality with context menu
        ContextMenu contextMenu = new ContextMenu();
        MenuItem zoomInItem = new MenuItem("Zoom In (Last 3 Points)");
        MenuItem zoomMediumItem = new MenuItem("Medium View (Last 5 Points)");
        MenuItem zoomOutItem = new MenuItem("Zoom Out (All Points)");

        zoomInItem.setOnAction(e -> updateMiniChartZoom(3));
        zoomMediumItem.setOnAction(e -> updateMiniChartZoom(5));
        zoomOutItem.setOnAction(e -> updateMiniChartZoom(0)); // 0 means all points

        contextMenu.getItems().addAll(zoomInItem, zoomMediumItem, zoomOutItem);
        miniChart.setOnContextMenuRequested(e -> contextMenu.show(miniChart, e.getScreenX(), e.getScreenY()));
    }
    
    /**
     * Aktualisiert das Mini-Chart mit dem angegebenen Zoom-Level
     * @param points Anzahl der anzuzeigenden Punkte (0 für alle)
     */
    private void updateMiniChartZoom(int points) {
        // Vorhandene Daten löschen
        miniChart.getData().clear();
        
        // Serie für den Portfolio-Wert erstellen
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Portfolio-Wert");
        
        // Wenn wir historische Daten haben, fügen wir sie dem Chart hinzu
        if (!portfolio.getPerformanceHistory().isEmpty()) {
            List<PortfolioSnapshot> history = portfolio.getPerformanceHistory();
            int startIndex = 0;
            
            if (points > 0) {
                // Startindex basierend auf dem Zoom-Level berechnen
                startIndex = Math.max(0, history.size() - points);
            }
            
            double minValue = Double.MAX_VALUE;
            double maxValue = Double.MIN_VALUE;
            
            for (int i = startIndex; i < history.size(); i++) {
                PortfolioSnapshot snapshot = history.get(i);
                String timeLabel = snapshot.getTimestamp().format(TIME_FORMATTER);
                double value = snapshot.getTotalAssetValue();
                series.getData().add(new XYChart.Data<>(timeLabel, value));
                
                // Min/Max-Werte für bessere Y-Achsen-Skalierung verfolgen
                minValue = Math.min(minValue, value);
                maxValue = Math.max(maxValue, value);
            }
            
            // Benutzerdefinierten Bereich für die Y-Achse festlegen, um die Sichtbarkeit von Schwankungen zu verbessern
            NumberAxis yAxis = (NumberAxis) miniChart.getYAxis();
            if (minValue != Double.MAX_VALUE && maxValue != Double.MIN_VALUE) {
                // 2% Padding für bessere Sichtbarkeit erstellen
                double range = maxValue - minValue;
                double padding = range * 0.02;
                yAxis.setAutoRanging(false);
                yAxis.setLowerBound(Math.max(0, minValue - padding)); // Nie unter Null gehen
                yAxis.setUpperBound(maxValue + padding);
                yAxis.setTickUnit(range / 4); // Vernünftige Anzahl von Ticks erstellen
            }
        } else {
            // Aktuellen Punkt hinzufügen, wenn keine Historie vorhanden ist
            series.getData().add(new XYChart.Data<>("Jetzt", portfolio.getTotalAssetValue()));
        }
        
        // Serie zum Chart hinzufügen
        miniChart.getData().add(series);
        
        // Serie stylen
        series.getNode().setStyle("-fx-stroke: #4caf50; -fx-stroke-width: 2px;");
    }
    
    /**
     * Initializes the asset allocation chart in the dashboard with improved styling
     */
    private void initializeAllocationChart() {
        // Clear existing data
        if (allocationChart != null) {
            allocationChart.getData().clear();

            // Populate chart if we have holdings
            if (!data.isEmpty()) {
                updateAllocationChart();
            } else {
                // Add a placeholder if no holdings exist
                PieChart.Data cashData = new PieChart.Data("Cash (100%)", portfolio.getCashBalance());
                allocationChart.getData().add(cashData);

                // Set modern styling for the cash segment
                if (cashData.getNode() != null) {
                    cashData.getNode().getStyleClass().add("pie-chart-cash");
                }

                // Add tooltip to show cash amount
                Tooltip tooltip = new Tooltip(
                    String.format("Cash Balance: %s", CURRENCY_FORMAT.format(portfolio.getCashBalance()))
                );
                tooltip.setStyle("-fx-font-size: 12px; -fx-font-weight: normal;");
                Tooltip.install(cashData.getNode(), tooltip);
            }

            // Apply modern styling to the chart
            allocationChart.setStyle("-fx-background-color: transparent;");
            allocationChart.setLegendVisible(false); // Hide legend since we use tooltips
            allocationChart.setAnimated(false); // Disable animations for better performance
            allocationChart.setLabelsVisible(false); // Hide labels for cleaner look
            allocationChart.setStartAngle(90); // Start angle for better visual appearance

            // Add click event for showing detailed allocation view
            allocationChart.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2) {
                    showChart(); // Show detailed chart on double-click
                }
            });

            // Add tooltip to instruct users about double-click functionality
            Tooltip chartTooltip = new Tooltip("Double-click to view detailed allocation");
            Tooltip.install(allocationChart, chartTooltip);
        }
    }
    
    /**
     * Updates the allocation chart with current portfolio data and modern styling
     */
    private void updateAllocationChart() {
        if (allocationChart == null) return;

        // Clear existing data
        allocationChart.getData().clear();

        double totalAssets = portfolio.getTotalAssetValue();
        double cashBalance = portfolio.getCashBalance();

        // Modern color palette with carefully selected colors for better visual distinction
        Map<AssetType, String> colorMap = new HashMap<>();
        colorMap.put(AssetType.CASH, "#4ECB71");      // Green for cash
        colorMap.put(AssetType.STOCK, "#2A3990");     // Blue for stocks
        colorMap.put(AssetType.CRYPTO, "#9C27B0");    // Purple for crypto
        colorMap.put(AssetType.ETF, "#FF9F43");       // Orange for ETFs
        colorMap.put(AssetType.FUND, "#3B4DB8");      // Light blue for funds
        colorMap.put(AssetType.BOND, "#4A5568");      // Gray for bonds
        colorMap.put(AssetType.OTHER, "#EA5455");     // Red for other assets

        // First add a segment for cash if we have cash
        if (cashBalance > 0) {
            double cashPercentage = (cashBalance / totalAssets) * 100;
            PieChart.Data cashData = new PieChart.Data(
                String.format("Cash (%.1f%%)", cashPercentage),
                cashBalance
            );
            allocationChart.getData().add(cashData);

            // Apply stylesheet class for better styling
            if (cashData.getNode() != null) {
                cashData.getNode().getStyleClass().add("cash-slice");

                // Also apply direct color styling as fallback
                cashData.getNode().setStyle("-fx-pie-color: " + colorMap.get(AssetType.CASH) + ";");
            }

            // Enhanced tooltip with better formatting
            Tooltip tooltip = new Tooltip(
                String.format("Cash Balance: %s (%.1f%%)",
                    CURRENCY_FORMAT.format(portfolio.getCashBalance()),
                    cashPercentage)
            );
            tooltip.setStyle("-fx-font-size: 12px;");
            Tooltip.install(cashData.getNode(), tooltip);
        }

        // Group holdings by type and calculate total value by type
        Map<AssetType, Double> assetTypeValues = data.stream()
            .collect(Collectors.groupingBy(
                Holding::getAssetType,
                Collectors.summingDouble(Holding::getCurrentValue)
            ));

        // Create a list for sorting by value (largest first)
        List<Map.Entry<AssetType, Double>> sortedEntries = new ArrayList<>(assetTypeValues.entrySet());
        sortedEntries.sort(Map.Entry.<AssetType, Double>comparingByValue().reversed());

        // Then add segments for each asset type with modern colors
        for (Map.Entry<AssetType, Double> entry : sortedEntries) {
            AssetType type = entry.getKey();
            double value = entry.getValue();

            // Skip if already covered (cash) or value is negligible
            if (type == AssetType.CASH || value < 0.01) continue;

            double percentage = (value / totalAssets) * 100;
            PieChart.Data assetData = new PieChart.Data(
                String.format("%s (%.1f%%)", type, percentage),
                value
            );
            allocationChart.getData().add(assetData);

            // Apply modern styling
            if (assetData.getNode() != null) {
                // Apply style class
                assetData.getNode().getStyleClass().add(type.toString().toLowerCase() + "-slice");

                // Apply color from our palette as a fallback
                String color = colorMap.getOrDefault(type, "#4A5568"); // Default gray if type not in map
                assetData.getNode().setStyle("-fx-pie-color: " + color + ";");
            }

            // Enhanced tooltip with better formatting
            Tooltip tooltip = new Tooltip(
                String.format("%s: %s (%.1f%%)",
                    type,
                    CURRENCY_FORMAT.format(value),
                    percentage)
            );
            tooltip.setStyle("-fx-font-size: 12px;");
            Tooltip.install(assetData.getNode(), tooltip);
        }

        // Apply final styling to the chart
        allocationChart.setStartAngle(90);
        allocationChart.setClockwise(true);
        allocationChart.setLabelsVisible(false);
    }
    
    /**
     * Initialisiert das Asset-Breakdown-Chart, das Bargeld, Aktien, Krypto und Gesamtwerte im Zeitverlauf anzeigt
     * mit verbesserter Visualisierung
     */
    private void initializeAssetBreakdownChart() {
        if (assetBreakdownChart == null) return;
        
        // Vorhandene Daten löschen
        assetBreakdownChart.getData().clear();
        
        // Historische Daten aus Portfolio-Snapshots abrufen
        List<PortfolioSnapshot> history = portfolio.getPerformanceHistory();
        
        if (history.isEmpty()) {
            // Anstelle der Verwendung von setPlaceholder, das für LineChart nicht verfügbar ist,
            // zeigen wir einfach ein leeres Chart mit einem Label im übergeordneten Container an
            assetBreakdownChart.getData().clear();
            lblStatus.setText("Nicht genügend Daten, um das Asset-Breakdown-Chart anzuzeigen");
            return;
        }
        
        // Nur die letzten 7 Datenpunkte für mehr Übersichtlichkeit anzeigen
        int startIndex = Math.max(0, history.size() - 7);
        
        // Eine Map erstellen, um Asset-Werte nach Typ und Zeitstempel zu speichern
        Map<AssetType, XYChart.Series<String, Number>> seriesMap = new HashMap<>();
        
        // Gesamtvermögen-Serie erstellen
        XYChart.Series<String, Number> totalSeries = new XYChart.Series<>();
        totalSeries.setName("Gesamtvermögen");
        
        // Serien für verschiedene Asset-Typen mit ihren Farben initialisieren
        Map<AssetType, String> colorMap = new HashMap<>();
        colorMap.put(AssetType.CASH, "#4CAF50");     // Grün für Bargeld
        colorMap.put(AssetType.STOCK, "#2196F3");    // Blau für Aktien
        colorMap.put(AssetType.CRYPTO, "#9C27B0");   // Lila für Krypto
        colorMap.put(AssetType.FUND, "#FFC107");     // Bernstein für Fonds
        colorMap.put(AssetType.ETF, "#FF9800");      // Orange für ETFs
        colorMap.put(AssetType.BOND, "#795548");     // Braun für Anleihen
        
        // Serien für jeden Asset-Typ erstellen
        for (AssetType type : colorMap.keySet()) {
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName(type.toString());
            seriesMap.put(type, series);
        }
        
        // Maximaler Asset-Wert zur Skalierung des Charts
        double maxAssetValue = 0;
        
        // Jeden Snapshot verarbeiten
        for (int i = startIndex; i < history.size(); i++) {
            PortfolioSnapshot snapshot = history.get(i);
            String timeLabel = snapshot.getTimestamp().format(
                DateTimeFormatter.ofPattern("HH:mm")
            );
            
            // Gesamtvermögen-Datenpunkt hinzufügen
            double totalValue = snapshot.getTotalAssetValue();
            totalSeries.getData().add(new XYChart.Data<>(timeLabel, totalValue));
            maxAssetValue = Math.max(maxAssetValue, totalValue);
            
            // Bargeld-Datenpunkt hinzufügen
            double cashValue = snapshot.getCashBalance();
            seriesMap.get(AssetType.CASH).getData().add(new XYChart.Data<>(timeLabel, cashValue));
            
            // Berechnete Asset-Werte für das Datum verwenden
            // In einer realen Implementierung würde dies aus dem Asset-Typ-Breakdown des Snapshots kommen
            Map<AssetType, Double> assetValues = 
                calculateTypicalAssetDistribution(totalValue - cashValue, i - startIndex);
            
            // Datenpunkte für jeden Asset-Typ hinzufügen
            for (Map.Entry<AssetType, Double> entry : assetValues.entrySet()) {
                if (entry.getKey() != AssetType.CASH && seriesMap.containsKey(entry.getKey())) {
                    seriesMap.get(entry.getKey()).getData().add(
                        new XYChart.Data<>(timeLabel, entry.getValue())
                    );
                }
            }
        }
        
        // Chart-Achsen für bessere Visualisierung konfigurieren
        NumberAxis yAxis = (NumberAxis) assetBreakdownChart.getYAxis();
        yAxis.setLabel("Wert (CHF)");
        yAxis.setAutoRanging(false);
        yAxis.setLowerBound(0);
        yAxis.setUpperBound(maxAssetValue * 1.05); // 5% Padding oben
        yAxis.setTickUnit(maxAssetValue / 5);
        
        CategoryAxis xAxis = (CategoryAxis) assetBreakdownChart.getXAxis();
        xAxis.setLabel("Zeit");
        
        // Jede Serie zum Chart hinzufügen und Styling anwenden
        for (AssetType type : seriesMap.keySet()) {
            XYChart.Series<String, Number> series = seriesMap.get(type);
            if (!series.getData().isEmpty()) {
                assetBreakdownChart.getData().add(series);
                // Farbstyling anwenden
                if (series.getNode() != null) {
                    series.getNode().setStyle("-fx-stroke: " + colorMap.get(type) + "; -fx-stroke-width: 2px;");
                }
            }
        }
        
        // Gesamtserie zuletzt hinzufügen, damit sie oben ist
        assetBreakdownChart.getData().add(totalSeries);
        if (totalSeries.getNode() != null) {
            totalSeries.getNode().setStyle("-fx-stroke: #F44336; -fx-stroke-width: 3px;"); // Rot für Gesamt (dicker)
        }
        
        // Legende hinzufügen
        assetBreakdownChart.setLegendVisible(true);
        assetBreakdownChart.setLegendSide(javafx.geometry.Side.TOP);
        
        // Animation für bessere Performance deaktivieren
        assetBreakdownChart.setAnimated(false);
    }
    
    /**
     * Hilfsmethode zur Generierung einer typischen Asset-Verteilung für Demonstrationszwecke
     * In einer realen App würde dies aus tatsächlichen historischen Daten stammen
     */
    private Map<AssetType, Double> calculateTypicalAssetDistribution(double totalInvestedValue, int timeOffset) {
        Map<AssetType, Double> result = new HashMap<>();
        
        // Eine realistisch aussehende Verteilung erstellen, die sich im Laufe der Zeit leicht ändert
        double stockBase = 0.45 + (timeOffset * 0.01); // Aktien wachsen leicht
        double cryptoBase = 0.25 - (timeOffset * 0.005); // Krypto nimmt leicht ab
        double fundBase = 0.20;
        double etfBase = 0.10 + (timeOffset * 0.002); // ETFs wachsen sehr leicht
        
        // Normalisieren, um sicherzustellen, dass sie 1.0 ergeben
        double sum = stockBase + cryptoBase + fundBase + etfBase;
        stockBase /= sum;
        cryptoBase /= sum;
        fundBase /= sum;
        etfBase /= sum;
        
        // Absolute Werte berechnen
        result.put(AssetType.STOCK, totalInvestedValue * stockBase);
        result.put(AssetType.CRYPTO, totalInvestedValue * cryptoBase);
        result.put(AssetType.FUND, totalInvestedValue * fundBase);
        result.put(AssetType.ETF, totalInvestedValue * etfBase);
        
        return result;
    }
    
    /**
     * Configures the cell value factories for each table column
     */
    private void configureTableColumns() {
        // Basic columns
        colSymbol.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getSymbol()));
        colName.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getName()));
        colType.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().getAssetType()));

        // Apply styling for dark mode compatibility
        colSymbol.setCellFactory(col -> new TableCell<Holding, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    // Set light text for dark mode
                    setStyle("-fx-text-fill: #F7FAFC;");
                }
            }
        });

        colName.setCellFactory(col -> new TableCell<Holding, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    // Set light text for dark mode
                    setStyle("-fx-text-fill: #F7FAFC;");
                }
            }
        });

        colType.setCellFactory(col -> new TableCell<Holding, AssetType>() {
            @Override
            protected void updateItem(AssetType item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.toString());
                    // Set light text for dark mode
                    setStyle("-fx-text-fill: #F7FAFC;");
                }
            }
        });

        // Quantity column with custom formatting
        colQty.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().getQuantity()));
        colQty.setCellFactory(col -> new TableCell<Holding, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(NUMBER_FORMAT.format(item));
                    setStyle("-fx-text-fill: #F7FAFC;");
                }
            }
        });
        
        // Buy price column (purchase price)
        colBuyPrice.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().getPurchasePricePerUnit()));
        colBuyPrice.setCellFactory(col -> new TableCell<Holding, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(CURRENCY_FORMAT.format(item));
                    setStyle("-fx-text-fill: #F7FAFC;");
                }
            }
        });

        // Current price column with custom formatting
        colPrice.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().getPricePerUnit()));
        colPrice.setCellFactory(col -> new TableCell<Holding, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(CURRENCY_FORMAT.format(item));
                    setStyle("-fx-text-fill: #F7FAFC;");
                }
            }
        });
        
        // Price change column (difference between current and purchase)
        colPriceChange.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().getPriceChange()));
        colPriceChange.setCellFactory(col -> new TableCell<Holding, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(CURRENCY_FORMAT.format(item));
                    // Color code: green for positive, red for negative
                    if (item > 0) {
                        setStyle("-fx-text-fill: green;");
                    } else if (item < 0) {
                        setStyle("-fx-text-fill: red;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });
        
        // Price change percentage
        colPriceChangePct.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().getPriceChangePercentage() / 100));
        colPriceChangePct.setCellFactory(col -> new TableCell<Holding, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(PERCENT_FORMAT.format(item));
                    // Color code: green for positive, red for negative
                    if (item > 0) {
                        setStyle("-fx-text-fill: green;");
                    } else if (item < 0) {
                        setStyle("-fx-text-fill: red;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });

        // Value column (current value = price * quantity)
        colValue.setCellValueFactory(cellData -> {
            double value = cellData.getValue().getCurrentValue();
            return new ReadOnlyStringWrapper(CURRENCY_FORMAT.format(value));
        });

        // Custom cell factory for text color in dark mode
        colValue.setCellFactory(col -> new TableCell<Holding, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #F7FAFC;");
                }
            }
        });
        
        // Profit/Loss column
        colProfitLoss.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().getProfitLoss()));
        colProfitLoss.setCellFactory(col -> new TableCell<Holding, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(CURRENCY_FORMAT.format(item));
                    // Color code: green for profit, red for loss
                    if (item > 0) {
                        setStyle("-fx-text-fill: green;");
                    } else if (item < 0) {
                        setStyle("-fx-text-fill: red;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });

        // Performance chart column using a LineChart
        colChart.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
        colChart.setCellFactory(col -> new TableCell<Holding, Holding>() {
            private LineChart<Number, Number> chart;
            
            @Override
            protected void updateItem(Holding holding, boolean empty) {
                super.updateItem(holding, empty);
                
                if (empty || holding == null) {
                    setGraphic(null);
                    return;
                }
                
                // Create a small line chart
                createMiniLineChart(holding);
                setGraphic(chart);
            }
            
            private void createMiniLineChart(Holding holding) {
                // X axis (time)
                NumberAxis xAxis = new NumberAxis();
                xAxis.setTickLabelsVisible(false);
                xAxis.setTickMarkVisible(false);
                xAxis.setMinorTickVisible(false);
                
                // Y axis (price)
                NumberAxis yAxis = new NumberAxis();
                yAxis.setTickLabelsVisible(false);
                yAxis.setTickMarkVisible(false);
                yAxis.setMinorTickVisible(false);
                
                // Create chart
                chart = new LineChart<>(xAxis, yAxis);
                chart.setLegendVisible(false);
                chart.setAnimated(false);
                chart.setCreateSymbols(false);
                chart.setPrefHeight(40);
                chart.setPrefWidth(130);
                chart.setMaxHeight(40);
                chart.setMaxWidth(130);
                chart.setPadding(new Insets(0));
                
                // Remove padding
                chart.getStyleClass().add("mini-chart");
                
                // Create data series
                XYChart.Series<Number, Number> series = new XYChart.Series<>();
                
                // Add price history points
                List<Holding.PricePoint> history = holding.getPriceHistory();
                if (history.size() > 0) {
                    // Only show last 10 points maximum
                    int startIndex = Math.max(0, history.size() - 10);
                    for (int i = startIndex; i < history.size(); i++) {
                        Holding.PricePoint point = history.get(i);
                        series.getData().add(new XYChart.Data<>(i, point.getPrice()));
                    }
                } else {
                    // If no history, just add current price
                    series.getData().add(new XYChart.Data<>(0, holding.getPricePerUnit()));
                }
                
                chart.getData().add(series);
                
                // Style the line based on performance (green for positive, red for negative)
                String colorStyle;
                if (holding.getPriceChange() > 0) {
                    colorStyle = "-fx-stroke: green;";
                } else if (holding.getPriceChange() < 0) {
                    colorStyle = "-fx-stroke: red;";
                } else {
                    colorStyle = "-fx-stroke: #666;";
                }
                
                series.getNode().setStyle(colorStyle + "-fx-stroke-width: 1.5px;");
            }
        });
    }

    /**
     * Updates the dashboard with current portfolio data
     */
    private void updateDashboard() {
        // Update basic labels
        lblTotal.setText(CURRENCY_FORMAT.format(portfolio.getTotalValue()));
        lblCashBalance.setText(CURRENCY_FORMAT.format(portfolio.getCashBalance()));
        lblTotalAssets.setText(CURRENCY_FORMAT.format(portfolio.getTotalAssetValue()));

        // Update profit/loss
        double profitLoss = portfolio.getProfitLoss();
        lblProfitLoss.setText(CURRENCY_FORMAT.format(profitLoss));

        // Apply style classes for profit/loss based on value
        if (profitLoss > 0) {
            lblProfitLoss.getStyleClass().removeAll("label-negative");
            lblProfitLoss.getStyleClass().add("label-positive");
        } else if (profitLoss < 0) {
            lblProfitLoss.getStyleClass().removeAll("label-positive");
            lblProfitLoss.getStyleClass().add("label-negative");
        } else {
            lblProfitLoss.getStyleClass().removeAll("label-positive", "label-negative");
        }

        // Update profit/loss percentage
        double profitLossPercent = portfolio.getProfitLossPercentage() / 100; // Convert to decimal for formatter
        lblProfitLossPercent.setText(PERCENT_FORMAT.format(profitLossPercent));

        // Apply style classes for percentage same as the monetary value
        if (profitLossPercent > 0) {
            lblProfitLossPercent.getStyleClass().removeAll("label-negative");
            lblProfitLossPercent.getStyleClass().add("label-positive");
        } else if (profitLossPercent < 0) {
            lblProfitLossPercent.getStyleClass().removeAll("label-positive");
            lblProfitLossPercent.getStyleClass().add("label-negative");
        } else {
            lblProfitLossPercent.getStyleClass().removeAll("label-positive", "label-negative");
        }

        // Take a new snapshot for the charts
        portfolio.takeSnapshot();

        // Update charts
        updateMiniChart();
        updateAllocationChart();
        initializeAssetBreakdownChart(); // Reinitialize with new data

        // Update status
        lblStatus.setText("Portfolio has " + data.size() + " holdings");

        // Update last update time
        updateLastUpdateTime();
    }
    
    /**
     * Updates the mini chart with current performance data
     */
    private void updateMiniChart() {
        // Take a new snapshot for the chart
        portfolio.takeSnapshot();
        
        // Refresh chart
        initializeMiniChart();
    }
    
    /**
     * Updates the label showing when data was last refreshed
     */
    private void updateLastUpdateTime() {
        lastUpdateTime = LocalDateTime.now();
        lblLastUpdate.setText("Last update: " + lastUpdateTime.format(TIME_FORMATTER));
    }

    private void updateEditDeleteButtonState() {
        boolean hasSelection = holdingsTable.getSelectionModel().getSelectedItem() != null;
        btnSell.setDisable(!hasSelection);
    }

    /**
     * Zeigt einen Dialog zum Zurücksetzen des Portfolios an.
     */
    @FXML
    private void resetPortfolio() {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Portfolio zurücksetzen");
        confirmAlert.setHeaderText("Portfolio wirklich zurücksetzen?");
        confirmAlert.setContentText("Alle Positionen werden gelöscht und das Guthaben auf 100 Millionen zurückgesetzt.");
        
        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            portfolio.reset();
            data.clear();
            repo.save(portfolio);
            updateDashboard();
            showInformationAlert("Portfolio zurückgesetzt", 
                "Das Portfolio wurde erfolgreich zurückgesetzt. Guthaben: " + 
                CURRENCY_FORMAT.format(portfolio.getCashBalance()));
        }
    }

    /**
     * Shows detailed chart for the selected holding with modern styling
     */
    @FXML
    private void showDetailedChart() {
        Holding selectedHolding = holdingsTable.getSelectionModel().getSelectedItem();
        if (selectedHolding == null) {
            showInformationAlert("No Selection", "Please select a holding to view its detailed chart.");
            return;
        }

        // Create a new stage for the detailed chart
        Stage stage = new Stage(StageStyle.DECORATED);
        stage.setTitle(selectedHolding.getSymbol() + " - Price History");
        stage.initModality(Modality.APPLICATION_MODAL);

        // Create chart container with card styling
        VBox chartContainer = new VBox();
        chartContainer.getStyleClass().add("card");
        chartContainer.setPadding(new Insets(20));
        chartContainer.setSpacing(20);

        // Add title
        Label titleLabel = new Label("Price History for " + selectedHolding.getName());
        titleLabel.getStyleClass().add("label-header");

        // Add subtitle with holding type and symbol
        Label subtitleLabel = new Label(selectedHolding.getAssetType() + " • " + selectedHolding.getSymbol());
        subtitleLabel.getStyleClass().add("label-subheader");

        // Create axes with improved styling
        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Time Points");
        xAxis.setTickLabelFill(Color.valueOf("#4A5568"));
        xAxis.setTickLabelGap(10);

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Price (" + CURRENCY_FORMAT.getCurrency().getSymbol() + ")");
        yAxis.setTickLabelFill(Color.valueOf("#4A5568"));

        // Create chart with modern styling
        LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setLegendVisible(false);
        lineChart.setCreateSymbols(true);
        lineChart.setAnimated(false);
        lineChart.getStyleClass().add("modern-chart");
        lineChart.setPrefHeight(350);

        // Create series with appropriate coloring
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("Price");

        // Add data points
        List<Holding.PricePoint> history = selectedHolding.getPriceHistory();
        for (int i = 0; i < history.size(); i++) {
            series.getData().add(new XYChart.Data<>(i, history.get(i).getPrice()));
        }

        // Get first and last prices to determine trend
        double firstPrice = history.isEmpty() ? 0 : history.get(0).getPrice();
        double lastPrice = history.isEmpty() ? 0 : history.get(history.size() - 1).getPrice();
        boolean isPositiveTrend = lastPrice >= firstPrice;

        // Add series to chart
        lineChart.getData().add(series);

        // Apply styling to the line based on trend
        if (series.getNode() != null) {
            if (isPositiveTrend) {
                series.getNode().setStyle("-fx-stroke: #4ECB71; -fx-stroke-width: 2px;"); // Green for positive
            } else {
                series.getNode().setStyle("-fx-stroke: #EA5455; -fx-stroke-width: 2px;"); // Red for negative
            }
        }

        // Apply styling to data points
        for (XYChart.Data<Number, Number> data : series.getData()) {
            if (data.getNode() != null) {
                if (isPositiveTrend) {
                    data.getNode().setStyle("-fx-background-color: #4ECB71, white; -fx-background-radius: 6px; -fx-padding: 5px;");
                } else {
                    data.getNode().setStyle("-fx-background-color: #EA5455, white; -fx-background-radius: 6px; -fx-padding: 5px;");
                }

                // Add tooltip for each data point
                int index = series.getData().indexOf(data);
                if (index < history.size()) {
                    Holding.PricePoint point = history.get(index);
                    Tooltip tooltip = new Tooltip(
                        String.format("Time: %s\nPrice: %s",
                            point.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                            CURRENCY_FORMAT.format(point.getPrice()))
                    );
                    Tooltip.install(data.getNode(), tooltip);
                }
            }
        }

        // Create metrics summary cards
        HBox metricsContainer = new HBox();
        metricsContainer.setSpacing(16);
        metricsContainer.setAlignment(javafx.geometry.Pos.CENTER);

        // Card for purchase price
        VBox purchaseCard = createMetricCard(
            "Purchase Price",
            CURRENCY_FORMAT.format(selectedHolding.getPurchasePricePerUnit()),
            null
        );

        // Card for current price
        VBox currentPriceCard = createMetricCard(
            "Current Price",
            CURRENCY_FORMAT.format(selectedHolding.getPricePerUnit()),
            null
        );

        // Card for price change with appropriate color
        String priceChangeStr = CURRENCY_FORMAT.format(selectedHolding.getPriceChange()) +
                " (" + PERCENT_FORMAT.format(selectedHolding.getPriceChangePercentage() / 100) + ")";
        VBox priceChangeCard = createMetricCard(
            "Price Change",
            priceChangeStr,
            selectedHolding.getPriceChange() > 0 ? "label-positive" :
                (selectedHolding.getPriceChange() < 0 ? "label-negative" : null)
        );

        // Card for total value
        VBox valueCard = createMetricCard(
            "Total Value",
            CURRENCY_FORMAT.format(selectedHolding.getCurrentValue()),
            null
        );

        // Card for profit/loss with appropriate color
        VBox profitLossCard = createMetricCard(
            "Profit/Loss",
            CURRENCY_FORMAT.format(selectedHolding.getProfitLoss()),
            selectedHolding.getProfitLoss() > 0 ? "label-positive" :
                (selectedHolding.getProfitLoss() < 0 ? "label-negative" : null)
        );

        // Add all cards to metrics container
        metricsContainer.getChildren().addAll(
            purchaseCard, currentPriceCard, priceChangeCard, valueCard, profitLossCard
        );

        // Add button to close with better styling
        Button closeButton = new Button("Close");
        closeButton.getStyleClass().add("button-primary");
        closeButton.setOnAction(e -> stage.close());

        HBox buttonBox = new HBox(closeButton);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));
        buttonBox.setAlignment(javafx.geometry.Pos.CENTER);

        // Combine all components
        chartContainer.getChildren().addAll(
            titleLabel,
            subtitleLabel,
            lineChart,
            metricsContainer,
            buttonBox
        );

        // Create layout with padding
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));
        root.setCenter(chartContainer);

        // Show the chart with CSS applied
        Scene scene = new Scene(root, 900, 650);
        scene.getStylesheets().add(getClass().getResource("/com/investtrack/view/ModernStyle.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Helper method to create a metric card for the detailed chart view
     */
    private VBox createMetricCard(String title, String value, String valueStyleClass) {
        VBox card = new VBox(5);
        card.getStyleClass().addAll("card", "metric-card");
        card.setPadding(new Insets(16));
        card.setAlignment(javafx.geometry.Pos.CENTER);
        card.setMinWidth(150);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("label-subheader");

        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("label-value");

        // Add additional style class if provided
        if (valueStyleClass != null) {
            valueLabel.getStyleClass().add(valueStyleClass);
        }

        card.getChildren().addAll(titleLabel, valueLabel);
        return card;
    }
    
    /**
     * Displays the allocation chart showing the distribution of assets.
     */
    @FXML
    public void showChart() {
        if (data.isEmpty()) {
            showInformationAlert("No Holdings", "There are no holdings to display in the chart.");
            return;
        }
        
        // Create the pie chart
        PieChart chart = new PieChart();
        chart.setTitle("Portfolio Allocation");
        
        // Group holdings by type and calculate total value by type
        data.stream()
            .collect(Collectors.groupingBy(Holding::getAssetType,
                    Collectors.summingDouble(Holding::getCurrentValue)))
            .forEach((type, value) -> chart.getData().add(new PieChart.Data(type.toString(), value)));
        
        // Sort slices by value (largest first)
        chart.getData().sort(Comparator.comparingDouble(PieChart.Data::getPieValue).reversed());

        // Apply Tooltips to show exact value on hover
        chart.getData().forEach(data -> {
            // Extract original name part before potential parenthesis
            String originalName = data.getName().split(" \\(")[0];
            String tooltipText = String.format("%s: %s", originalName, CURRENCY_FORMAT.format(data.getPieValue()));
            Tooltip tooltip = new Tooltip(tooltipText);
            Tooltip.install(data.getNode(), tooltip);

             // Update the label to include the percentage
             double percentage = (portfolio.getTotalValue() > 0) ? (data.getPieValue() / portfolio.getTotalValue()) * 100 : 0.0;
             String labelText = String.format("%s (%.1f%%)", originalName, percentage);
             data.setName(labelText);
        });

        // Display chart in a modal window
        Stage stage = new Stage(StageStyle.DECORATED);
        stage.setTitle("Portfolio Asset Allocation");
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setScene(new Scene(new BorderPane(chart), 600, 400));
        stage.show();
    }

    /**
     * Starts the stock price simulation that periodically updates prices
     */
    private void startStockPriceSimulation() {
        // Start simulation with a callback to update UI when prices change
        stockDataService.startPriceSimulation(updatedStocks -> {
            // Update UI on the JavaFX application thread
            javafx.application.Platform.runLater(() -> {
                // For each holding, check if its symbol matches a stock and update its price
                for (Holding holding : data) {
                    if (holding.getAssetType() == AssetType.STOCK || 
                        holding.getAssetType() == AssetType.ETF || 
                        holding.getAssetType() == AssetType.FUND ||
                        holding.getAssetType() == AssetType.CRYPTO) {
                        // Find matching stock
                        updatedStocks.stream()
                            .filter(stock -> stock.getSymbol().equals(holding.getSymbol()))
                            .findFirst()
                            .ifPresent(stock -> {
                                // Update the holding's price
                                holding.setPricePerUnit(stock.getCurrentPrice());
                            });
                    }
                }
                
                // Refresh UI
                holdingsTable.refresh();
                updateDashboard();
            });
        });
    }
    
    /**
     * Adds predefined assets from the StockDataService
     */
    @FXML
    public void addPredefinedStocksAction() {
        // Create dialog to select from predefined assets
        Dialog<StockSelectionResult> dialog = new Dialog<>();
        dialog.setTitle("Add Predefined Asset");
        dialog.setHeaderText("Select an asset by type, group, and name");
        
        // Set the button types
        ButtonType addButtonType = new ButtonType("Add to Portfolio", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);
        
        // Create the asset selection UI
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        
        // Asset Type ComboBox
        Label typeLabel = new Label("Asset Type:");
        ComboBox<AssetType> assetTypeCombo = new ComboBox<>();
        assetTypeCombo.getItems().addAll(AssetType.values());
        assetTypeCombo.setValue(AssetType.STOCK); // Default to stocks
        
        // Asset Group ComboBox (will be populated based on type)
        Label groupLabel = new Label("Asset Group:");
        ComboBox<String> groupCombo = new ComboBox<>();
        
        // Specific Asset ComboBox (will be populated based on group)
        Label assetLabel = new Label("Specific Asset:");
        ComboBox<StockDataService.StockData> assetCombo = new ComboBox<>();
        
        // Quantity field
        Label quantityLabel = new Label("Quantity:");
        TextField quantityField = new TextField("1"); // Default quantity
        
        // Preview section
        VBox previewBox = new VBox(5);
        previewBox.setStyle("-fx-padding: 10; -fx-border-color: #ddd; -fx-border-radius: 5;");
        
        Label previewTitle = new Label("Purchase Preview");
        previewTitle.setStyle("-fx-font-weight: bold;");
        
        Label previewPrice = new Label();
        Label previewTotal = new Label();
        Label previewBalance = new Label("Available Balance: " + CURRENCY_FORMAT.format(portfolio.getCashBalance()));
        
        previewBox.getChildren().addAll(previewTitle, previewPrice, previewTotal, previewBalance);
        
        // Initial population of groups
        populateGroups(assetTypeCombo.getValue(), groupCombo);
        
        // Add listeners
        assetTypeCombo.valueProperty().addListener((obs, oldType, newType) -> {
            if (newType != null) {
                populateGroups(newType, groupCombo);
                groupCombo.getSelectionModel().selectFirst(); // Select first group
            }
        });
        
        groupCombo.valueProperty().addListener((obs, oldGroup, newGroup) -> {
            if (newGroup != null) {
                populateAssets(assetTypeCombo.getValue(), newGroup, assetCombo);
                assetCombo.getSelectionModel().selectFirst(); // Select first asset
            }
        });
        
        assetCombo.valueProperty().addListener((obs, oldAsset, newAsset) -> {
            updatePreview(newAsset, quantityField.getText(), previewPrice, previewTotal);
        });
        
        quantityField.textProperty().addListener((obs, oldText, newText) -> {
            updatePreview(assetCombo.getValue(), newText, previewPrice, previewTotal);
        });
        
        // Create the layout
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        
        grid.add(typeLabel, 0, 0);
        grid.add(assetTypeCombo, 1, 0);
        
        grid.add(groupLabel, 0, 1);
        grid.add(groupCombo, 1, 1);
        
        grid.add(assetLabel, 0, 2);
        grid.add(assetCombo, 1, 2);
        
        grid.add(quantityLabel, 0, 3);
        grid.add(quantityField, 1, 3);
        
        content.getChildren().addAll(grid, previewBox);
        dialog.getDialogPane().setContent(content);
        
        // Enable/Disable Add button depending on selection
        Button addButton = (Button) dialog.getDialogPane().lookupButton(addButtonType);
        addButton.setDisable(true);
        
        assetCombo.getSelectionModel().selectedItemProperty().addListener(
            (observable, oldValue, newValue) -> addButton.setDisable(newValue == null));
        
        // Convert the result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                StockDataService.StockData selectedAsset = assetCombo.getValue();
                if (selectedAsset != null) {
                    try {
                        double quantity = Double.parseDouble(quantityField.getText().trim());
                        if (quantity <= 0) {
                            throw new NumberFormatException("Quantity must be positive");
                        }
                        return new StockSelectionResult(selectedAsset, quantity);
                    } catch (NumberFormatException e) {
                        // Show error
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Invalid Quantity");
                        alert.setHeaderText("Please enter a valid positive number for quantity");
                        alert.setContentText("Die Anzahl muss positiv sein und darf die vorhandene Anzahl nicht überschreiten.");
                        alert.showAndWait();
                        return null;
                    }
                }
            }
            return null;
        });
        
        // Initialize combo boxes
        if (!groupCombo.getItems().isEmpty()) {
            groupCombo.getSelectionModel().selectFirst();
        }
        
        // Show the dialog and process result
        Optional<StockSelectionResult> result = dialog.showAndWait();
        
        result.ifPresent(selection -> {
            // Calculate total cost
            double totalCost = selection.stock.getCurrentPrice() * selection.quantity;
            
            // Check if enough cash is available
            if (totalCost > portfolio.getCashBalance()) {
                showErrorAlert("Insufficient Funds", 
                    "The total cost is " + CURRENCY_FORMAT.format(totalCost) + 
                    ", but you only have " + CURRENCY_FORMAT.format(portfolio.getCashBalance()) + " available.",
                    "Please reduce the quantity or select a less expensive asset.");
                return;
            }
            
            // Create a new holding from the selected asset
            Holding newHolding = stockDataService.createHoldingFromStock(
                selection.stock.getSymbol(), 
                selection.quantity
            );
            
            if (newHolding != null) {
                // Deduct cash and add holding to portfolio
                portfolio.deductCash(totalCost);
                data.add(newHolding);
                updateDashboard();
                
                // Show success message
                lblStatus.setText("Added " + selection.quantity + " " + 
                                 selection.stock.getSymbol() + " for " + 
                                 CURRENCY_FORMAT.format(totalCost));
            }
        });
    }
    
    /**
     * Populates the group combo box based on asset type
     */
    private void populateGroups(AssetType type, ComboBox<String> groupCombo) {
        groupCombo.getItems().clear();
        List<String> groups = stockDataService.getAssetGroups(type);
        groupCombo.getItems().addAll(groups);
    }
    
    /**
     * Populates the asset combo box based on type and group
     */
    private void populateAssets(AssetType type, String group, ComboBox<StockDataService.StockData> assetCombo) {
        assetCombo.getItems().clear();
        List<StockDataService.StockData> assets = stockDataService.getPredefinedAssetsByTypeAndGroup(type, group);
        assetCombo.getItems().addAll(assets);
        
        // Custom cell factory for better display
        assetCombo.setCellFactory(param -> new ListCell<StockDataService.StockData>() {
            @Override
            protected void updateItem(StockDataService.StockData item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%s - %s (%s)", 
                        item.getSymbol(), 
                        item.getName(),
                        CURRENCY_FORMAT.format(item.getCurrentPrice())));
                }
            }
        });
        
        // Same for button cell
        assetCombo.setButtonCell(new ListCell<StockDataService.StockData>() {
            @Override
            protected void updateItem(StockDataService.StockData item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%s - %s", item.getSymbol(), item.getName()));
                }
            }
        });
    }
    
    /**
     * Updates the preview section with current selection
     */
    private void updatePreview(StockDataService.StockData asset, String quantityText, 
                              Label priceLabel, Label totalLabel) {
        if (asset == null) {
            priceLabel.setText("Price: -");
            totalLabel.setText("Total: -");
            return;
        }
        
        try {
            double quantity = Double.parseDouble(quantityText);
            if (quantity <= 0) quantity = 1;
            
            double price = asset.getCurrentPrice();
            double total = price * quantity;
            
            priceLabel.setText("Price: " + CURRENCY_FORMAT.format(price));
            totalLabel.setText("Total: " + CURRENCY_FORMAT.format(total));
            
            // Visual indicator if user can afford it
            if (total > portfolio.getCashBalance()) {
                totalLabel.setTextFill(Color.RED);
            } else {
                totalLabel.setTextFill(Color.GREEN);
            }
            
        } catch (NumberFormatException e) {
            priceLabel.setText("Price: " + CURRENCY_FORMAT.format(asset.getCurrentPrice()));
            totalLabel.setText("Total: (Enter valid quantity)");
            totalLabel.setTextFill(Color.BLACK);
        }
    }
    
    /**
     * Shows the performance view with detailed information about stock holdings.
     */
    @FXML
    public void showPerformanceView() {
        // Filter to get only STOCK type holdings
        List<Holding> stocks = data.stream()
            .filter(h -> h.getAssetType() == AssetType.STOCK || 
                         h.getAssetType() == AssetType.ETF || 
                         h.getAssetType() == AssetType.FUND ||
                         h.getAssetType() == AssetType.CRYPTO)
            .collect(Collectors.toList());
            
        if (stocks.isEmpty()) {
            showInformationAlert("No Assets", "There are no stock holdings to display performance for.");
            return;
        }
        
        // Create the performance window
        Stage stage = new Stage(StageStyle.DECORATED);
        stage.setTitle("Asset Performance Overview");
        stage.initModality(Modality.APPLICATION_MODAL);
        
        // Create a table to display performance data
        TableView<Holding> performanceTable = new TableView<>();
        performanceTable.setPlaceholder(new Label("No stock holdings available"));
        
        // Define columns
        TableColumn<Holding, String> symbolCol = new TableColumn<>("Symbol");
        symbolCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getSymbol()));
        
        TableColumn<Holding, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getName()));
        
        // Type column
        TableColumn<Holding, AssetType> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getAssetType()));
        
        // Quantity column
        TableColumn<Holding, Double> qtyCol = new TableColumn<>("Quantity");
        qtyCol.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getQuantity()));
        qtyCol.setCellFactory(col -> new TableCell<Holding, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(NUMBER_FORMAT.format(item));
                }
            }
        });
        
        // Purchase Price column
        TableColumn<Holding, String> purchasePriceCol = new TableColumn<>("Purchase Price");
        purchasePriceCol.setCellValueFactory(data -> 
            new ReadOnlyStringWrapper(CURRENCY_FORMAT.format(data.getValue().getPurchasePricePerUnit())));
        
        // Current Price column
        TableColumn<Holding, String> currentPriceCol = new TableColumn<>("Current Price");
        currentPriceCol.setCellValueFactory(data -> 
            new ReadOnlyStringWrapper(CURRENCY_FORMAT.format(data.getValue().getPricePerUnit())));
        
        // Change column (value)
        TableColumn<Holding, String> changeCol = new TableColumn<>("Price Change");
        changeCol.setCellValueFactory(data -> {
            Holding holding = data.getValue();
            double change = holding.getPriceChange();
            String formattedChange = CURRENCY_FORMAT.format(change);
            return new ReadOnlyStringWrapper(formattedChange);
        });
        changeCol.setCellFactory(col -> new TableCell<Holding, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    
                    // Color based on whether it's positive or negative
                    if (item.startsWith("-")) {
                        setStyle("-fx-text-fill: red;");
                    } else if (!item.equals(CURRENCY_FORMAT.format(0))) {
                        setStyle("-fx-text-fill: green;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });
        
        // Change column (percentage)
        TableColumn<Holding, String> changePctCol = new TableColumn<>("Change %");
        changePctCol.setCellValueFactory(data -> {
            Holding holding = data.getValue();
            double changePct = holding.getPriceChangePercentage();
            return new ReadOnlyStringWrapper(String.format("%.2f%%", changePct));
        });
        changePctCol.setCellFactory(col -> new TableCell<Holding, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    
                    // Color based on whether it's positive or negative
                    if (item.startsWith("-")) {
                        setStyle("-fx-text-fill: red;");
                    } else if (!item.equals("0.00%")) {
                        setStyle("-fx-text-fill: green;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });
        
        // Current Value
        TableColumn<Holding, String> valueCol = new TableColumn<>("Current Value");
        valueCol.setCellValueFactory(data -> 
            new ReadOnlyStringWrapper(CURRENCY_FORMAT.format(data.getValue().getCurrentValue())));
        
        // Profit/Loss
        TableColumn<Holding, String> profitLossCol = new TableColumn<>("Profit/Loss");
        profitLossCol.setCellValueFactory(data -> {
            Holding holding = data.getValue();
            double profitLoss = holding.getProfitLoss();
            return new ReadOnlyStringWrapper(CURRENCY_FORMAT.format(profitLoss));
        });
        profitLossCol.setCellFactory(col -> new TableCell<Holding, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    
                    // Color based on whether it's positive or negative
                    if (item.startsWith("-")) {
                        setStyle("-fx-text-fill: red;");
                    } else if (!item.equals(CURRENCY_FORMAT.format(0))) {
                        setStyle("-fx-text-fill: green;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });
        
        // Add all columns to the table
        performanceTable.getColumns().add(symbolCol);
        performanceTable.getColumns().add(nameCol);
        performanceTable.getColumns().add(typeCol);
        performanceTable.getColumns().add(qtyCol);
        performanceTable.getColumns().add(purchasePriceCol);
        performanceTable.getColumns().add(currentPriceCol);
        performanceTable.getColumns().add(changeCol);
        performanceTable.getColumns().add(changePctCol);
        performanceTable.getColumns().add(valueCol);
        performanceTable.getColumns().add(profitLossCol);
        
        // Set data
        performanceTable.setItems(FXCollections.observableArrayList(stocks));
        
        // Create a layout with chart and table
        BorderPane root = new BorderPane();
        
        // Add the performance table
        root.setCenter(performanceTable);
        
        // Add a title and summary
        VBox header = new VBox(10);
        header.setPadding(new Insets(10));
        
        Text title = new Text("Asset Performance Overview");
        title.setFont(Font.font("System", FontWeight.BOLD, 16));
        
        Text summary = new Text(String.format(
            "Total Assets: %s\nTotal Invested: %s\nTotal Profit/Loss: %s (%.2f%%)",
            CURRENCY_FORMAT.format(portfolio.getTotalAssetValue()),
            CURRENCY_FORMAT.format(portfolio.getTotalInvested()),
            CURRENCY_FORMAT.format(portfolio.getProfitLoss()),
            portfolio.getProfitLossPercentage()
        ));
        
        header.getChildren().addAll(title, summary);
        root.setTop(header);
        
        // Create a scene and show the stage
        Scene scene = new Scene(root, 950, 600);
        stage.setScene(scene);
        stage.show();
    }
    
    /**
     * Adds a new holding to the portfolio manually.
     * Opens a dialog to enter holding details.
     */
    @FXML
    public void add() {
        Dialog<Holding> dialog = new HoldingDialog(null);
        Optional<Holding> result = dialog.showAndWait();
        
        result.ifPresent(newHolding -> {
            // Calculate cost
            double cost = newHolding.getQuantity() * newHolding.getPricePerUnit();
            
            // Check if we have enough cash
            if (cost > portfolio.getCashBalance()) {
                showErrorAlert("Insufficient Funds", 
                    "You need " + CURRENCY_FORMAT.format(cost) + " but only have " + 
                    CURRENCY_FORMAT.format(portfolio.getCashBalance()) + " available.",
                    "Please reduce the quantity or purchase a less expensive asset.");
                return;
            }
            
            // Add to portfolio and deduct cash
            portfolio.deductCash(cost);
            data.add(newHolding);
        });
    }

    /**
     * Edits the selected holding.
     * Opens a dialog pre-populated with the selected holding details.
     */
    @FXML
    public void edit() {
        Holding selectedHolding = holdingsTable.getSelectionModel().getSelectedItem();
        if (selectedHolding == null) {
            showInformationAlert("No Selection", "Please select a holding to edit.");
            return;
        }

        Dialog<Holding> dialog = new HoldingDialog(selectedHolding);
        
        Optional<Holding> result = dialog.showAndWait();
        
        result.ifPresent(updatedHolding -> {
            // Apply updates to the existing holding (maintaining its ID)
            selectedHolding.setSymbol(updatedHolding.getSymbol());
            selectedHolding.setName(updatedHolding.getName());
            selectedHolding.setQuantity(updatedHolding.getQuantity());
            selectedHolding.setPricePerUnit(updatedHolding.getPricePerUnit());
            selectedHolding.setAssetType(updatedHolding.getAssetType());
            
            // Refresh the table
            holdingsTable.refresh();
        });
    }

    /**
     * Deletes the selected holding from the portfolio.
     */
    @FXML
    public void delete() {
        Holding selectedHolding = holdingsTable.getSelectionModel().getSelectedItem();
        if (selectedHolding == null) {
            showInformationAlert("No Selection", "Please select a holding to delete.");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Deletion");
        confirmAlert.setHeaderText("Delete Holding");
        confirmAlert.setContentText("Are you sure you want to delete " + selectedHolding.getSymbol() + "?");
        
        Optional<ButtonType> result = confirmAlert.showAndWait();
        
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Add cash back from selling
            double value = selectedHolding.getCurrentValue();
            portfolio.addCash(value);
            
            // Remove from the observable list (will trigger update to portfolio)
            data.remove(selectedHolding);
        }
    }

    /**
     * Zeigt einen Dialog zum Verkauf einer Anlage oder eines Teils davon an.
     */
    @FXML
    public void sellHolding() {
        Holding selectedHolding = holdingsTable.getSelectionModel().getSelectedItem();
        if (selectedHolding == null) {
            showInformationAlert("Keine Auswahl", "Bitte wähle eine Position zum Verkaufen aus.");
            return;
        }
        
        // Dialog für den Verkauf erstellen
        Dialog<Double> dialog = new Dialog<>();
        dialog.setTitle("Position verkaufen");
        dialog.setHeaderText("Verkauf von " + selectedHolding.getSymbol() + " - " + selectedHolding.getName());
        
        // Buttons einrichten
        ButtonType sellButtonType = new ButtonType("Verkaufen", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(sellButtonType, ButtonType.CANCEL);
        
        // Eingabefeld für die zu verkaufende Anzahl
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        // Informationen zur aktuellen Position
        Text infoText = new Text(
            "Aktuelle Anzahl: " + NUMBER_FORMAT.format(selectedHolding.getQuantity()) + "\n" +
            "Aktueller Preis: " + CURRENCY_FORMAT.format(selectedHolding.getPricePerUnit()) + "\n" +
            "Gesamtwert: " + CURRENCY_FORMAT.format(selectedHolding.getCurrentValue())
        );
        
        TextField quantityField = new TextField(String.valueOf(selectedHolding.getQuantity()));
        Label maxValueLabel = new Label("Max: " + NUMBER_FORMAT.format(selectedHolding.getQuantity()));
        
        Button maxButton = new Button("Max");
        maxButton.setOnAction(e -> quantityField.setText(String.valueOf(selectedHolding.getQuantity())));
        
        Label valueLabel = new Label("Verkaufswert: " + CURRENCY_FORMAT.format(selectedHolding.getCurrentValue()));
        
        // Aktualisiere den Verkaufswert, wenn sich die Anzahl ändert
        quantityField.textProperty().addListener((obs, oldVal, newVal) -> {
            try {
                double qty = Double.parseDouble(newVal);
                if (qty > selectedHolding.getQuantity()) {
                    qty = selectedHolding.getQuantity();
                    quantityField.setText(String.valueOf(qty));
                }
                double value = qty * selectedHolding.getPricePerUnit();
                valueLabel.setText("Verkaufswert: " + CURRENCY_FORMAT.format(value));
            } catch (NumberFormatException e) {
                valueLabel.setText("Verkaufswert: ?");
            }
        });
        
        // Grid-Layout
        grid.add(infoText, 0, 0, 2, 1);
        grid.add(new Label("Zu verkaufende Anzahl:"), 0, 1);
        grid.add(quantityField, 1, 1);
        grid.add(maxValueLabel, 0, 2);
        grid.add(maxButton, 1, 2);
        grid.add(valueLabel, 0, 3, 2, 1);
        
        dialog.getDialogPane().setContent(grid);
        
        // Setze den Fokus auf das Anzahl-Feld
        dialog.setOnShown(e -> quantityField.requestFocus());
        
        // Wandle das Ergebnis in die zu verkaufende Anzahl um
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == sellButtonType) {
                try {
                    double quantity = Double.parseDouble(quantityField.getText());
                    if (quantity <= 0 || quantity > selectedHolding.getQuantity()) {
                        throw new NumberFormatException("Ungültige Anzahl");
                    }
                    return quantity;
                } catch (NumberFormatException ex) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Ungültige Eingabe");
                    alert.setHeaderText("Bitte gib eine gültige Anzahl ein");
                    alert.setContentText("Die Anzahl muss positiv sein und darf die vorhandene Anzahl nicht überschreiten.");
                    alert.showAndWait();
                    return null;
                }
            }
            return null;
        });
        
        // Dialog anzeigen und Ergebnis verarbeiten
        Optional<Double> result = dialog.showAndWait();
        
        result.ifPresent(sellQuantity -> {
            // Berechne den Verkaufswert
            double saleValue = sellQuantity * selectedHolding.getPricePerUnit();
            
            // Wenn komplette Position verkauft wird
            if (sellQuantity >= selectedHolding.getQuantity()) {
                data.remove(selectedHolding);
                portfolio.addCash(saleValue);
                showInformationAlert("Verkauf abgeschlossen", 
                    "Position " + selectedHolding.getSymbol() + " wurde vollständig verkauft für " + 
                    CURRENCY_FORMAT.format(saleValue) + ".");
            } else {
                // Teilverkauf - reduziere die Anzahl
                double remainingQuantity = selectedHolding.getQuantity() - sellQuantity;
                selectedHolding.setQuantity(remainingQuantity);
                portfolio.addCash(saleValue);
                showInformationAlert("Verkauf abgeschlossen", 
                    sellQuantity + " Einheiten von " + selectedHolding.getSymbol() + 
                    " wurden verkauft für " + CURRENCY_FORMAT.format(saleValue) + ".");
            }
            
            // UI aktualisieren
            holdingsTable.refresh();
            updateDashboard();
            repo.save(portfolio);
        });
    }
    
    /**
     * Theme toggle functionality (removed)
     */
    @FXML
    private void toggleTheme() {
        // Theme functionality removed
    }

    /**
     * Zeigt einen Fehler-Dialog an.
     */
    private void showErrorAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * Shows an informational alert with the given title and message.
     */
    private void showInformationAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null); // No header
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Inner class to hold stock selection result from dialog
     */
    private static class StockSelectionResult {
        final StockDataService.StockData stock;
        final double quantity;
        
        StockSelectionResult(StockDataService.StockData stock, double quantity) {
            this.stock = stock;
            this.quantity = quantity;
        }
    }
}

package com.investtrack.view;

import com.investtrack.model.AssetType;
import com.investtrack.model.Holding;
import com.investtrack.model.Portfolio;
import com.investtrack.model.PortfolioSnapshot;
import com.investtrack.persistence.PortfolioRepository;
import com.investtrack.service.StockDataService;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
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
import javafx.util.Duration;
import javafx.util.Callback;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

/**
 * Controller for the main window (MainView.fxml).
 */
public class MainController {

    // --- FXML Injected Fields ---
    @FXML private TableView<Holding> holdingsTable;
    @FXML private TableColumn<Holding, String> colSymbol;
    @FXML private TableColumn<Holding, String> colName;
    @FXML private TableColumn<Holding, Double> colQty;
    @FXML private TableColumn<Holding, Double> colBuyPrice;  // Purchase price
    @FXML private TableColumn<Holding, Double> colPrice;     // Current price
    @FXML private TableColumn<Holding, Double> colPriceChange; // Price difference
    @FXML private TableColumn<Holding, Double> colPriceChangePct; // Price change percentage
    @FXML private TableColumn<Holding, String> colValue;
    @FXML private TableColumn<Holding, Double> colProfitLoss; // Profit/loss for the holding
    @FXML private TableColumn<Holding, AssetType> colType;
    @FXML private TableColumn<Holding, Holding> colChart;    // Mini chart for each holding
    
    // Dashboard labels
    @FXML private Label lblTotal;            // Total holdings value
    @FXML private Label lblCashBalance;      // Available cash
    @FXML private Label lblTotalAssets;      // Total assets (holdings + cash)
    @FXML private Label lblProfitLoss;       // Profit/loss in CHF
    @FXML private Label lblProfitLossPercent; // Profit/loss percentage
    @FXML private Label lblTotalInvested;    // Total invested amount
    @FXML private Label lblStatus;           // Status message
    @FXML private Label lblLastUpdate;       // Last update timestamp
    
    // Mini performance chart
    @FXML private LineChart<String, Number> miniChart;
    
    // Buttons
    @FXML private Button btnEdit;
    @FXML private Button btnDelete;
    @FXML private Button btnSell;

    // --- Data Model & Persistence ---
    private PortfolioRepository repo;
    private Portfolio portfolio;
    private ObservableList<Holding> data;
    
    // --- Last update time tracking ---
    private LocalDateTime lastUpdateTime;

    // --- Stock Data Service for price simulation ---
    private final StockDataService stockDataService = StockDataService.getInstance();
    
    // --- Formatting ---
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.getDefault());
    private static final NumberFormat PERCENT_FORMAT = NumberFormat.getPercentInstance(Locale.getDefault());
    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getNumberInstance(Locale.getDefault());
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    /**
     * Initialize the controller.
     * This method is automatically called after the FXML file has been loaded.
     */
    @FXML
    private void initialize() {
        // Customize Number Formats
        PERCENT_FORMAT.setMinimumFractionDigits(2);
        NUMBER_FORMAT.setMinimumFractionDigits(2);
        NUMBER_FORMAT.setMaximumFractionDigits(6);
        
        // 1. Initialize the repository and load data
        repo = new PortfolioRepository(Paths.get(System.getProperty("user.home"), "investtrack.json"));
        portfolio = repo.load();
        data = FXCollections.observableArrayList(portfolio.getHoldings());
        
        // 2. Configure the Table Columns
        configureTableColumns();
        
        // 3. Set up sorting
        SortedList<Holding> sortedData = new SortedList<>(data);
        sortedData.comparatorProperty().bind(holdingsTable.comparatorProperty());
        holdingsTable.setItems(sortedData);
        
        // 4. Set up selection mode
        holdingsTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        
        // 5. Initial UI Update
        updateDashboard();
        updateEditDeleteButtonState();
        
        // Initialize mini chart
        initializeMiniChart();

        // 6. Add Listener for Selection Changes
        holdingsTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> updateEditDeleteButtonState()
        );

        // 7. Add Listener for Data Changes
        data.addListener((javafx.collections.ListChangeListener.Change<? extends Holding> c) -> {
            boolean changed = false;
            while (c.next()) {
                if (c.wasAdded() || c.wasRemoved() || c.wasUpdated()) {
                    changed = true;
                    // Handle additions
                    if(c.wasAdded()){
                        c.getAddedSubList().forEach(h -> {
                            if(portfolio.findHoldingById(h.getId()).isEmpty()){
                                portfolio.addHolding(h);
                            }
                        });
                    }
                    // Handle removals
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
        
        // 8. Start price simulation for stock holdings
        startStockPriceSimulation();
        
        // Record initial update time
        lastUpdateTime = LocalDateTime.now();
        updateLastUpdateTime();
    }
    
    /**
     * Initialize mini chart in the dashboard header
     */
    private void initializeMiniChart() {
        // Clear any existing data
        miniChart.getData().clear();
        
        // Create series for portfolio value
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Portfolio Value");
        
        // If we have historical data, add it to the chart
        if (!portfolio.getPerformanceHistory().isEmpty()) {
            // Add last 10 snapshots (or fewer if we don't have 10)
            List<PortfolioSnapshot> history = portfolio.getPerformanceHistory();
            int startIndex = Math.max(0, history.size() - 10);
            
            for (int i = startIndex; i < history.size(); i++) {
                PortfolioSnapshot snapshot = history.get(i);
                String timeLabel = snapshot.getTimestamp().format(TIME_FORMATTER);
                series.getData().add(new XYChart.Data<>(timeLabel, snapshot.getTotalAssetValue()));
            }
        } else {
            // Add current point if no history
            series.getData().add(new XYChart.Data<>("Now", portfolio.getTotalAssetValue()));
        }
        
        // Add series to chart
        miniChart.getData().add(series);
        
        // Style the series
        series.getNode().setStyle("-fx-stroke: #4caf50; -fx-stroke-width: 2px;");
    }
    
    /**
     * Configures the cell value factories for each table column
     */
    private void configureTableColumns() {
        // Basic columns
        colSymbol.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getSymbol()));
        colName.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getName()));
        colType.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().getAssetType()));

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
        lblTotalInvested.setText(CURRENCY_FORMAT.format(portfolio.getTotalInvested()));
        
        // Update profit/loss
        double profitLoss = portfolio.getProfitLoss();
        lblProfitLoss.setText(CURRENCY_FORMAT.format(profitLoss));
        
        // Color profit/loss based on value
        if (profitLoss > 0) {
            lblProfitLoss.setTextFill(Color.GREEN);
        } else if (profitLoss < 0) {
            lblProfitLoss.setTextFill(Color.RED);
        } else {
            lblProfitLoss.setTextFill(Color.BLACK);
        }
        
        // Update profit/loss percentage
        double profitLossPercent = portfolio.getProfitLossPercentage() / 100; // Convert to decimal for formatter
        lblProfitLossPercent.setText(PERCENT_FORMAT.format(profitLossPercent));
        
        // Color percentage same as the monetary value
        if (profitLossPercent > 0) {
            lblProfitLossPercent.setTextFill(Color.GREEN);
        } else if (profitLossPercent < 0) {
            lblProfitLossPercent.setTextFill(Color.RED);
        } else {
            lblProfitLossPercent.setTextFill(Color.BLACK);
        }
        
        // Update mini chart
        updateMiniChart();
        
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
        btnEdit.setDisable(!hasSelection);
        btnDelete.setDisable(!hasSelection);
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
     * Shows detailed chart for the selected holding
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
        
        // Create axes
        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Time Points");
        
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Price (" + CURRENCY_FORMAT.getCurrency().getSymbol() + ")");
        
        // Create chart
        LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Price History for " + selectedHolding.getName());
        
        // Create series
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("Price");
        
        // Add data points
        List<Holding.PricePoint> history = selectedHolding.getPriceHistory();
        for (int i = 0; i < history.size(); i++) {
            series.getData().add(new XYChart.Data<>(i, history.get(i).getPrice()));
        }
        
        // Add series to chart
        lineChart.getData().add(series);
        
        // Create layout
        BorderPane root = new BorderPane(lineChart);
        
        // Add information
        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(10);
        infoGrid.setVgap(5);
        infoGrid.setPadding(new Insets(10));
        
        // Add purchase info
        infoGrid.add(new Label("Purchase Price:"), 0, 0);
        infoGrid.add(new Label(CURRENCY_FORMAT.format(selectedHolding.getPurchasePricePerUnit())), 1, 0);
        
        infoGrid.add(new Label("Current Price:"), 0, 1);
        infoGrid.add(new Label(CURRENCY_FORMAT.format(selectedHolding.getPricePerUnit())), 1, 1);
        
        infoGrid.add(new Label("Price Change:"), 0, 2);
        Label priceChangeLabel = new Label(CURRENCY_FORMAT.format(selectedHolding.getPriceChange()) + 
                " (" + PERCENT_FORMAT.format(selectedHolding.getPriceChangePercentage() / 100) + ")");
        if (selectedHolding.getPriceChange() > 0) {
            priceChangeLabel.setTextFill(Color.GREEN);
        } else if (selectedHolding.getPriceChange() < 0) {
            priceChangeLabel.setTextFill(Color.RED);
        }
        infoGrid.add(priceChangeLabel, 1, 2);
        
        infoGrid.add(new Label("Quantity:"), 0, 3);
        infoGrid.add(new Label(NUMBER_FORMAT.format(selectedHolding.getQuantity())), 1, 3);
        
        infoGrid.add(new Label("Total Value:"), 0, 4);
        infoGrid.add(new Label(CURRENCY_FORMAT.format(selectedHolding.getCurrentValue())), 1, 4);
        
        infoGrid.add(new Label("Profit/Loss:"), 0, 5);
        Label profitLossLabel = new Label(CURRENCY_FORMAT.format(selectedHolding.getProfitLoss()));
        if (selectedHolding.getProfitLoss() > 0) {
            profitLossLabel.setTextFill(Color.GREEN);
        } else if (selectedHolding.getProfitLoss() < 0) {
            profitLossLabel.setTextFill(Color.RED);
        }
        infoGrid.add(profitLossLabel, 1, 5);
        
        // Add button to close
        Button closeButton = new Button("Close");
        closeButton.setOnAction(e -> stage.close());
        
        HBox buttonBox = new HBox(closeButton);
        buttonBox.setPadding(new Insets(10));
        buttonBox.setAlignment(javafx.geometry.Pos.CENTER);
        
        // Combine components
        VBox bottomBox = new VBox(infoGrid, buttonBox);
        root.setBottom(bottomBox);
        
        // Show the chart
        Scene scene = new Scene(root, 800, 600);
        stage.setScene(scene);
        stage.show();
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
     * Adds predefined stocks from the StockDataService
     */
    @FXML
    public void addPredefinedStocksAction() {
        // Create dialog to select from predefined stocks
        Dialog<StockSelectionResult> dialog = new Dialog<>();
        dialog.setTitle("Add Predefined Asset");
        dialog.setHeaderText("Select an asset and specify quantity");
        
        // Set the button types
        ButtonType addButtonType = new ButtonType("Add to Portfolio", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);
        
        // Create ComboBox for stock selection
        ComboBox<StockDataService.StockData> stockComboBox = new ComboBox<>();
        stockComboBox.setItems(FXCollections.observableArrayList(stockDataService.getPredefinedStocks()));
        stockComboBox.setCellFactory(param -> new ListCell<StockDataService.StockData>() {
            @Override
            protected void updateItem(StockDataService.StockData item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%s - %s (%s) [%s]", 
                        item.getSymbol(), 
                        item.getName(),
                        CURRENCY_FORMAT.format(item.getCurrentPrice()),
                        item.getAssetType()));
                }
            }
        });
        
        // Same display format for the selected item
        stockComboBox.setButtonCell(new ListCell<StockDataService.StockData>() {
            @Override
            protected void updateItem(StockDataService.StockData item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%s - %s (%s) [%s]", 
                        item.getSymbol(), 
                        item.getName(),
                        CURRENCY_FORMAT.format(item.getCurrentPrice()),
                        item.getAssetType()));
                }
            }
        });
        
        // Create quantity field
        TextField quantityField = new TextField("1"); // Default quantity
        
        // Layout
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        grid.add(new Label("Select Asset:"), 0, 0);
        grid.add(stockComboBox, 1, 0);
        grid.add(new Label("Quantity:"), 0, 1);
        grid.add(quantityField, 1, 1);
        
        dialog.getDialogPane().setContent(grid);
        
        // Enable/Disable Add button depending on whether a stock was selected
        Button addButton = (Button) dialog.getDialogPane().lookupButton(addButtonType);
        addButton.setDisable(true);
        
        stockComboBox.getSelectionModel().selectedItemProperty().addListener(
            (observable, oldValue, newValue) -> addButton.setDisable(newValue == null));
        
        // Convert the result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                StockDataService.StockData selectedStock = stockComboBox.getValue();
                if (selectedStock != null) {
                    try {
                        double quantity = Double.parseDouble(quantityField.getText().trim());
                        if (quantity <= 0) {
                            throw new NumberFormatException("Quantity must be positive");
                        }
                        return new StockSelectionResult(selectedStock, quantity);
                    } catch (NumberFormatException e) {
                        // Show error
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Invalid Quantity");
                        alert.setHeaderText("Please enter a valid positive number for quantity");
                        alert.showAndWait();
                        return null;
                    }
                }
            }
            return null;
        });
        
        // Show the dialog and process result
        Optional<StockSelectionResult> result = dialog.showAndWait();
        
        result.ifPresent(selection -> {
            // Calculate total cost
            double totalCost = selection.stock.getCurrentPrice() * selection.quantity;
            
            // Check if enough cash is available
            if (totalCost > portfolio.getCashBalance()) {
                showErrorAlert("Nicht genügend Guthaben", 
                    "Der Gesamtbetrag beträgt " + CURRENCY_FORMAT.format(totalCost) + 
                    ", aber du hast nur " + CURRENCY_FORMAT.format(portfolio.getCashBalance()) + " verfügbar.",
                    "Bitte reduziere die Anzahl oder wähle ein günstigeres Asset.");
                return;
            }
            
            // Create a new holding from the selected stock
            Holding newHolding = stockDataService.createHoldingFromStock(
                selection.stock.getSymbol(), 
                selection.quantity
            );
            
            if (newHolding != null) {
                // Deduct cash and add holding to portfolio
                portfolio.deductCash(totalCost);
                data.add(newHolding);
                updateDashboard();
            }
        });
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
        performanceTable.getColumns().addAll(
            symbolCol, nameCol, typeCol, qtyCol, 
            purchasePriceCol, currentPriceCol, 
            changeCol, changePctCol, 
            valueCol, profitLossCol);
        
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
        Dialog<Holding> dialog = new HoldingDialog();
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

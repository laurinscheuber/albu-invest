package com.investtrack.view;

import com.investtrack.model.AssetType;
import com.investtrack.model.Holding;
import com.investtrack.model.Portfolio;
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
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Controller for the main window (MainView.fxml).
 */
public class MainController {

    // --- FXML Injected Fields ---
    @FXML private TableView<Holding> holdingsTable;
    @FXML private TableColumn<Holding, String> colSymbol;
    @FXML private TableColumn<Holding, String> colName;
    @FXML private TableColumn<Holding, Double> colQty;
    @FXML private TableColumn<Holding, Double> colPrice;
    @FXML private TableColumn<Holding, String> colValue;
    @FXML private TableColumn<Holding, AssetType> colType;
    @FXML private Label lblTotal;
    @FXML private Label lblCashBalance; // Label für das aktuelle Guthaben
    @FXML private Button btnEdit;
    @FXML private Button btnDelete;
    @FXML private Button btnSell; // Button für Teil-Verkauf von Anlagen

    // --- Data Model & Persistence ---
    private PortfolioRepository repo;
    private Portfolio portfolio;
    private ObservableList<Holding> data;

    // --- Stock Data Service for price simulation ---
    private final StockDataService stockDataService = StockDataService.getInstance();
    
    // --- Formatting ---
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.getDefault());
    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getNumberInstance(Locale.getDefault());
    private static final NumberFormat PERCENT_FORMAT = NumberFormat.getPercentInstance(Locale.getDefault());
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    static {
        NUMBER_FORMAT.setMaximumFractionDigits(4);
        PERCENT_FORMAT.setMaximumFractionDigits(2);
    }

    /**
     * Initializes controller class after FXML is loaded
     */
    @FXML
    private void initialize() {
        // 1. Setup Repository and Load Data
        Path dataPath = Paths.get(System.getProperty("user.home"), ".investtrack", "portfolio.json");
        repo = new PortfolioRepository(dataPath);
        portfolio = repo.load();

        // 2. Wrap holdings in observable list
        data = FXCollections.observableArrayList(portfolio.getHoldings());

        // 3. Configure Table Columns
        configureTableColumns();

        // 4. Bind Data to Table (using SortedList for sorting)
        SortedList<Holding> sortedData = new SortedList<>(data);
        sortedData.comparatorProperty().bind(holdingsTable.comparatorProperty());
        holdingsTable.setItems(sortedData);

        // 5. Initial UI Update
        updateTotal();
        updateEditDeleteButtonState();

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
                updateTotal();
                holdingsTable.refresh();
            }
        });
        
        // 8. Start price simulation for stock holdings
        startStockPriceSimulation();
    }
    
    /**
     * Configures the cell value factories for each table column
     */
    private void configureTableColumns() {
        colSymbol.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getSymbol()));
        colName.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getName()));
        colType.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().getAssetType()));

        // Quantity column with custom formatting
        colQty.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().getQuantity()));
        colQty.setCellFactory(column -> new TableCell<Holding, Double>() {
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

        // Price column with currency formatting
        colPrice.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().getPricePerUnit()));
        colPrice.setCellFactory(column -> new TableCell<Holding, Double>() {
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

        // Value column with calculated current value
        colValue.setCellValueFactory(cellData -> {
             Holding holding = cellData.getValue();
             return new ReadOnlyStringWrapper(CURRENCY_FORMAT.format(holding.getCurrentValue()));
        });
    }

    /* --- CRUD Actions -------------------------------------------- */

    /** Add button handler */
    @FXML public void add() { 
        openDialog(null); 
    }
    
    /** Edit button handler */
    @FXML public void edit() {
        Holding sel = holdingsTable.getSelectionModel().getSelectedItem();
        if (sel != null) openDialog(sel);
        else showInformationAlert("No Selection", "Please select a holding to edit.");
    }
    
    /** Delete button handler */
    @FXML public void delete() {
        Holding sel = holdingsTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showInformationAlert("No Selection", "Please select a holding to delete.");
            return;
        }
        
        // Confirmation Dialog
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Delete");
        alert.setHeaderText("Delete " + sel.getSymbol() + " - " + sel.getName() + "?");
        alert.setContentText("Are you sure you want to delete this holding?");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            data.remove(sel);
            portfolio.removeHolding(sel);
            repo.save(portfolio);
            updateTotal();
        }
    }

    /* --- Chart Action -------------------------------------------- */

    /** Chart button handler */
    @FXML public void showChart() {
        if (data.isEmpty()) {
            showInformationAlert("No Data", "There are no holdings to display in the chart.");
            return;
        }

        PieChart chart = new PieChart();
        chart.setTitle("Asset Allocation");

        // Group by asset type
        data.stream()
            .collect(Collectors.groupingBy(
                Holding::getAssetType,
                Collectors.summingDouble(Holding::getCurrentValue)
            ))
            .forEach((assetType, totalValue) -> {
                if (totalValue > 0) {
                    String label = String.format("%s (%s)",
                                                assetType.toString(),
                                                CURRENCY_FORMAT.format(totalValue));
                    PieChart.Data slice = new PieChart.Data(label, totalValue);
                    chart.getData().add(slice);
                }
            });

        // Sort slices by value (descending)
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
                    if (holding.getAssetType() == AssetType.STOCK) {
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
                updateTotal();
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
        dialog.setTitle("Add Predefined Stock");
        dialog.setHeaderText("Select a stock and specify quantity");
        
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
                    setText(String.format("%s - %s (%s)", 
                        item.getSymbol(), 
                        item.getName(),
                        CURRENCY_FORMAT.format(item.getCurrentPrice())));
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
                    setText(String.format("%s - %s (%s)", 
                        item.getSymbol(), 
                        item.getName(),
                        CURRENCY_FORMAT.format(item.getCurrentPrice())));
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
        grid.add(new Label("Select Stock:"), 0, 0);
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
                updateTotal();
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
            .filter(h -> h.getAssetType() == AssetType.STOCK)
            .collect(Collectors.toList());
            
        if (stocks.isEmpty()) {
            showInformationAlert("No Stocks", "There are no stock holdings to display performance for.");
            return;
        }
        
        // Create the performance window
        Stage stage = new Stage(StageStyle.DECORATED);
        stage.setTitle("Stock Performance Overview");
        stage.initModality(Modality.APPLICATION_MODAL);
        
        // Create a table to display performance data
        TableView<Holding> performanceTable = new TableView<>();
        performanceTable.setPlaceholder(new Label("No stock holdings available"));
        
        // Define columns
        TableColumn<Holding, String> symbolCol = new TableColumn<>("Symbol");
        symbolCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getSymbol()));
        
        TableColumn<Holding, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getName()));
        
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
        
        // Purchase Price column (from StockDataService history)
        TableColumn<Holding, String> purchasePriceCol = new TableColumn<>("Purchase Price");
        purchasePriceCol.setCellValueFactory(data -> {
            Holding holding = data.getValue();
            StockDataService.PerformanceData perfData = 
                stockDataService.getPerformanceData(holding.getSymbol());
                
            if (perfData != null) {
                return new ReadOnlyStringWrapper(CURRENCY_FORMAT.format(perfData.getPurchasePrice()));
            } else {
                return new ReadOnlyStringWrapper(CURRENCY_FORMAT.format(holding.getPricePerUnit()));
            }
        });
        
        // Current Price column
        TableColumn<Holding, String> currentPriceCol = new TableColumn<>("Current Price");
        currentPriceCol.setCellValueFactory(data -> 
            new ReadOnlyStringWrapper(CURRENCY_FORMAT.format(data.getValue().getPricePerUnit())));
        
        // Change column (value)
        TableColumn<Holding, String> changeCol = new TableColumn<>("Change");
        changeCol.setCellValueFactory(data -> {
            Holding holding = data.getValue();
            StockDataService.PerformanceData perfData = 
                stockDataService.getPerformanceData(holding.getSymbol());
                
            if (perfData != null) {
                String formattedChange = CURRENCY_FORMAT.format(perfData.getPriceChange());
                return new ReadOnlyStringWrapper(formattedChange);
            } else {
                return new ReadOnlyStringWrapper("N/A");
            }
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
                    if (item.startsWith("-")) {
                        setStyle("-fx-text-fill: red;");
                    } else if (!item.equals("N/A")) {
                        setStyle("-fx-text-fill: green;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });
        
        // Percent Change column
        TableColumn<Holding, String> percentChangeCol = new TableColumn<>("% Change");
        percentChangeCol.setCellValueFactory(data -> {
            Holding holding = data.getValue();
            StockDataService.PerformanceData perfData = 
                stockDataService.getPerformanceData(holding.getSymbol());
                
            if (perfData != null) {
                double pctChange = perfData.getPercentChange();
                String formatted = String.format("%.2f%%", pctChange);
                return new ReadOnlyStringWrapper(formatted);
            } else {
                return new ReadOnlyStringWrapper("N/A");
            }
        });
        percentChangeCol.setCellFactory(col -> new TableCell<Holding, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.startsWith("-")) {
                        setStyle("-fx-text-fill: red;");
                    } else if (!item.equals("N/A")) {
                        setStyle("-fx-text-fill: green;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });
        
        // Total Value column
        TableColumn<Holding, String> totalValueCol = new TableColumn<>("Total Value");
        totalValueCol.setCellValueFactory(data -> {
            double value = data.getValue().getCurrentValue();
            return new ReadOnlyStringWrapper(CURRENCY_FORMAT.format(value));
        });
        
        // Add columns to table
        performanceTable.getColumns().addAll(
            symbolCol, nameCol, qtyCol, purchasePriceCol, currentPriceCol, 
            changeCol, percentChangeCol, totalValueCol
        );
        
        // Add data to table
        performanceTable.setItems(FXCollections.observableArrayList(stocks));
        
        // Make columns resize with table
        performanceTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        // Create refresh button for manual updates
        Button refreshButton = new Button("Refresh Data");
        refreshButton.setOnAction(e -> performanceTable.refresh());
        
        // Auto-refresh every 5 seconds
        Timeline timeline = new Timeline(
            new KeyFrame(Duration.seconds(5), event -> performanceTable.refresh())
        );
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
        
        // Create layout
        VBox layout = new VBox(10);
        layout.setPadding(new Insets(10));
        
        // Add an info text at the top
        Text infoText = new Text("Performance since purchase price. Values update every 5 seconds.");
        infoText.setFont(Font.font("System", FontWeight.NORMAL, 12));
        
        layout.getChildren().addAll(infoText, performanceTable, refreshButton);
        
        // Set scene and show stage
        Scene scene = new Scene(layout, 800, 500);
        stage.setScene(scene);
        
        // Clean up when stage closes
        stage.setOnHidden(e -> timeline.stop());
        
        stage.show();
    }

    /* --- Helper Methods ------------------------------------------ */

    private void openDialog(Holding h) {
        HoldingDialog dlg = new HoldingDialog(h);
        Optional<Holding> res = dlg.showAndWait();
        res.ifPresent(holding -> {
            if (h == null) { // Adding new
                data.add(holding);
            } else { // Updating existing
                // Find index and update
                int idx = data.indexOf(h);
                if (idx >= 0) {
                    data.set(idx, holding);
                }
            }
            repo.save(portfolio);
            holdingsTable.refresh();
            updateTotal();
        });
    }

    private void updateTotal() {
        lblTotal.setText("Total: " + 
            CURRENCY_FORMAT.format(portfolio.getTotalValue()));
        lblCashBalance.setText("Guthaben: " + 
            CURRENCY_FORMAT.format(portfolio.getCashBalance()));
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
            updateTotal();
            showInformationAlert("Portfolio zurückgesetzt", 
                "Das Portfolio wurde erfolgreich zurückgesetzt. Guthaben: " + 
                CURRENCY_FORMAT.format(portfolio.getCashBalance()));
        }
    }

    /**
     * Shows a standard information alert dialog.
     */
    private void showInformationAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    /**
     * Helper class to store stock selection results
     */
    private static class StockSelectionResult {
        final StockDataService.StockData stock;
        final double quantity;
        
        StockSelectionResult(StockDataService.StockData stock, double quantity) {
            this.stock = stock;
            this.quantity = quantity;
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
            updateTotal();
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
}

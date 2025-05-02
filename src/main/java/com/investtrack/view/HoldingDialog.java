package com.investtrack.view;

import com.investtrack.model.AssetType;
import com.investtrack.model.Holding;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.util.StringConverter;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.Optional;

/**
 * A custom {@link Dialog} for creating or editing a {@link Holding}.
 * Provides input fields for symbol, name, quantity, price, and asset type.
 * Includes basic input validation.
 */
public class HoldingDialog extends Dialog<Holding> {

    // --- UI Elements ---
    private final TextField symbolTextField = new TextField();
    private final TextField nameTextField = new TextField();
    private final TextField quantityTextField = new TextField();
    private final TextField priceTextField = new TextField();
    private final ComboBox<AssetType> assetTypeComboBox = new ComboBox<>();

    // --- Data ---
    private final Holding holdingToEdit; // Null if creating a new holding

    // --- Formatting ---
    // Use locale-specific number format for parsing input
    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getNumberInstance(Locale.getDefault());


    /**
     * Constructs a new HoldingDialog.
     *
     * @param existingHolding The {@link Holding} to edit, or {@code null} to create a new one.
     */
    public HoldingDialog(Holding existingHolding) {
        this.holdingToEdit = existingHolding;

        setTitle(existingHolding == null ? "Add New Holding" : "Edit Holding");
        setHeaderText(existingHolding == null ? "Enter details for the new holding." : "Update the details for the holding.");

        // --- Setup Buttons ---
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Button okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
        okButton.setText(existingHolding == null ? "Add" : "Update");
        // Add validation listener to OK button (disable if invalid)
         okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (!validateInput()) {
                event.consume(); // Prevent dialog closing if validation fails
            }
        });


        // --- Setup ComboBox ---
        assetTypeComboBox.setItems(FXCollections.observableArrayList(AssetType.values()));
        assetTypeComboBox.setConverter(new StringConverter<AssetType>() {
            @Override public String toString(AssetType type) { return type == null ? "" : type.toString(); }
            @Override public AssetType fromString(String string) { return AssetType.valueOf(string); }
        });
        // Select OTHER as default if adding new
        if (existingHolding == null) {
            assetTypeComboBox.setValue(AssetType.OTHER);
        }

        // --- Setup Input Fields & Prompts ---
        symbolTextField.setPromptText("e.g., AAPL, BTC");
        nameTextField.setPromptText("e.g., Apple Inc., Bitcoin");
        quantityTextField.setPromptText("e.g., 100.5"); // Use locale-specific decimal separator
        priceTextField.setPromptText("e.g., 175.50"); // Use locale-specific decimal/currency separator

        // --- Layout ---
        GridPane grid = createLayout();
        getDialogPane().setContent(grid);

        // --- Populate Fields if Editing ---
        if (existingHolding != null) {
            populateFields(existingHolding);
        }

        // --- Set Result Converter ---
        // This runs *after* validation when OK is clicked
        setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return buildResult();
            }
            return null; // Return null if Cancel is pressed or validation failed earlier
        });

        // Request focus on the first input field
        Platform.runLater(symbolTextField::requestFocus);
    }

    /** Creates the GridPane layout for the dialog content. */
    private GridPane createLayout() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10)); // top, right, bottom, left

        grid.add(new Label("Symbol:"), 0, 0);
        grid.add(symbolTextField, 1, 0);
        grid.add(new Label("Name:"), 0, 1);
        grid.add(nameTextField, 1, 1);
        grid.add(new Label("Quantity:"), 0, 2);
        grid.add(quantityTextField, 1, 2);
        grid.add(new Label("Price/Unit:"), 0, 3);
        grid.add(priceTextField, 1, 3);
        grid.add(new Label("Asset Type:"), 0, 4);
        grid.add(assetTypeComboBox, 1, 4);

        // Make text fields expand
        GridPane.setHgrow(symbolTextField, javafx.scene.layout.Priority.ALWAYS);
        GridPane.setHgrow(nameTextField, javafx.scene.layout.Priority.ALWAYS);
        GridPane.setHgrow(quantityTextField, javafx.scene.layout.Priority.ALWAYS);
        GridPane.setHgrow(priceTextField, javafx.scene.layout.Priority.ALWAYS);
        GridPane.setHgrow(assetTypeComboBox, javafx.scene.layout.Priority.ALWAYS);


        return grid;
    }

    /** Populates the input fields with data from an existing Holding. */
    private void populateFields(Holding holding) {
        symbolTextField.setText(holding.getSymbol());
        nameTextField.setText(holding.getName());
        // Format numbers back to strings for text fields
        quantityTextField.setText(NUMBER_FORMAT.format(holding.getQuantity()));
        priceTextField.setText(NUMBER_FORMAT.format(holding.getPricePerUnit())); // Use number format, not currency
        assetTypeComboBox.setValue(holding.getAssetType());
    }

    /**
     * Validates the user input in the dialog fields.
     * Shows an error alert if validation fails.
     * @return {@code true} if all inputs are valid, {@code false} otherwise.
     */
    private boolean validateInput() {
        StringBuilder errors = new StringBuilder();

        if (symbolTextField.getText() == null || symbolTextField.getText().trim().isEmpty()) {
            errors.append("- Symbol cannot be empty.\\n"); // Correct newline escape
        }
        if (nameTextField.getText() == null || nameTextField.getText().trim().isEmpty()) {
            errors.append("- Name cannot be empty.\\n"); // Correct newline escape
        }
        if (assetTypeComboBox.getValue() == null) {
            errors.append("- Asset Type must be selected.\\n"); // Correct newline escape
        }

        // Validate Quantity
        Optional<Double> quantityOpt = parseDouble(quantityTextField.getText());
         if (quantityOpt.isEmpty()) {
             errors.append("- Quantity must be a valid number (e.g., ").append(NUMBER_FORMAT.format(10.5)).append(").\\n"); // Correct newline escape
         } else if (quantityOpt.get() <= 0) {
             errors.append("- Quantity must be positive.\\n"); // Correct newline escape
         }


        // Validate Price
        Optional<Double> priceOpt = parseDouble(priceTextField.getText());
        if (priceOpt.isEmpty()) {
            errors.append("- Price/Unit must be a valid number (e.g., ").append(NUMBER_FORMAT.format(100.99)).append(").\\n"); // Correct newline escape
        } else if (priceOpt.get() < 0) {
             errors.append("- Price/Unit cannot be negative.\\n"); // Correct newline escape
         }


        if (errors.length() > 0) {
            showErrorAlert("Invalid Input", "Please correct the following errors:", errors.toString());
            return false;
        }

        return true; // All valid
    }

     /**
     * Safely parses a string into a Double using the locale-specific number format.
     * @param text The string to parse.
     * @return An Optional containing the Double if parsing is successful, otherwise an empty Optional.
     */
    private Optional<Double> parseDouble(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Optional.empty();
        }
        try {
            Number number = NUMBER_FORMAT.parse(text.trim());
            return Optional.of(number.doubleValue());
        } catch (ParseException e) {
            // Try parsing with a dot as decimal separator as a fallback
             try {
                 return Optional.of(Double.parseDouble(text.trim().replace(',', '.')));
             } catch (NumberFormatException ex) {
                 return Optional.empty(); // Parsing failed with both formats
             }
        }
    }


    /**
     * Builds the {@link Holding} object from the validated input fields.
     * If editing, it updates the existing holding object; otherwise, creates a new one.
     * Assumes input has already been validated by {@link #validateInput()}.
     *
     * @return The created or updated {@link Holding} object.
     */
    private Holding buildResult() {
        // These parse calls are safe because validateInput() succeeded
        double quantity = parseDouble(quantityTextField.getText()).orElseThrow();
        double price = parseDouble(priceTextField.getText()).orElseThrow();
        String symbol = symbolTextField.getText().trim();
        String name = nameTextField.getText().trim();
        AssetType type = assetTypeComboBox.getValue();

        Holding resultHolding;
        if (holdingToEdit == null) {
            // Create new Holding
            resultHolding = new Holding(symbol, name, quantity, price, type);
        } else {
            // Update existing Holding object directly
            holdingToEdit.setSymbol(symbol);
            holdingToEdit.setName(name);
            holdingToEdit.setQuantity(quantity);
            holdingToEdit.setPricePerUnit(price);
            holdingToEdit.setAssetType(type);
            resultHolding = holdingToEdit; // Return the modified existing object
        }
        return resultHolding;
    }

    /**
     * Shows a standard error alert dialog.
     * @param title Title of the dialog window.
     * @param header Header text (brief summary of error).
     * @param content Detailed error message or context.
     */
    private void showErrorAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        // Ensure the dialog stays on top
        alert.initOwner(getDialogPane().getScene().getWindow());
        alert.initModality(Modality.APPLICATION_MODAL);
        alert.showAndWait();
    }

    // --- Need javafx.application.Platform for runLater ---
    // (Should be available if JavaFX runtime is correctly set up)
    private static class Platform {
        public static void runLater(Runnable runnable) {
            javafx.application.Platform.runLater(runnable);
        }
    }
}

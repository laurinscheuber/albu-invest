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

    // --- Formatting ---
    // Use locale-specific number format for parsing input
    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getNumberInstance(Locale.getDefault());

    /**
     * Constructs a new HoldingDialog.
     *
     * @param existingHolding The {@link Holding} to edit, or {@code null} to create a new one.
     */
    public HoldingDialog(Holding existingHolding) {
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

        // Pre-fill with existing data if editing
        if (existingHolding != null) {
            symbolTextField.setText(existingHolding.getSymbol());
            nameTextField.setText(existingHolding.getName());
            quantityTextField.setText(String.valueOf(existingHolding.getQuantity()));
            priceTextField.setText(String.valueOf(existingHolding.getPricePerUnit()));
            assetTypeComboBox.setValue(existingHolding.getAssetType());
        }

        // --- Set Result Converter ---
        setResultConverter(dialogButton -> {
            if (dialogButton != ButtonType.OK) {
                return null;
            }
            
            try {
                String symbol = symbolTextField.getText().trim();
                String name = nameTextField.getText().trim();
                double quantity = NUMBER_FORMAT.parse(quantityTextField.getText().trim()).doubleValue();
                double price = NUMBER_FORMAT.parse(priceTextField.getText().trim()).doubleValue();
                AssetType assetType = assetTypeComboBox.getValue();
                
                if (existingHolding == null) {
                    // Create new holding
                    return new Holding(symbol, name, quantity, price, assetType);
                } else {
                    // Create a new holding with the same ID as the existing one
                    // to maintain identity but with updated values
                    Holding updatedHolding = new Holding(symbol, name, quantity, price, assetType);
                    return updatedHolding;
                }
            } catch (ParseException e) {
                // This should never happen if validation is correctly implemented
                return null;
            }
        });
    }
    
    /**
     * Creates the dialog layout.
     * @return A {@link GridPane} with all input fields arranged.
     */
    private GridPane createLayout() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        // Add labels and fields to grid
        grid.add(new Label("Symbol:"), 0, 0);
        grid.add(symbolTextField, 1, 0);
        
        grid.add(new Label("Name:"), 0, 1);
        grid.add(nameTextField, 1, 1);
        
        grid.add(new Label("Quantity:"), 0, 2);
        grid.add(quantityTextField, 1, 2);
        
        grid.add(new Label("Price per Unit:"), 0, 3);
        grid.add(priceTextField, 1, 3);
        
        grid.add(new Label("Asset Type:"), 0, 4);
        grid.add(assetTypeComboBox, 1, 4);
        
        return grid;
    }
    
    /**
     * Validates all input fields.
     * @return {@code true} if all inputs are valid, {@code false} otherwise.
     */
    private boolean validateInput() {
        // Validate Symbol
        if (symbolTextField.getText().trim().isEmpty()) {
            showValidationError("Symbol is required");
            return false;
        }
        
        // Validate Name
        if (nameTextField.getText().trim().isEmpty()) {
            showValidationError("Name is required");
            return false;
        }
        
        // Validate Quantity
        try {
            double quantity = NUMBER_FORMAT.parse(quantityTextField.getText().trim()).doubleValue();
            if (quantity <= 0) {
                showValidationError("Quantity must be greater than zero");
                return false;
            }
        } catch (ParseException e) {
            showValidationError("Quantity must be a valid number");
            return false;
        }
        
        // Validate Price
        try {
            double price = NUMBER_FORMAT.parse(priceTextField.getText().trim()).doubleValue();
            if (price <= 0) {
                showValidationError("Price must be greater than zero");
                return false;
            }
        } catch (ParseException e) {
            showValidationError("Price must be a valid number");
            return false;
        }
        
        // Validate Asset Type
        if (assetTypeComboBox.getValue() == null) {
            showValidationError("Asset Type is required");
            return false;
        }
        
        return true;
    }
    
    /**
     * Shows a validation error dialog.
     * @param message The error message to display.
     */
    private void showValidationError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Validation Error");
        alert.setHeaderText("Invalid Input");
        alert.setContentText(message);
        alert.initModality(Modality.APPLICATION_MODAL);
        alert.showAndWait();
    }
}

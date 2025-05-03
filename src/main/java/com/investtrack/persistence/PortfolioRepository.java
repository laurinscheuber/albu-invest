package com.investtrack.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSyntaxException;
import com.investtrack.model.Portfolio;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Handles the persistence (saving and loading) of the {@link Portfolio} object
 * to and from a JSON file. Uses the Gson library for JSON serialization/deserialization.
 */
public class PortfolioRepository {

    /** Gson instance configured for pretty printing JSON output. */
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting() // Makes the JSON file human-readable
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();
            
    /**
     * Custom type adapter for LocalDateTime to handle serialization and deserialization properly.
     */
    private static class LocalDateTimeAdapter implements JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
        private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        
        @Override
        public JsonElement serialize(LocalDateTime src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(FORMATTER.format(src));
        }
        
        @Override
        public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            return LocalDateTime.parse(json.getAsString(), FORMATTER);
        }
    }

    /** The file path where the portfolio data is stored. */
    private final Path portfolioFilePath;

    /**
     * Constructs a PortfolioRepository associated with a specific file path.
     *
     * @param portfolioFilePath The {@link Path} to the JSON file used for storage.
     *                          This path should include the filename (e.g., "portfolio.json").
     */
    public PortfolioRepository(Path portfolioFilePath) {
        if (portfolioFilePath == null) {
            throw new IllegalArgumentException("Portfolio file path cannot be null.");
        }
        this.portfolioFilePath = portfolioFilePath;
    }

    /**
     * Loads the portfolio data from the JSON file specified during construction.
     * <p>
     * If the file does not exist, an empty {@link Portfolio} is returned.
     * If an error occurs during reading or parsing (e.g., corrupted file, invalid JSON),
     * an error message is printed to stderr, and an empty {@link Portfolio} is returned.
     * </p>
     *
     * @return The loaded {@link Portfolio}, or a new empty Portfolio if the file doesn't exist or an error occurs.
     */
    public Portfolio load() {
        if (!Files.exists(portfolioFilePath)) {
            System.out.println("Portfolio file not found at " + portfolioFilePath + ". Creating a new portfolio.");
            return new Portfolio(); // Return a new portfolio if file doesn't exist
        }

        // Use try-with-resources for automatic closing of the reader
        try (BufferedReader reader = Files.newBufferedReader(portfolioFilePath, StandardCharsets.UTF_8)) {
            Portfolio portfolio = GSON.fromJson(reader, Portfolio.class);
            // Gson returns null if the JSON is empty or represents 'null'
            return portfolio != null ? portfolio : new Portfolio();
        } catch (IOException e) {
            System.err.println("Error reading portfolio file: " + portfolioFilePath);
            e.printStackTrace(); // Log the full stack trace for debugging
            return new Portfolio(); // Return empty portfolio on read error
        } catch (JsonSyntaxException e) {
            System.err.println("Error parsing portfolio JSON file: " + portfolioFilePath + ". The file might be corrupted.");
            e.printStackTrace();
            return new Portfolio(); // Return empty portfolio on JSON parse error
        } catch (Exception e) { // Catch unexpected errors
             System.err.println("An unexpected error occurred while loading the portfolio from " + portfolioFilePath);
             e.printStackTrace();
             return new Portfolio();
        }
    }

    /**
     * Saves the provided {@link Portfolio} object to the JSON file.
     * <p>
     * This method will:
     * 1. Ensure the parent directory for the file exists, creating it if necessary.
     * 2. Serialize the {@link Portfolio} object to JSON using Gson.
     * 3. Write the JSON data to the file, overwriting existing content.
     * </p><p>
     * If an error occurs during directory creation or file writing, an error message
     * is printed to stderr.
     * </p>
     *
     * @param portfolio The {@link Portfolio} object to save. If null, the behavior might
     *                  depend on Gson configuration (might write 'null' or throw error).
     *                  It's recommended to pass a non-null Portfolio.
     */
    public void save(Portfolio portfolio) {
        if (portfolio == null) {
             System.err.println("Warning: Attempting to save a null portfolio. Saving an empty portfolio instead.");
             portfolio = new Portfolio(); // Or decide to throw an exception / do nothing
        }

        try {
            // Ensure parent directory exists
            Path parentDir = portfolioFilePath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
                System.out.println("Created directory: " + parentDir);
            }

            // Use try-with-resources for automatic closing of the writer
            try (BufferedWriter writer = Files.newBufferedWriter(portfolioFilePath, StandardCharsets.UTF_8,
                                                                StandardOpenOption.CREATE, // Create file if it doesn't exist
                                                                StandardOpenOption.TRUNCATE_EXISTING)) // Overwrite if exists
            {
                GSON.toJson(portfolio, writer);
            }
            // System.out.println("Portfolio saved successfully to " + portfolioFilePath); // Optional success message

        } catch (IOException e) {
            System.err.println("Error saving portfolio file: " + portfolioFilePath);
            e.printStackTrace(); // Log the full stack trace
        } catch (SecurityException e) {
             System.err.println("Security error: Permission denied while saving portfolio to " + portfolioFilePath);
             e.printStackTrace();
        } catch (Exception e) { // Catch unexpected errors
             System.err.println("An unexpected error occurred while saving the portfolio to " + portfolioFilePath);
             e.printStackTrace();
        }
    }
}

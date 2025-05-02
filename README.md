# Portfolio Tracker

A simple JavaFX application to track investment holdings.

## Features

*   Add, edit, and delete holdings (stocks, ETFs, bonds, etc.).
*   Persists portfolio data to `$HOME/.investtrack/portfolio.json` using JSON.
*   Displays holdings in a sortable table view.
*   Includes basic error handling for invalid inputs.
*   Visualizes asset allocation with a dynamic pie chart.

## Architecture

*   **GUI Toolkit:** Java 17 + OpenJFX 21 (JavaFX)
*   **Architecture:** Model-View-Controller (MVC)
    *   **Model:** Plain Java objects (`Holding`, `Portfolio`, `AssetType`)
    *   **View:** FXML (`MainView.fxml`)
    *   **Controller:** JavaFX Controllers (`MainController`, `HoldingDialog`)
*   **Persistence:** JSON serialization using Gson library.
*   **Build Tool:** Maven 3.9+ with `javafx-maven-plugin`.

## Quick Start

**Prerequisites:**

*   JDK 17 or later
*   Maven 3.9 or later

**Steps:**

1.  **Clone the repository (or navigate to the project folder):**
    ```bash
    # git clone <your-repo-url> # If applicable
    cd /Users/laurin/Development/albu-invest
    ```

2.  **Run the application:**
    ```bash
    mvn clean javafx:run
    ```
    This command compiles the code and starts the JavaFX application. The `javafx-maven-plugin` handles the necessary OpenJFX runtime libraries automatically for your OS (Windows/Mac/Linux).

3.  **(Optional) Package the application:**
    ```bash
    mvn package
    ```
    This creates an executable JAR file in the `target` directory.

## AI Support

This project structure and initial code were generated with the assistance of an AI programming assistant. The AI helped set up the Maven project, create the basic MVC structure, implement core features like CRUD operations and JSON persistence, and generate the initial FXML layout and Javadoc comments.

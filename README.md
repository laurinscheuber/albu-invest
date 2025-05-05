# Portfolio-Tracker

Eine umfassende JavaFX-Anwendung zur Verfolgung und Visualisierung von Investitionsportfolios. Diese Anwendung ermöglicht es Benutzern, ihre Investitionen über verschiedene Anlageklassen hinweg zu verwalten, einschliesslich Aktien, Kryptowährungen, ETFs und Fonds mit Echtzeit-Preissimulationen.

## Über dieses Projekt

Dies ist ein Studentenprojekt, entwickelt von Albulena Aziri. Die Anwendung wurde entwickelt, um Benutzern bei der Verfolgung ihres Investitionsportfolios mit einer übersichtlichen, intuitiven Benutzeroberfläche und leistungsstarken Visualisierungswerkzeugen zu helfen.

## Funktionen

- **Multi-Asset-Unterstützung**: Verfolgen Sie Aktien, Kryptowährungen, ETFs und Fonds an einem Ort
- **Umfassendes Dashboard**: Überblick über Barguthaben, Bestandswert und Gewinn/Verlust auf einen Blick
- **Interaktive Visualisierungen**: Asset-Allokations-Diagramm und Leistungstrends
- **Dynamische Preissimulation**: Echtzeit-Preisschwankungen mit unterschiedlicher Volatilität je nach Anlagetyp
- **Portfolio-Management**: Einfaches Kaufen und Verkaufen von Vermögenswerten mit einer benutzerfreundlichen Oberfläche
- **Persistente Speicherung**: Portfoliodaten werden zwischen Sitzungen in einer lokalen JSON-Datei gespeichert
- **Leistungsanalyse**: Verfolgen Sie Gewinn/Verlust sowohl in absoluten Zahlen als auch in Prozentsätzen

## Architektur

- **GUI-Toolkit:** Java 17 + OpenJFX 21 (JavaFX)
- **Architektur:** Model-View-Controller (MVC)
  - **Model:** Einfache Java-Objekte (`Holding`, `Portfolio`, `AssetType`)
  - **View:** FXML (`MainView.fxml`)
  - **Controller:** JavaFX-Controller (`MainController`, `HoldingDialog`)
- **Persistenz:** JSON-Serialisierung mit der Gson-Bibliothek
- **Build-Tool:** Maven 3.9+ mit `javafx-maven-plugin`

## Ausführen der Anwendung

**Voraussetzungen:**

- JDK 17 oder höher
- Maven 3.9 oder höher

**Schritte:**

1. **Repository klonen oder herunterladen**

2. **Zum Projektverzeichnis navigieren:**

   ```bash
   cd albu-invest
   ```

3. **Anwendung ausführen:**

   ```bash
   mvn clean javafx:run
   ```

4. **Beginnen Sie mit dem Investieren!**
   - Verwenden Sie die Schaltfläche "Buy Assets", um Investitionen zu Ihrem Portfolio hinzuzufügen
   - Verfolgen Sie die Performance über das Dashboard und die Visualisierungen
   - Verkaufen Sie Vermögenswerte bei Bedarf mit der Schaltfläche "Verkaufen"

## Erstellung eines verteilbaren Pakets

Um eine ausführbare JAR-Datei zu erstellen:

```bash
mvn package
```

Die generierte JAR-Datei befindet sich im Verzeichnis `target`.

## Entwicklungshinweis

Dieses Projekt wurde als Studentenprojekt von Albulena Aziri entwickelt und demonstriert Fähigkeiten in JavaFX, Finanzanwendungsentwicklung und interaktiver Datenvisualisierung.

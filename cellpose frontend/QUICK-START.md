# Quick Start Guide - Java Version

## Prerequisites

- ☑️ Java 17 or higher installed
- ☑️ Maven 3.6+ installed
- ☑️ JAVA_HOME environment variable set

## Quick Start

### 1. Build the Project
```bash
cd "c:\Users\user\Documents\Projects\cellpose frontend"
mvn clean compile
```

### 2. Run the Application
```bash
mvn javafx:run
```

### 3. Create Executable JAR
```bash
mvn package
```

The JAR will be in `target/cellpose-frontend-1.0.0.jar`

## Usage

### Opening an Image
1. Click **File** → **Open** in the menu bar
2. Select an image file (PNG, JPG, TIF supported)
3. Image will display in the canvas area

### Segmentation
1. Click **Select Image File** in the Segmentation section
2. Set parameters:
   - **diameter**: Cell diameter in pixels (0 = auto)
   - **channels**: Channel configuration (e.g., "0,0")
   - **flow_threshold**: Flow threshold (0-1, default 0.4)
   - **cellprob_threshold**: Cell probability threshold (default 0)
3. Ensure backend URL is correct (default: http://localhost:8000)
4. Click **compute masks**
5. Wait for segmentation to complete

### Canvas Controls
- **Pan**: Click and drag on the canvas
- **Zoom**: Mouse wheel or use zoom buttons
- **Reset View**: Click the ⟲ button

### Status Information
- **Footer bar** shows:
  - Mouse position (x, y coordinates)
  - Current zoom level
  - Number of detected cells

## Architecture Overview

```
CellposeApp (Main Entry)
    ↓
MainView (BorderPane)
    ├── MenuBarView (Top)
    ├── SidebarView (Left)
    │   ├── SegmentationSection
    │   ├── DisplaySection
    │   └── ViewsSection
    └── CanvasAreaView (Center)
        ├── Header (File path + controls)
        ├── Canvas (Image + cells)
        └── Footer (Status bar)
```

## Key Classes

### Model
- **Cell.java** - Represents a cell with position, size, color
- **ImageData.java** - Wraps image with metadata

### UI Components
- **MainView.java** - Root container
- **MenuBarView.java** - File/Edit/Help menus
- **SidebarView.java** - Left control panel
- **CanvasAreaView.java** - Main visualization area
- **SegmentationSection.java** - Segmentation controls
- **DisplaySection.java** - Display settings
- **ViewsSection.java** - View mode controls

## Backend Integration

The application expects a Cellpose backend server running at `http://localhost:8000`.

### API Endpoint: POST /segment

**Request (multipart/form-data):**
```
file: <image file>
diameter: <integer>
channels: <string>
flow_threshold: <double>
cellprob_threshold: <double>
```

**Response (JSON):**
```json
{
  "masks": [[...]],
  "num_cells": 45
}
```

## Troubleshooting

### Application won't start
```bash
# Check Java version
java -version

# Ensure it's Java 17+
# If not, download from: https://adoptium.net/
```

### Build errors
```bash
# Clean and rebuild
mvn clean install -U

# If still failing, check pom.xml for dependency issues
```

### Canvas not rendering
- Check that JavaFX modules are loaded
- Ensure graphics drivers are up to date
- Try running with: `mvn javafx:run -X` for debug output

### Backend connection failed
- Verify backend server is running on port 8000
- Check firewall settings
- Update backend URL in Segmentation section if using different host

## Development

### Project Structure
```
src/main/
├── java/com/cellpose/
│   ├── CellposeApp.java
│   ├── model/
│   └── ui/
└── resources/
    └── styles.css
```

### Making Changes

1. Edit source files in `src/main/java/`
2. Rebuild: `mvn compile`
3. Run: `mvn javafx:run`

### Adding Dependencies

Edit `pom.xml`:
```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>library</artifactId>
    <version>1.0.0</version>
</dependency>
```

Then run: `mvn clean install`

### Styling

Edit `src/main/resources/styles.css`:
```css
.custom-button {
    -fx-background-color: #4a90e2;
    -fx-text-fill: white;
}
```

Apply in code:
```java
button.getStyleClass().add("custom-button");
```

## Performance Tips

1. **Large Images**: Use ImageView instead of pixel-by-pixel rendering
2. **Memory**: Increase heap size if needed: `mvn javafx:run -Djavafx.run.jvmArgs="-Xmx2g"`
3. **Rendering**: Disable animations during heavy operations

## Common Tasks

### Export JAR with Dependencies
```bash
# Use maven-assembly-plugin (add to pom.xml)
mvn clean package assembly:single
```

### Run without Maven
```bash
java --module-path /path/to/javafx-sdk/lib \
     --add-modules javafx.controls,javafx.fxml \
     -jar target/cellpose-frontend-1.0.0.jar
```

### Create Windows Installer
```bash
jpackage --input target \
         --name Cellpose \
         --main-jar cellpose-frontend-1.0.0.jar \
         --main-class com.cellpose.CellposeApp \
         --type exe
```

## Resources

- **JavaFX Documentation**: https://openjfx.io/
- **Maven Guide**: https://maven.apache.org/guides/
- **Cellpose**: https://cellpose.org/

## Support

For issues with:
- **Java code**: Check README-Java.md and source code comments
- **Conversion questions**: See CONVERSION-GUIDE.md
- **Original Cellpose**: Visit https://github.com/MouseLand/cellpose

---

**Happy coding!** 🚀

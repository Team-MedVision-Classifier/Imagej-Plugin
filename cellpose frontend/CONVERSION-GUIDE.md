# React to Java Conversion Guide

This document details the conversion of the Cellpose frontend from React to JavaFX.

## Overview

The original application was built using React, a JavaScript library for building user interfaces. The Java version uses JavaFX, Oracle's standard GUI toolkit for Java applications.

## State Management Conversion

### React State Hooks → JavaFX Properties

| React Pattern | JavaFX Equivalent |
|--------------|-------------------|
| `useState([])` | `ObservableList<T>` |
| `useState(null)` | `ObjectProperty<T>` |
| `useState(0)` | `IntegerProperty` / `DoubleProperty` |
| `useState({x: 0, y: 0})` | `ObjectProperty<Point2D>` |

**React Example:**
```javascript
const [cells, setCells] = useState([]);
const [currentImage, setCurrentImage] = useState(null);
const [zoomLevel, setZoomLevel] = useState(1);
```

**Java Equivalent:**
```java
private ObservableList<Cell> cells = FXCollections.observableArrayList();
private ObjectProperty<ImageData> currentImage = new SimpleObjectProperty<>();
private DoubleProperty zoomLevel = new SimpleDoubleProperty(1.0);
```

### Props Passing → Constructor Parameters

**React:**
```javascript
function CanvasArea({ cells, setCells, currentImage, zoomLevel }) {
  // Component logic
}
```

**Java:**
```java
public class CanvasAreaView extends BorderPane {
    public CanvasAreaView(ObservableList<Cell> cells, 
                          ObjectProperty<ImageData> currentImage,
                          DoubleProperty zoomLevel) {
        this.cells = cells;
        this.currentImage = currentImage;
        this.zoomLevel = zoomLevel;
    }
}
```

## Component Structure Conversion

### Functional Components → JavaFX Classes

**React (MenuBar.jsx):**
```javascript
function MenuBar({ setCurrentImage, setSelectedFile }) {
  const handleFileOpen = () => {
    // File opening logic
  };

  return (
    <div className="menu-bar">
      <div className="menu-item" onClick={handleFileOpen}>File</div>
      <div className="menu-item">Edit</div>
      <div className="menu-item" onClick={handleHelp}>Help</div>
    </div>
  );
}
```

**Java (MenuBarView.java):**
```java
public class MenuBarView extends MenuBar {
    private ObjectProperty<ImageData> currentImage;

    public MenuBarView(ObjectProperty<ImageData> currentImage) {
        this.currentImage = currentImage;
        
        Menu fileMenu = new Menu("File");
        MenuItem openItem = new MenuItem("Open");
        openItem.setOnAction(e -> handleFileOpen());
        fileMenu.getItems().add(openItem);
        
        Menu editMenu = new Menu("Edit");
        Menu helpMenu = new Menu("Help");
        MenuItem aboutItem = new MenuItem("About");
        aboutItem.setOnAction(e -> handleHelp());
        helpMenu.getItems().add(aboutItem);
        
        getMenus().addAll(fileMenu, editMenu, helpMenu);
    }
}
```

## Event Handling Conversion

### onClick → setOnAction

**React:**
```javascript
<button onClick={() => zoom(1.2)}>Zoom In</button>
```

**Java:**
```java
Button zoomInBtn = new Button("Zoom In");
zoomInBtn.setOnAction(e -> zoom(1.2, centerX, centerY));
```

### onChange → Listeners

**React:**
```javascript
<input 
  type="number" 
  value={diameter} 
  onChange={(e) => setDiameter(Number(e.target.value))}
/>
```

**Java:**
```java
Spinner<Integer> diameterSpinner = new Spinner<>(0, 500, 0, 1);
diameterSpinner.setEditable(true);
// Value can be retrieved with: diameterSpinner.getValue()
```

### Mouse Events

**React:**
```javascript
<canvas
  onMouseDown={handleMouseDown}
  onMouseMove={handleMouseMove}
  onMouseUp={handleMouseUp}
  onWheel={handleWheel}
/>
```

**Java:**
```java
canvas.setOnMousePressed(this::handleMousePressed);
canvas.setOnMouseMoved(this::handleMouseMoved);
canvas.setOnMouseReleased(this::handleMouseReleased);
canvas.setOnScroll(this::handleScroll);
```

## Effect Hooks Conversion

### useEffect → Listeners

**React (Auto-update on state change):**
```javascript
useEffect(() => {
  redraw();
}, [cells, currentImage, zoomLevel, panX, panY]);
```

**Java (Property listeners):**
```java
cells.addListener((ListChangeListener<Cell>) c -> redraw());
currentImage.addListener((obs, old, newVal) -> redraw());
zoomLevel.addListener((obs, old, newVal) -> redraw());
```

### useRef → Direct Instance Variables

**React:**
```javascript
const canvasRef = useRef(null);
// Usage: canvasRef.current
```

**Java:**
```java
private Canvas canvas = new Canvas();
// Direct usage: canvas
```

## Canvas Rendering Conversion

### Drawing APIs

Both React and Java use similar 2D graphics APIs:

**React (HTML5 Canvas):**
```javascript
const ctx = canvas.getContext('2d');
ctx.fillStyle = '#000000';
ctx.fillRect(0, 0, width, height);
ctx.beginPath();
ctx.arc(x, y, radius, 0, Math.PI * 2);
ctx.fill();
```

**Java (JavaFX Canvas):**
```java
GraphicsContext gc = canvas.getGraphicsContext2D();
gc.setFill(Color.BLACK);
gc.fillRect(0, 0, width, height);
gc.fillOval(x - radius, y - radius, radius * 2, radius * 2);
```

### Transform Stack

**React:**
```javascript
ctx.save();
ctx.translate(panX, panY);
ctx.scale(zoomLevel, zoomLevel);
// Draw operations
ctx.restore();
```

**Java:**
```java
gc.save();
gc.translate(panX, panY);
gc.scale(zoomLevel, zoomLevel);
// Draw operations
gc.restore();
```

## Async Operations Conversion

### Fetch API → HttpClient

**React (async/await):**
```javascript
const computeMasks = async () => {
  const formData = new FormData();
  formData.append('file', selectedFile);
  formData.append('diameter', diameter);

  const response = await fetch(`${backendUrl}/segment`, {
    method: 'POST',
    body: formData
  });

  const data = await response.json();
  setCells(processMaskData(data));
};
```

**Java (Thread + HttpClient):**
```java
private void computeMasks() {
    new Thread(() -> {
        try {
            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpPost uploadFile = new HttpPost(backendUrl + "/segment");

            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.addBinaryBody("file", selectedFile);
            builder.addTextBody("diameter", String.valueOf(diameter));

            uploadFile.setEntity(builder.build());
            CloseableHttpResponse response = httpClient.execute(uploadFile);
            String jsonResponse = EntityUtils.toString(response.getEntity());
            
            Gson gson = new Gson();
            JsonObject data = gson.fromJson(jsonResponse, JsonObject.class);

            // Update UI on JavaFX thread
            Platform.runLater(() -> {
                cells.clear();
                cells.addAll(processMaskData(data));
            });

            httpClient.close();
        } catch (Exception e) {
            Platform.runLater(() -> showError(e.getMessage()));
        }
    }).start();
}
```

## Layout Conversion

### CSS Flexbox → JavaFX Layout Panes

**React:**
```javascript
<div className="app-container">
  <MenuBar />
  <div className="main-content">
    <Sidebar />
    <CanvasArea />
  </div>
</div>
```

**Java:**
```java
public class MainView extends BorderPane {
    public MainView() {
        MenuBarView menuBar = new MenuBarView(currentImage);
        SidebarView sidebar = new SidebarView(cells, currentImage, maskData);
        CanvasAreaView canvasArea = new CanvasAreaView(cells, currentImage, 
                                                        maskData, zoomLevel, mousePosition);
        
        setTop(menuBar);
        setLeft(sidebar);
        setCenter(canvasArea);
    }
}
```

### Layout Containers

| React/CSS | JavaFX |
|-----------|--------|
| `<div>` with flexbox row | `HBox` |
| `<div>` with flexbox column | `VBox` |
| Grid layout | `GridPane` |
| Absolute positioning | `StackPane` or `Pane` |
| Border layout | `BorderPane` |

## Styling Conversion

### CSS Classes → JavaFX CSS

**React:**
```css
.menu-bar {
  background-color: #2a2a2a;
  padding: 8px;
}

.btn {
  background-color: #4a90e2;
  color: white;
}
```

**JavaFX CSS:**
```css
.menu-bar {
  -fx-background-color: #2a2a2a;
  -fx-padding: 8px;
}

.btn {
  -fx-background-color: #4a90e2;
  -fx-text-fill: white;
}
```

**Applying Styles:**
```java
button.getStyleClass().add("btn");
```

## File Handling Conversion

### File Upload

**React:**
```javascript
const input = document.createElement('input');
input.type = 'file';
input.accept = 'image/*';
input.onchange = (e) => {
  const file = e.target.files[0];
  const reader = new FileReader();
  reader.onload = (event) => {
    const img = new Image();
    img.src = event.target.result;
  };
  reader.readAsDataURL(file);
};
input.click();
```

**Java:**
```java
FileChooser fileChooser = new FileChooser();
fileChooser.setTitle("Open Image");
fileChooser.getExtensionFilters().add(
    new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.tif")
);

File file = fileChooser.showOpenDialog(window);
if (file != null) {
    Image image = new Image(new FileInputStream(file));
    ImageData imageData = new ImageData(
        (int) image.getWidth(),
        (int) image.getHeight(),
        image,
        file.getName()
    );
}
```

## Key Differences Summary

### 1. **Threading Model**
- **React**: Single-threaded with async/await
- **Java**: Explicit threading, UI updates must use `Platform.runLater()`

### 2. **Type System**
- **React/JavaScript**: Dynamic typing
- **Java**: Static typing with compile-time checks

### 3. **Reactivity**
- **React**: Virtual DOM diffing and reconciliation
- **Java**: Observable properties and manual listeners

### 4. **Build System**
- **React**: npm/Vite with hot module replacement
- **Java**: Maven with standard compilation

### 5. **Rendering Performance**
- **React**: Optimized virtual DOM updates
- **Java**: Direct rendering, manual optimization needed

### 6. **Component Reusability**
- **React**: HOCs and hooks for composition
- **Java**: Class inheritance and composition

## Best Practices for Conversion

1. **Use Properties for Reactive State**: Leverage JavaFX properties instead of manual getters/setters
2. **Separate UI from Logic**: Keep business logic in separate service classes
3. **Thread Safety**: Always update UI on JavaFX Application Thread
4. **Memory Management**: Be mindful of image sizes and canvas rendering performance
5. **Error Handling**: Use try-catch blocks and show user-friendly error messages
6. **CSS Styling**: Use external CSS files for maintainability
7. **Testing**: Write unit tests for business logic, integration tests for UI

## Advantages of Java Version

- **Type Safety**: Compile-time error detection
- **Desktop Integration**: Better file system access, native dialogs
- **Performance**: Generally faster for compute-intensive operations
- **Deployment**: Single JAR file, no browser dependency
- **Memory Control**: Better control over memory usage

## Advantages of React Version

- **Hot Reload**: Instant feedback during development
- **Cross-Platform**: Runs in any browser
- **Ecosystem**: Rich npm ecosystem
- **Deployment**: Easy web hosting
- **Modern DX**: Better developer experience with modern tooling

---

**Conversion completed successfully!** 

The Java application maintains feature parity with the React version while leveraging JavaFX's strengths for desktop application development.

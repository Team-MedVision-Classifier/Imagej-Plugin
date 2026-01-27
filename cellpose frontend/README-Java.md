# Cellpose Frontend - Java Edition

This is a JavaFX-based desktop application for the Cellpose cell segmentation tool, converted from the original React web application.

## Requirements

- Java 17 or higher
- Maven 3.6 or higher

## Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── com/
│   │       └── cellpose/
│   │           ├── CellposeApp.java           # Main application entry point
│   │           ├── model/
│   │           │   ├── Cell.java              # Cell data model
│   │           │   └── ImageData.java         # Image data model
│   │           └── ui/
│   │               ├── MainView.java          # Main application view
│   │               ├── MenuBarView.java       # Menu bar component
│   │               ├── SidebarView.java       # Sidebar container
│   │               ├── SegmentationSection.java  # Segmentation controls
│   │               ├── DisplaySection.java    # Display controls
│   │               ├── ViewsSection.java      # View controls
│   │               └── CanvasAreaView.java    # Main canvas area
│   └── resources/
│       └── styles.css                         # Application styles
└── pom.xml                                    # Maven configuration
```

## Building and Running

### Using Maven

1. **Build the project:**
   ```bash
   mvn clean install
   ```

2. **Run the application:**
   ```bash
   mvn javafx:run
   ```

### Creating an executable JAR

```bash
mvn clean package
java -jar target/cellpose-frontend-1.0.0.jar
```

## Features

- **Image Loading**: Load cell images from disk
- **Cell Segmentation**: Connect to Cellpose backend for automated cell segmentation
- **Interactive Canvas**: 
  - Pan and zoom with mouse
  - View cell masks and outlines
  - Real-time visualization
- **Segmentation Controls**:
  - Adjustable cell diameter
  - Channel configuration
  - Flow and cellprob thresholds
  - Backend URL configuration
- **Display Controls**: Saturation adjustment and auto-adjust
- **View Modes**: RGB and channel-specific views

## Configuration

The application connects to a Cellpose backend server. The default URL is `http://localhost:8000`, which can be changed in the Segmentation section.

## Controls

- **Mouse Wheel**: Zoom in/out
- **Click and Drag**: Pan the image
- **File Menu**: Open image files
- **Help Menu**: Display usage information

## Dependencies

- **JavaFX 21**: For UI components
- **Gson**: JSON parsing
- **Apache HttpClient 5**: HTTP communication with backend

## Backend Integration

The application expects a Cellpose backend server with the following endpoint:

- `POST /segment`: Accepts multipart form data with:
  - `file`: Image file
  - `diameter`: Cell diameter (0 for auto)
  - `channels`: Channel configuration
  - `flow_threshold`: Flow threshold value
  - `cellprob_threshold`: Cell probability threshold

## Differences from React Version

1. **Desktop Application**: Runs as a native desktop app instead of in a browser
2. **JavaFX Components**: Uses JavaFX UI components instead of HTML/CSS
3. **Threading**: Background tasks run in separate threads to maintain UI responsiveness
4. **File Handling**: Direct file system access without web security restrictions

## Future Enhancements

- Keyboard shortcuts implementation
- More advanced image processing features
- Export functionality for segmentation results
- Multi-image batch processing
- Custom colormap support

## License

Same as the original Cellpose project.

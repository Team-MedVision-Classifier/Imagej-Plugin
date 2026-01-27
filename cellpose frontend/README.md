# Cellpose Frontend - Java Edition

A JavaFX-based desktop application for cell segmentation visualization using Cellpose.

## Overview

This is a desktop application built with Java and JavaFX that provides an interactive interface for cell segmentation visualization. The application communicates with a Cellpose backend server to perform cell segmentation and displays results with interactive pan/zoom controls.

## Requirements

- Java 17 or higher
- Maven 3.6+
- Cellpose backend server (optional, for segmentation)

## Quick Start

### Build the Project
```bash
mvn clean compile
```

### Run the Application
```bash
mvn javafx:run
```

### Create Executable JAR
```bash
mvn package
```

The JAR will be created in `target/cellpose-frontend-1.0.0.jar`

## Features

✅ Interactive canvas with pan and zoom  
✅ Cell segmentation visualization with colored masks  
✅ Cell outline rendering  
✅ File operations (open images)  
✅ Backend API integration for segmentation  
✅ Segmentation parameter controls  
✅ Display settings (saturation, auto-adjust)  
✅ View controls (RGB modes, color channels)  
✅ Real-time status updates (mouse position, zoom, cell count)  
✅ Sample cell generation for demonstration  

## Documentation

- **[README-Java.md](README-Java.md)** - Detailed Java implementation documentation
- **[CONVERSION-GUIDE.md](CONVERSION-GUIDE.md)** - React to Java conversion patterns
- **[CONVERSION-SUMMARY.md](CONVERSION-SUMMARY.md)** - Complete conversion summary
- **[QUICK-START.md](QUICK-START.md)** - Quick reference guide

## Project Structure

```
src/main/
├── java/com/cellpose/
│   ├── CellposeApp.java              # Main application entry
│   ├── model/
│   │   ├── Cell.java                 # Cell data model
│   │   └── ImageData.java            # Image data model
│   └── ui/
│       ├── MainView.java             # Main application view
│       ├── MenuBarView.java          # Menu bar
│       ├── SidebarView.java          # Sidebar container
│       ├── CanvasAreaView.java       # Canvas display
│       ├── SegmentationSection.java  # Segmentation controls
│       ├── DisplaySection.java       # Display settings
│       └── ViewsSection.java         # View controls
└── resources/
    └── styles.css                    # Application styles
```

## Usage

1. **Open an Image**: File → Open, select an image file
2. **Set Parameters**: Configure diameter, channels, thresholds in the Segmentation section
3. **Compute Masks**: Click "compute masks" to run segmentation (requires backend)
4. **Navigate**: Use mouse to pan (drag) and zoom (wheel)
5. **View Results**: Cells are displayed with colored masks and white outlines
- **Compute Masks**: Run segmentation

## Technologies

- **JavaFX 21.0.1** - UI framework
- **Java 17** - Programming language
- **Maven** - Build tool
- **GSON 2.10.1** - JSON parsing
- **Apache HttpClient 5.3** - HTTP communication

## Backend Integration

The application communicates with a Cellpose backend server at `http://localhost:8000` (configurable).

**API Endpoint**: POST `/segment`

**Request Parameters**:
- `file`: Image file (multipart)
- `diameter`: Cell diameter in pixels
- `channels`: Channel configuration (e.g., "0,0")
- `flow_threshold`: Flow threshold (0-1)
- `cellprob_threshold`: Cell probability threshold

## Contributing

See [README-Java.md](README-Java.md) for development guidelines and architecture details.

## License

This is a frontend implementation for Cellpose. For the original Cellpose tool, visit: https://github.com/MouseLand/cellpose

## Credits

Interface inspired by the original Cellpose application by Stringer, Wang, Michaelos, and Pachitariu.

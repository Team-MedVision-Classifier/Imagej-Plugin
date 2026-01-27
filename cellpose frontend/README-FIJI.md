# Cellpose Plugin for Fiji/ImageJ

This is a Fiji/ImageJ plugin for cell segmentation using Cellpose.

## Building the Plugin

1. Make sure you have Maven installed
2. Build the plugin:
```bash
mvn clean package
```

This will create a JAR file in the `target/` directory: `cellpose-frontend-1.0.0.jar`

## Installing in Fiji

### Method 1: Copy to plugins folder
1. Build the plugin using Maven (see above)
2. Copy the generated JAR file to your Fiji `plugins` folder:
   - Windows: `C:\Users\<username>\Fiji.app\plugins\`
   - Mac: `/Applications/Fiji.app/plugins/`
   - Linux: `~/Fiji.app/plugins/`
3. Restart Fiji

### Method 2: Use Fiji's Plugin Installer
1. In Fiji, go to `Plugins > Install PlugIn...`
2. Navigate to the JAR file: `target/cellpose-frontend-1.0.0.jar`
3. Click OK
4. Restart Fiji

## Dependencies

The plugin requires these additional JARs (Maven will download them automatically):
- gson-2.10.1.jar
- httpclient5-5.3.jar
- httpcore5-5.2.jar

If you manually install the plugin, you'll also need to copy these dependency JARs to Fiji's `jars` folder.

### Copying Dependencies Manually

After building with Maven, copy all dependencies:

```bash
# On Windows
copy target\dependency\*.jar "C:\Users\<username>\Fiji.app\jars\"

# On Mac/Linux
cp target/dependency/*.jar ~/Fiji.app/jars/
```

## Using the Plugin

1. Open an image in Fiji (`File > Open...`)
2. Run the plugin: `Plugins > Cellpose > Cellpose Segmentation`
3. The Cellpose interface will open with controls for:
   - **Segmentation**: Configure channel, diameter, thresholds, and backend URL
   - **Display**: Adjust brightness and visibility options
   - **Views**: Color channel controls
4. Click "Compute Masks" to run segmentation
5. Detected cells will be displayed as overlays on the image

## Backend Server

The plugin requires a Cellpose backend server running. Make sure you have:
- Cellpose backend server running (default: `http://localhost:8000`)
- Update the "Backend URL" field in the plugin if using a different address

## Troubleshooting

### Plugin doesn't appear in menu
- Make sure the JAR is in the `plugins` folder
- Restart Fiji completely
- Check Fiji's console for error messages

### ClassNotFoundException errors
- Copy all dependency JARs to `Fiji.app/jars/` folder
- Restart Fiji

### Image not found error
- Make sure an image is open in Fiji before running the plugin
- The plugin works with the currently active image

## Development

To modify the plugin:
1. Edit the source code in `src/main/java/`
2. Rebuild: `mvn clean package`
3. Copy the new JAR to Fiji's plugins folder
4. Restart Fiji

## File Structure

```
src/main/java/com/cellpose/
├── CellposeApp.java          # Main plugin entry point (implements PlugIn)
├── model/
│   ├── Cell.java             # Cell data model
│   └── ImageData.java        # Image metadata
└── ui/
    ├── CellposeFrame.java    # Main Swing frame
    ├── SegmentationPanel.java # Segmentation controls
    ├── DisplayPanel.java      # Display options
    ├── ViewsPanel.java        # View controls
    └── CanvasPanel.java       # Canvas with ImageJ integration

src/main/resources/
└── plugins.config            # Plugin registration file
```

## License

[Your license here]

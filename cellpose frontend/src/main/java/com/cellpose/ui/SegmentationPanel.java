package com.cellpose.ui;

import ij.ImagePlus;
import ij.io.FileSaver;
import ij.gui.Overlay;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import com.cellpose.model.Cell;
import com.cellpose.backend.BackendManager;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;

import javax.swing.*;
import java.awt.*;
import java.awt.Polygon;
import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.function.Consumer;

public class SegmentationPanel extends JPanel {
    private ImagePlus imagePlus;
    private List<Cell> cells;
    private Consumer<List<Cell>> cellUpdateCallback;
    private BackendManager backendManager;
    private boolean backendStarting = false;

    private JComboBox<String> modelTypeCombo;
    private JComboBox<String> modelNameCombo;
    private JComboBox<String> channelCombo;
    private JComboBox<String> channel2Combo;
    private JSpinner diameterSpinner;
    private JCheckBox useGpuCheckBox;

    private JTextField backendUrlField;
    private JCheckBox useExternalBackendCheckBox;
    private JButton fetchModelsButton;
    private JButton additionalSettingsButton;
    private AdditionalSettingsPanel additionalSettingsPanel;
    private boolean additionalSettingsVisible = false;
    
    private JButton computeButton;
    private JTextArea statusLabel;

    private Map<String, List<String>> modelsByType = new HashMap<>();

    public SegmentationPanel(ImagePlus imp, List<Cell> cells, Consumer<List<Cell>> cellUpdateCallback, BackendManager backendManager) {
        this.imagePlus = imp;
        this.cells = cells;
        this.cellUpdateCallback = cellUpdateCallback;
        this.backendManager = backendManager;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Title
        JLabel titleLabel = new JLabel("Segmentation:");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setForeground(new Color(79, 195, 247));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(titleLabel);
        add(Box.createVerticalStrut(10));

        // Backend URL (top item)
        backendUrlField = new JTextField("http://localhost:8000");
        add(createLabeledField("Backend URL:", backendUrlField));

        // External backend toggle
        useExternalBackendCheckBox = new JCheckBox("Use external backend");
        useExternalBackendCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        useExternalBackendCheckBox.setSelected(false);
        useExternalBackendCheckBox.addActionListener(e -> handleBackendToggle());
        add(Box.createVerticalStrut(5));
        add(useExternalBackendCheckBox);

        // Fetch Models Button
        fetchModelsButton = new JButton("Fetch Models");
        fetchModelsButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        fetchModelsButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        fetchModelsButton.addActionListener(e -> fetchModels());
        add(Box.createVerticalStrut(5));
        add(fetchModelsButton);
        add(Box.createVerticalStrut(10));

        // Model Type
        String[] models = {"Cellpose SAM", "Cellpose 3.1"};
        modelTypeCombo = new JComboBox<>(models);
        modelTypeCombo.addActionListener(e -> updateModelNameOptions());
        add(createLabeledCombo("Model Type:", modelTypeCombo));

        // Model Name (populated from backend)
        modelNameCombo = new JComboBox<>(new String[]{});
        add(createLabeledCombo("Model Name:", modelNameCombo));
        updateModelNameOptions();

        // Channel selectors populated from the image
        int nChannels = 1;
        try {
            nChannels = Math.max(1, imagePlus.getNChannels());
        } catch (Throwable t) {
            nChannels = 1;
        }

        String[] channelOptions = new String[nChannels];
        for (int i = 0; i < nChannels; i++) {
            channelOptions[i] = "Channel " + (i + 1);
        }

        channelCombo = new JComboBox<>(channelOptions);
        channelCombo.setSelectedIndex(0);
        add(createLabeledCombo("Channel to segment:", channelCombo));

        // Channel 2 (optional) - first option is "None"
        String[] channel2Options = new String[nChannels + 1];
        channel2Options[0] = "None";
        for (int i = 0; i < nChannels; i++) channel2Options[i + 1] = "Channel " + (i + 1);
        channel2Combo = new JComboBox<>(channel2Options);
        channel2Combo.setSelectedIndex(0);
        add(createLabeledCombo("Channel 2 (optional):", channel2Combo));

        // Diameter
        add(createLabeledSpinner("Diameter:", 30, 0, 500, diameterSpinner = new JSpinner(new SpinnerNumberModel(30, 0, 500, 1))));

        // GPU Checkbox
        add(createLabeledCheckBox("Use GPU:", useGpuCheckBox = new JCheckBox()));
        add(Box.createVerticalStrut(5));

        // Additional Settings Button
        additionalSettingsButton = new JButton("Additional Settings ▼");
        additionalSettingsButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        additionalSettingsButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        additionalSettingsButton.addActionListener(e -> toggleAdditionalSettings());
        add(Box.createVerticalStrut(10));
        add(additionalSettingsButton);

        // Additional Settings Panel (initially hidden)
        additionalSettingsPanel = new AdditionalSettingsPanel();
        additionalSettingsPanel.setVisible(false);
        add(additionalSettingsPanel);

        // Compute button
        computeButton = new JButton("Compute Masks");
        computeButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        computeButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        computeButton.addActionListener(e -> computeMasks());
        add(Box.createVerticalStrut(10));
        add(computeButton);

        // Status label
        statusLabel = new JTextArea(3, 24);
        statusLabel.setEditable(false);
        statusLabel.setLineWrap(true);
        statusLabel.setWrapStyleWord(true);
        statusLabel.setOpaque(false);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Wrap status label in a container panel to prevent layout shifts
        JPanel statusContainer = new JPanel(new BorderLayout());
        statusContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusContainer.setMaximumSize(new Dimension(290, Integer.MAX_VALUE));
        statusContainer.setOpaque(false);
        statusContainer.add(statusLabel, BorderLayout.CENTER);

        add(Box.createVerticalStrut(10));
        add(statusContainer);

        // Auto-start bundled backend by default (only if bundled resources exist)
        SwingUtilities.invokeLater(() -> {
            if (backendManager != null && backendManager.isBundledBackendAvailable()) {
                startBundledBackendAsync();
            } else {
                useExternalBackendCheckBox.setSelected(true);
                backendUrlField.setEnabled(true);
                setStatusText("Bundled backend not found. Using external URL.", Color.ORANGE);
            }
        });
    }

    public void setBackendUrl(String backendUrl) {
        if (backendUrl != null && !backendUrl.trim().isEmpty()) {
            backendUrlField.setText(backendUrl.trim());
        }
    }

    private void handleBackendToggle() {
        if (useExternalBackendCheckBox.isSelected()) {
            backendStarting = false;
            if (backendManager != null) {
                backendManager.stop();
            }
            backendUrlField.setEnabled(true);
            setStatusText("Using external backend.", new Color(76, 175, 80));
        } else {
            startBundledBackendAsync();
        }
    }

    private void startBundledBackendAsync() {
        if (backendManager == null || backendStarting) return;
        if (!backendManager.isBundledBackendAvailable()) {
            useExternalBackendCheckBox.setSelected(true);
            backendUrlField.setEnabled(true);
            setStatusText("Bundled backend not found. Using external URL.", Color.ORANGE);
            return;
        }
        backendStarting = true;
        backendUrlField.setEnabled(false);
        setStatusText("Starting bundled backend...", Color.ORANGE);

        new Thread(() -> {
            try {
                String url = backendManager.start();
                SwingUtilities.invokeLater(() -> {
                    backendUrlField.setText(url);
                    backendUrlField.setEnabled(false);
                    useExternalBackendCheckBox.setSelected(false);
                    setStatusText("Bundled backend ready.", new Color(76, 175, 80));
                    backendStarting = false;
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    backendUrlField.setEnabled(true);
                    useExternalBackendCheckBox.setSelected(true);
                    setStatusText("Failed to start bundled backend. Use external URL.", Color.RED);
                    backendStarting = false;
                });
            }
        }).start();
    }

    private JPanel createLabeledSpinner(String labelText, int value, int min, int max, JSpinner spinner) {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(5, 0));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        JLabel label = new JLabel(labelText);
        label.setPreferredSize(new Dimension(120, 25));
        panel.add(label, BorderLayout.WEST);
        panel.add(spinner, BorderLayout.CENTER);

        add(Box.createVerticalStrut(5));
        return panel;
    }

    private JPanel createLabeledCheckBox(String labelText, JCheckBox checkBox) {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(5, 0));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        JLabel label = new JLabel(labelText);
        label.setPreferredSize(new Dimension(120, 25));
        panel.add(label, BorderLayout.WEST);
        panel.add(checkBox, BorderLayout.CENTER);

        add(Box.createVerticalStrut(5));
        return panel;
    }

    private JPanel createLabeledField(String labelText, JTextField field) {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(5, 0));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        JLabel label = new JLabel(labelText);
        label.setPreferredSize(new Dimension(120, 25));
        panel.add(label, BorderLayout.WEST);
        panel.add(field, BorderLayout.CENTER);

        add(Box.createVerticalStrut(5));
        return panel;
    }

    private JPanel createLabeledCombo(String labelText, JComboBox<String> combo) {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(5, 0));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        JLabel label = new JLabel(labelText);
        label.setPreferredSize(new Dimension(120, 25));
        panel.add(label, BorderLayout.WEST);
        panel.add(combo, BorderLayout.CENTER);

        add(Box.createVerticalStrut(5));
        return panel;
    }

    private void toggleAdditionalSettings() {
        additionalSettingsVisible = !additionalSettingsVisible;
        additionalSettingsPanel.setVisible(additionalSettingsVisible);
        additionalSettingsButton.setText(additionalSettingsVisible ? "Additional Settings ▲" : "Additional Settings ▼");
        revalidate();
        repaint();
    }

    private void fetchModels() {
        fetchModelsButton.setEnabled(false);
        setStatusText("Fetching models...", Color.ORANGE);

        new Thread(() -> {
            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                String url = buildEndpoint(backendUrlField.getText(), "/getModels");
                HttpGet get = new HttpGet(url);
                CloseableHttpResponse response = httpClient.execute(get);
                int statusCode = response.getCode();

                if (statusCode == 200) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(response.getEntity().getContent())
                    );
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();

                    Map<String, List<String>> finalResult = parseModelsJson(sb.toString());

                    SwingUtilities.invokeLater(() -> {
                        modelsByType = finalResult;
                        updateModelNameOptions();
                        setStatusText("Models updated.", new Color(76, 175, 80));
                        fetchModelsButton.setEnabled(true);
                    });
                } else {
                    String errorMsg = new String(response.getEntity().getContent().readAllBytes());
                    SwingUtilities.invokeLater(() -> {
                        setStatusText("Error: " + errorMsg, Color.RED);
                        fetchModelsButton.setEnabled(true);
                    });
                }

                response.close();
            } catch (Exception ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    setStatusText("Error: " + ex.getMessage(), Color.RED);
                    fetchModelsButton.setEnabled(true);
                });
            }
        }).start();
    }

    private void updateModelNameOptions() {
        String selectedType = (String) modelTypeCombo.getSelectedItem();
        String backendKey = toBackendModelType(selectedType);
        List<String> models = modelsByType.getOrDefault(backendKey, new ArrayList<>());

        modelNameCombo.removeAllItems();
        if (models.isEmpty()) {
            modelNameCombo.addItem("No models");
            modelNameCombo.setEnabled(false);
        } else {
            for (String model : models) {
                modelNameCombo.addItem(model);
            }
            modelNameCombo.setEnabled(true);
        }
    }

    private void computeMasks() {
        computeButton.setEnabled(false);
        setStatusText("Computing...", Color.ORANGE);

        new Thread(() -> {
            File tempImageFile = null;
            File fileToSend = null;
            boolean createdTemp = false;
            try {
                // Try to send the original ImageJ file when available
                try {
                    ij.io.FileInfo fi = imagePlus.getOriginalFileInfo();
                    if (fi != null && fi.fileName != null) {
                        String dir = fi.directory != null ? fi.directory : "";
                        File orig = new File(dir, fi.fileName);
                        if (orig.exists()) {
                            fileToSend = orig;
                        }
                    }
                } catch (Throwable t) {
                    // Some ImagePlus instances or ImageJ builds may not expose
                    // original file info; ignore and fall back to creating PNG.
                }

                // If original file not available on disk, save a temporary PNG
                // if (fileToSend == null) {
                //     tempImageFile = File.createTempFile("cellpose_", ".png");
                //     FileSaver fs = new FileSaver(imagePlus);
                //     fs.saveAsPng(tempImageFile.getAbsolutePath());
                //     fileToSend = tempImageFile;
                //     createdTemp = true;
                // }

                // 2. Prepare multipart request
                String modelType = toBackendModelType((String) modelTypeCombo.getSelectedItem());
                String modelName = null;
                if (modelNameCombo.isEnabled()) {
                    modelName = (String) modelNameCombo.getSelectedItem();
                    if (modelName != null && "No models".equals(modelName)) {
                        modelName = null;
                    }
                }
                int channel1Index = Math.max(0, channelCombo.getSelectedIndex()); // 0-based
                int channel2Selection = channel2Combo.getSelectedIndex(); // 0 = None, else 1..n
                String channels;
                if (channel2Selection <= 0) {
                    channels = String.valueOf(channel1Index);
                } else {
                    int channel2Index = channel2Selection - 1;
                    channels = channel1Index + "," + channel2Index;
                }
                int diameter = (Integer) diameterSpinner.getValue();
                boolean useGpu = useGpuCheckBox.isSelected();
                
                // Get additional settings from the panel
                int batchSize = additionalSettingsPanel.getBatchSize();
                boolean resample = additionalSettingsPanel.isResample();
                boolean normalize = additionalSettingsPanel.isNormalize();
                double flowThreshold = additionalSettingsPanel.getFlowThreshold();
                double cellprobThreshold = additionalSettingsPanel.getCellprobThreshold();

                // Build URL with all parameters
                String url = buildEndpoint(backendUrlField.getText(), "/segment") +
                    "?model_type=" + encodeUrlParam(modelType) +
                        "&diameter=" + diameter +
                        "&channels=" + channels +
                        "&use_gpu=" + useGpu +
                        "&batch_size=" + batchSize +
                        "&resample=" + resample +
                        "&normalize=" + normalize +
                        "&flow_threshold=" + flowThreshold +
                        "&cellprob_threshold=" + cellprobThreshold;

                if (modelName != null && !modelName.trim().isEmpty()) {
                    url += "&model_name=" + encodeUrlParam(modelName);
                }
                
                // Add normalization sub-options if normalize is enabled
                if (normalize) {
                    url += "&percentile_low=" + additionalSettingsPanel.getPercentileLow() +
                           "&percentile_high=" + additionalSettingsPanel.getPercentileHigh() +
                           "&tile_norm=" + additionalSettingsPanel.getTileNorm();
                }

                // 3. Build multipart entity
                HttpEntity entity = MultipartEntityBuilder.create()
                    .addBinaryBody("image", fileToSend, ContentType.APPLICATION_OCTET_STREAM, fileToSend.getName())
                    .build();

                // 4. Make HTTP request
                CloseableHttpClient httpClient = HttpClients.createDefault();
                HttpPost post = new HttpPost(url);
                post.setEntity(entity);

                CloseableHttpResponse response = httpClient.execute(post);
                int statusCode = response.getCode();

                if (statusCode == 200) {
                    // 5. Parse response - each line contains ROI coordinates
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(response.getEntity().getContent())
                    );

                    List<Cell> newCells = new ArrayList<>();
                    List<Roi> rois = new ArrayList<>();
                    String line;
                    int cellId = 1;

                    while ((line = reader.readLine()) != null) {
                        if (line.trim().isEmpty()) continue;

                        // Parse coordinates (format: "x1,y1,x2,y2,...")
                        String[] coords = line.split(",");
                        if (coords.length >= 2) {
                            // Create polygon ROI from coordinates
                            float[] xPoints = new float[coords.length / 2];
                            float[] yPoints = new float[coords.length / 2];

                            double sumX = 0, sumY = 0;
                            int numPoints = coords.length / 2;

                            for (int i = 0; i < coords.length; i += 2) {
                                xPoints[i / 2] = Float.parseFloat(coords[i]);
                                yPoints[i / 2] = Float.parseFloat(coords[i + 1]);
                                sumX += xPoints[i / 2];
                                sumY += yPoints[i / 2];
                            }

                            double centerX = sumX / numPoints;
                            double centerY = sumY / numPoints;

                            // Create ImageJ ROI
                            PolygonRoi roi = new PolygonRoi(xPoints, yPoints, Roi.POLYGON);
                            roi.setName("Cell_" + cellId);
                            roi.setStrokeColor(Color.YELLOW);
                            roi.setStrokeWidth(2);
                            rois.add(roi);

                            // Estimate radius as average distance from center
                            double sumDist = 0;
                            for (int i = 0; i < coords.length; i += 2) {
                                double dx = Double.parseDouble(coords[i]) - centerX;
                                double dy = Double.parseDouble(coords[i + 1]) - centerY;
                                sumDist += Math.sqrt(dx * dx + dy * dy);
                            }
                            double radius = sumDist / numPoints;

                            // Create cell with random color
                            Color cellColor = new Color(
                                    (int)(Math.random() * 156 + 100),
                                    (int)(Math.random() * 156 + 100),
                                    (int)(Math.random() * 156 + 100)
                            );

                            Cell cell = new Cell(cellId++, centerX, centerY, radius, cellColor, 0.0);
                            cell.setRoi(roi);
                            newCells.add(cell);
                        }
                    }

                    reader.close();

                    // Add ROIs to image overlay and RoiManager
                    SwingUtilities.invokeLater(() -> {
                        // Add to overlay - all outlines will be visible at once
                        Overlay overlay = new Overlay();

                        for (Roi roi : rois) {
                            overlay.add(roi);
                        }
                        imagePlus.setOverlay(overlay);
                        imagePlus.updateAndDraw();

                        cellUpdateCallback.accept(newCells);
                        setStatusText("Segmentation complete! Found " + newCells.size() + " cells", new Color(76, 175, 80));
                        computeButton.setEnabled(true);
                    });
                } else {
                    String errorMsg = new String(response.getEntity().getContent().readAllBytes());
                    SwingUtilities.invokeLater(() -> {
                        ij.IJ.error("Segmentation Error", "Segmentation failed:\n" + errorMsg);
                        setStatusText("Error: " + errorMsg, Color.RED);
                        computeButton.setEnabled(true);
                    });
                }

                httpClient.close();
            } catch (Exception ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    ij.IJ.error("Segmentation Error", "An error occurred during segmentation:\n" + ex.getMessage());
                    setStatusText("Error: " + ex.getMessage(), Color.RED);
                    computeButton.setEnabled(true);
                });
            } finally {
                // Clean up only the temp file we created
                if (createdTemp && tempImageFile != null && tempImageFile.exists()) {
                    tempImageFile.delete();
                }
            }
        }).start();
    }

    private void setStatusText(String message, Color color) {
        statusLabel.setText(message);
        statusLabel.setForeground(color);
    }

    private Map<String, List<String>> parseModelsJson(String json) {
        Map<String, List<String>> result = new HashMap<>();
        if (json == null) return result;

        result.put("Cellpose3.1", parseJsonArray(json, "Cellpose3.1"));
        result.put("CellposeSAM", parseJsonArray(json, "CellposeSAM"));
        return result;
    }

    private List<String> parseJsonArray(String json, String key) {
        List<String> values = new ArrayList<>();
        if (json == null || key == null) return values;

        Pattern pattern = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*\\[(.*?)\\]", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) return values;

        String arrayContent = matcher.group(1).trim();
        if (arrayContent.isEmpty()) return values;

        String[] parts = arrayContent.split(",");
        for (String part : parts) {
            String item = part.trim();
            if (item.startsWith("\"") && item.endsWith("\"")) {
                item = item.substring(1, item.length() - 1);
            }
            if (!item.isEmpty()) {
                values.add(item);
            }
        }
        return values;
    }

    private String buildEndpoint(String baseUrl, String path) {
        if (baseUrl == null) return path;
        String trimmed = baseUrl.trim();
        if (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        if (path != null && !path.startsWith("/")) {
            return trimmed + "/" + path;
        }
        return trimmed + path;
    }

    private String encodeUrlParam(String value) {
        try {
            return URLEncoder.encode(value == null ? "" : value, "UTF-8");
        } catch (Exception e) {
            return value;
        }
    }

    private String toBackendModelType(String displayName) {
        if (displayName == null) return "";
        if ("Cellpose SAM".equals(displayName)) return "CellposeSAM";
        if ("Cellpose 3.1".equals(displayName)) return "Cellpose3.1";
        return displayName;
    }
}
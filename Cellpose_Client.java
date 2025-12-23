import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.io.FileSaver;
import ij.plugin.PlugIn;
import ij.plugin.frame.RoiManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class Cellpose_Client implements PlugIn {

    private static final String API_BASE_URL = "https://apexw-cellposesampre.hf.space/segment";
    private static final int TIMEOUT_MS = 300000; // 5 Minutes

    // --- STATE ---
    private ImagePlus sourceImp;
    private BufferedImage originalImage;
    private JLabel imageLabel;
    private JProgressBar progressBar;
    private JLabel statusLabel;
    private JButton runButton;

    // GUI Controls
    private JSpinner diameterSpinner;
    private JCheckBox autoDiameterCheck;
    private JSpinner scaleSpinner;
    private JComboBox<String> channel1Combo;
    private JComboBox<String> channel2Combo;
    private JSlider flowSlider;

    @Override
    public void run(String arg) {
        sourceImp = WindowManager.getCurrentImage();
        if (sourceImp == null) {
            IJ.error("No image open. Please open an image first.");
            return;
        }

        originalImage = sourceImp.getBufferedImage();

        // Main Window
        JFrame frame = new JFrame("Cellpose Client: " + sourceImp.getTitle());
        frame.setSize(1000, 700);
        frame.setLayout(new BorderLayout());
        frame.setLocationRelativeTo(null);

        // LEFT: Controls
        JPanel controlsPanel = createControlsPanel(sourceImp.getWidth());

        // RIGHT: Image Viewer (Using standard JLabel in ScrollPane)
        imageLabel = new JLabel();
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imageLabel.setVerticalAlignment(SwingConstants.CENTER);
        imageLabel.setOpaque(true);
        imageLabel.setBackground(Color.DARK_GRAY);

        // Initial Draw (Just the image, no ROIs yet)
        updateDisplay(new ArrayList<Polygon>());

        JScrollPane scrollPane = new JScrollPane(imageLabel);
        scrollPane.getViewport().setBackground(Color.DARK_GRAY);

        // Split Pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, controlsPanel, scrollPane);
        splitPane.setDividerLocation(300);

        frame.add(splitPane, BorderLayout.CENTER);
        frame.setVisible(true);
    }

    /**
     * Draws the image and ROIs onto a single BufferedImage.
     */
    private void updateDisplay(List<Polygon> rois) {
        if (originalImage == null) return;

        int w = originalImage.getWidth();
        int h = originalImage.getHeight();

        BufferedImage combined = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = combined.createGraphics();

        // 1. Draw Image
        g2.drawImage(originalImage, 0, 0, null);

        // 2. Draw ROIs
        g2.setColor(Color.YELLOW);
        g2.setStroke(new BasicStroke(2.0f));

        if (rois != null) {
            for (Polygon p : rois) {
                g2.drawPolygon(p);
            }
        }
        g2.dispose();

        // 3. Update UI
        imageLabel.setIcon(new ImageIcon(combined));
        imageLabel.repaint();
    }

    private JPanel createControlsPanel(int originalWidth) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(15, 15, 15, 15));
        p.setMinimumSize(new Dimension(300, 0));

        // Title
        JLabel title = new JLabel("Settings");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(title);
        p.add(Box.createVerticalStrut(15));

        // Diameter
        JPanel diamPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 5));
        diamPanel.setBorder(BorderFactory.createTitledBorder("Diameter (px)"));
        diamPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        autoDiameterCheck = new JCheckBox("Auto", true);
        diameterSpinner = new JSpinner(new SpinnerNumberModel(30.0, 1.0, 1000.0, 1.0));
        diameterSpinner.setEnabled(false);
        diameterSpinner.setPreferredSize(new Dimension(80, 25));

        autoDiameterCheck.addActionListener(e -> diameterSpinner.setEnabled(!autoDiameterCheck.isSelected()));

        diamPanel.add(autoDiameterCheck);
        diamPanel.add(Box.createHorizontalStrut(10));
        diamPanel.add(diameterSpinner);
        p.add(diamPanel);
        p.add(Box.createVerticalStrut(10));

        // Scaling
        JPanel scalePanel = new JPanel(new GridLayout(2, 1));
        scalePanel.setBorder(BorderFactory.createTitledBorder("Resolution Reduction"));
        scalePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        scalePanel.setMaximumSize(new Dimension(300, 80));

        double defaultScale = 1.0;
        if (originalWidth > 500) {
            defaultScale = 500.0 / originalWidth;
            defaultScale = Math.round(defaultScale * 100.0) / 100.0;
        }

        scaleSpinner = new JSpinner(new SpinnerNumberModel(defaultScale, 0.05, 1.0, 0.05));
        JLabel sizeLabel = new JLabel("Est. Width: " + (int)(originalWidth * defaultScale) + "px");
        sizeLabel.setForeground(Color.GRAY);

        scaleSpinner.addChangeListener(e -> {
            double val = (Double) scaleSpinner.getValue();
            sizeLabel.setText("Est. Width: " + (int)(originalWidth * val) + "px");
        });

        scalePanel.add(scaleSpinner);
        scalePanel.add(sizeLabel);
        p.add(scalePanel);
        p.add(Box.createVerticalStrut(10));

        // Channels
        JPanel chanPanel = new JPanel(new GridLayout(4, 1, 0, 2));
        chanPanel.setBorder(BorderFactory.createTitledBorder("Channels"));
        chanPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        chanPanel.setMaximumSize(new Dimension(300, 120));

        String[] opts = {"Grayscale (0)", "Red (1)", "Green (2)", "Blue (3)"};
        String[] opts2 = {"None (0)", "Red (1)", "Green (2)", "Blue (3)"};

        channel1Combo = new JComboBox<>(opts);
        channel2Combo = new JComboBox<>(opts2);

        chanPanel.add(new JLabel("Main Object:"));
        chanPanel.add(channel1Combo);
        chanPanel.add(new JLabel("Nuclei (Helper):"));
        chanPanel.add(channel2Combo);
        p.add(chanPanel);
        p.add(Box.createVerticalStrut(10));

        // Flow
        JPanel flowPanel = new JPanel(new BorderLayout());
        flowPanel.setBorder(BorderFactory.createTitledBorder("Flow Threshold"));
        flowPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        flowPanel.setMaximumSize(new Dimension(300, 70));

        JLabel flowVal = new JLabel("0.4");
        flowSlider = new JSlider(0, 100, 40);
        flowSlider.addChangeListener(e -> flowVal.setText(String.format("%.2f", flowSlider.getValue()/100.0)));

        flowPanel.add(flowVal, BorderLayout.EAST);
        flowPanel.add(flowSlider, BorderLayout.CENTER);
        p.add(flowPanel);

        p.add(Box.createVerticalGlue());

        // Progress
        statusLabel = new JLabel("Ready");
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        progressBar = new JProgressBar(0, 100);
        progressBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        progressBar.setStringPainted(true);

        p.add(statusLabel);
        p.add(Box.createVerticalStrut(5));
        p.add(progressBar);
        p.add(Box.createVerticalStrut(10));

        // Button
        runButton = new JButton("Run Segmentation");
        runButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        runButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        runButton.setBackground(new Color(70, 130, 180));
        runButton.setForeground(Color.BLACK);
        runButton.setMaximumSize(new Dimension(300, 40));

        runButton.addActionListener(e -> startSegmentation());

        p.add(runButton);

        return p;
    }

    private void startSegmentation() {
        runButton.setEnabled(false);
        progressBar.setValue(0);
        statusLabel.setText("Starting...");

        // Parameters
        double diameter = autoDiameterCheck.isSelected() ? 0.0 : (Double) diameterSpinner.getValue();
        double scaleFactor = (Double) scaleSpinner.getValue();
        double flow = flowSlider.getValue() / 100.0;

        int c1 = getChannelIndex((String) channel1Combo.getSelectedItem());
        int c2 = getChannelIndex((String) channel2Combo.getSelectedItem());
        String channelsParam = c1 + "," + c2;

        new Thread(() -> {
            File tempInputFile = null;
            try {
                IJ.log("--- Starting Job ---");

                // 1. SAVE
                updateStatus(10, "Preprocessing Image...");
                tempInputFile = File.createTempFile("cp_dash_", ".tif");
                ImagePlus toSend = sourceImp;
                if (scaleFactor < 1.0) {
                    int w = (int)(sourceImp.getWidth() * scaleFactor);
                    int h = (int)(sourceImp.getHeight() * scaleFactor);
                    toSend = sourceImp.resize(w, h, "bilinear");
                }
                new FileSaver(toSend).saveAsTiff(tempInputFile.getAbsolutePath());

                // 2. UPLOAD
                updateStatus(30, "Uploading...");
                String query = String.format("?diameter=%s&channels=%s&flow_threshold=%s", diameter, channelsParam, flow);

                URL url = new URL(API_BASE_URL + query);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setDoOutput(true);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=---Boundary");
                conn.setConnectTimeout(TIMEOUT_MS);
                conn.setReadTimeout(TIMEOUT_MS);

                try (OutputStream out = conn.getOutputStream();
                     FileInputStream fis = new FileInputStream(tempInputFile)) {
                    PrintWriter w = new PrintWriter(new OutputStreamWriter(out, "UTF-8"), true);
                    w.append("-----Boundary\r\n").append("Content-Disposition: form-data; name=\"image\"; filename=\"i.tif\"\r\n").append("Content-Type: image/tiff\r\n\r\n").flush();
                    byte[] buf = new byte[4096]; int read;
                    while ((read = fis.read(buf)) != -1) out.write(buf, 0, read);
                    out.flush();
                    w.append("\r\n-----Boundary--\r\n").flush();
                }

                // 3. PROCESS
                updateStatus(70, "Waiting for Server...");
                int code = conn.getResponseCode();

                if (code == 200) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    List<Polygon> newRois = new ArrayList<>();
                    String line;

                    while ((line = br.readLine()) != null) {
                        if (line.trim().isEmpty()) continue;
                        String[] parts = line.split(",");
                        int n = parts.length / 2;
                        int[] x = new int[n];
                        int[] y = new int[n];

                        for (int i = 0; i < n; i++) {
                            // --- CORRECTED COORDINATE MAPPING (SWAPPED AGAIN) ---
                            // Now assuming: [X, Y, X, Y...]

                            float rawX = Float.parseFloat(parts[i*2]);   // First is X
                            float rawY = Float.parseFloat(parts[i*2+1]); // Second is Y

                            x[i] = (int) (rawX / scaleFactor);
                            y[i] = (int) (rawY / scaleFactor);
                        }

                        newRois.add(new Polygon(x, y, n));
                    }

                    SwingUtilities.invokeLater(() -> {
                        updateDisplay(newRois);
                        addRoisToManager(newRois);
                        updateStatus(100, "Done! " + newRois.size() + " ROIs.");
                        runButton.setEnabled(true);
                    });
                } else {
                    throw new IOException("Server Error: " + code);
                }

            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Error: " + e.getMessage());
                    runButton.setEnabled(true);
                });
            } finally {
                if (tempInputFile != null) tempInputFile.delete();
            }
        }).start();
    }

    private void addRoisToManager(List<Polygon> polys) {
        RoiManager rm = RoiManager.getRoiManager();
        if (rm == null) { rm = new RoiManager(); rm.setVisible(true); }

        for (int i = 0; i < polys.size(); i++) {
            Polygon p = polys.get(i);
            // Standard order: X array, Y array
            Roi roi = new PolygonRoi(p.xpoints, p.ypoints, p.npoints, Roi.POLYGON);
            roi.setName("Cell-" + (i + 1));
            rm.addRoi(roi);
        }
        rm.runCommand(sourceImp, "Show All");
    }

    private void updateStatus(int val, String msg) {
        SwingUtilities.invokeLater(() -> {
            progressBar.setValue(val);
            statusLabel.setText(msg);
        });
    }

    private int getChannelIndex(String s) {
        if(s.contains("(1)")) return 1;
        if(s.contains("(2)")) return 2;
        if(s.contains("(3)")) return 3;
        return 0;
    }
}
package com.cellpose.ui;

import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.gui.OvalRoi;
import com.cellpose.model.Cell;
import com.cellpose.model.ImageData;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class CanvasPanel extends JPanel {
    private ImagePlus imagePlus;
    private List<Cell> cells;
    private ImageData imageData;
    
    private JLabel filePathLabel;
    private JLabel zoomLabel;
    private JLabel cellCountLabel;
    private JLabel mousePositionLabel;
    
    private ImageCanvas imageCanvas;
    private double zoomLevel = 1.0;
    private Point dragStart;
    private Point panOffset = new Point(0, 0);
    
    public CanvasPanel(ImagePlus imp, List<Cell> cells, ImageData imageData) {
        this.imagePlus = imp;
        this.cells = new ArrayList<>();
        this.imageData = imageData;
        
        setLayout(new BorderLayout());
        setBackground(Color.BLACK);
        
        // Header
        JPanel header = createHeader();
        add(header, BorderLayout.NORTH);
        
        // Canvas area
        JPanel canvasArea = new JPanel(new GridBagLayout());
        canvasArea.setBackground(Color.BLACK);
        
        // Create a panel to hold the ImageJ canvas
        JPanel canvasHolder = new JPanel(new GridBagLayout());
        canvasHolder.setBackground(Color.BLACK);
        
        // Add ImageJ's canvas
        if (imagePlus != null && imagePlus.getWindow() != null) {
            imageCanvas = imagePlus.getCanvas();
            if (imageCanvas != null) {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridx = 0;
                gbc.gridy = 0;
                gbc.anchor = GridBagConstraints.CENTER;
                canvasHolder.add(imageCanvas, gbc);
            }
        }
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        canvasArea.add(canvasHolder, gbc);
        add(canvasArea, BorderLayout.CENTER);
        
        // Add component listener to resize image when panel size changes
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                fitImageToCanvas();
            }
        });
        
        // Footer
        JPanel footer = createFooter();
        add(footer, BorderLayout.SOUTH);
        
        updateLabels();
    }
    
    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(37, 37, 37));
        header.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        
        filePathLabel = new JLabel("File: " + (imageData != null ? imageData.getName() : "No image"));
        filePathLabel.setForeground(Color.WHITE);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttonPanel.setOpaque(false);
        
        JButton resetButton = new JButton("Reset View");
        resetButton.addActionListener(e -> resetView());
        
        JButton clearButton = new JButton("Clear All");
        clearButton.addActionListener(e -> clearCells());
        
        buttonPanel.add(resetButton);
        buttonPanel.add(clearButton);
        
        header.add(filePathLabel, BorderLayout.WEST);
        header.add(buttonPanel, BorderLayout.EAST);
        
        return header;
    }
    
    private JPanel createFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 8));
        footer.setBackground(new Color(37, 37, 37));
        
        zoomLabel = new JLabel("Zoom: 100%");
        zoomLabel.setForeground(Color.WHITE);
        
        cellCountLabel = new JLabel("Cells: 0");
        cellCountLabel.setForeground(Color.WHITE);
        
        mousePositionLabel = new JLabel("Position: (0, 0)");
        mousePositionLabel.setForeground(Color.WHITE);
        
        footer.add(zoomLabel);
        footer.add(new JSeparator(SwingConstants.VERTICAL));
        footer.add(cellCountLabel);
        footer.add(new JSeparator(SwingConstants.VERTICAL));
        footer.add(mousePositionLabel);
        
        return footer;
    }
    
    private void updateLabels() {
        if (filePathLabel != null) {
            filePathLabel.setText("File: " + (imageData != null ? imageData.getName() : "No image"));
        }
        if (zoomLabel != null) {
            zoomLabel.setText(String.format("Zoom: %.0f%%", zoomLevel * 100));
        }
        if (cellCountLabel != null) {
            cellCountLabel.setText("Cells: " + cells.size());
        }
    }
    
    public void updateCells(List<Cell> newCells) {
        cells.clear();
        cells.addAll(newCells);
        updateLabels();
        repaint();
    }
    
    private void resetView() {
        fitImageToCanvas();
        panOffset = new Point(0, 0);
        updateLabels();
        repaint();
    }
    
    private void clearCells() {
        cells.clear();
        if (imagePlus != null) {
            imagePlus.setOverlay(null);
        }
        updateLabels();
        repaint();
    }
    
    private void fitImageToCanvas() {
        if (imagePlus == null || imageCanvas == null) {
            return;
        }
        
        // Get available space (subtract header and footer heights)
        int availableWidth = getWidth();
        int availableHeight = getHeight() - 100; // Approximate header + footer height
        
        if (availableWidth <= 0 || availableHeight <= 0) {
            return;
        }
        
        // Get image dimensions
        int imageWidth = imagePlus.getWidth();
        int imageHeight = imagePlus.getHeight();
        
        // Calculate zoom to fit with margin
        double zoomX = (double) (availableWidth - 40) / imageWidth; // 20px margin on each side
        double zoomY = (double) (availableHeight - 40) / imageHeight; // 20px margin top and bottom
        double newZoom = Math.min(zoomX, zoomY);
        
        // Set the magnification
        imageCanvas.setMagnification(newZoom);
        imageCanvas.setSize((int)(imageWidth * newZoom), (int)(imageHeight * newZoom));
        zoomLevel = newZoom;
        
        updateLabels();
        revalidate();
        repaint();
    }
}

package com.cellpose.ui;

import ij.ImagePlus;
import ij.IJ;
import javax.swing.*;
import java.awt.*;
import java.util.stream.Collectors;
import com.cellpose.model.Cell;
import com.cellpose.model.ImageData;
import java.util.ArrayList;
import java.util.List;

public class CellposeFrame extends JFrame {
    private ImagePlus imagePlus;
    private List<Cell> cells;
    private ImageData imageData;
    
    private SegmentationPanel segmentationPanel;
    private DisplayPanel displayPanel;
    private ViewsPanel viewsPanel;
    private CanvasPanel canvasPanel;
    private DefaultListModel<String> cellListModel;
    private JList<String> cellList;
    
    public CellposeFrame(ImagePlus imp) {
        super("Cellpose - Cell Segmentation Tool");
        this.imagePlus = imp;
        this.cells = new ArrayList<>();
        this.imageData = new ImageData(imp.getWidth(), imp.getHeight(), imp.getTitle());
        
        initializeUI();
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
    }

    public void setBackendUrl(String backendUrl) {
        if (segmentationPanel != null) {
            segmentationPanel.setBackendUrl(backendUrl);
        }
    }
    
    private void initializeUI() {
        // Apply theme BEFORE creating any components
        applyDarkTheme();
        
        setLayout(new BorderLayout());
        
        // Menu bar
        JMenuBar menuBar = createMenuBar();
        setJMenuBar(menuBar);
        
        // Left sidebar with controls
        JPanel sidebar = createSidebar();
        JScrollPane sidebarScroll = new JScrollPane(sidebar);
        sidebarScroll.setPreferredSize(new Dimension(320, 0));
        sidebarScroll.setBorder(BorderFactory.createEmptyBorder());
        sidebarScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sidebarScroll.getVerticalScrollBar().setUnitIncrement(16);
        add(sidebarScroll, BorderLayout.WEST);
        
        // Center canvas area
        canvasPanel = new CanvasPanel(imagePlus, cells, imageData);
        add(canvasPanel, BorderLayout.CENTER);

        // Right panel: cell list
        JPanel rightPanel = createRightPanel();
        JScrollPane rightScroll = new JScrollPane(rightPanel);
        rightScroll.setPreferredSize(new Dimension(280, 0));
        rightScroll.setBorder(BorderFactory.createEmptyBorder());
        add(rightScroll, BorderLayout.EAST);
    }
    
    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        
        JMenu fileMenu = new JMenu("File");
        JMenuItem openItem = new JMenuItem("Open");
        JMenuItem saveItem = new JMenuItem("Save");
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> dispose());
        fileMenu.add(openItem);
        fileMenu.add(saveItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        
        JMenu editMenu = new JMenu("Edit");
        JMenuItem undoItem = new JMenuItem("Undo");
        JMenuItem redoItem = new JMenuItem("Redo");
        editMenu.add(undoItem);
        editMenu.add(redoItem);
        
        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About");
        helpMenu.add(aboutItem);
        
        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(helpMenu);
        
        return menuBar;
    }
    
    private JPanel createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Segmentation section
        segmentationPanel = new SegmentationPanel(imagePlus, cells, this::updateCells);
        sidebar.add(segmentationPanel);
        sidebar.add(Box.createVerticalStrut(20));
        
        // Display section
        displayPanel = new DisplayPanel();
        sidebar.add(displayPanel);
        sidebar.add(Box.createVerticalStrut(20));
        
        // Views section
        viewsPanel = new ViewsPanel();
        sidebar.add(viewsPanel);
        sidebar.add(Box.createVerticalGlue());
        
        return sidebar;
    }
    
    private void updateCells(List<Cell> newCells) {
        IJ.log("updateCells called with " + newCells.size() + " cells");
        this.cells.clear();
        this.cells.addAll(newCells);
        IJ.log("Cells list now has " + this.cells.size() + " cells");
        canvasPanel.updateCells(cells);
        IJ.log("Cells list now has " + this.cells.size() + " cells");

        // Update the right-side cell list
        if (cellListModel != null) {
            cellListModel.clear();
            for (Cell c : newCells) {
                String item = String.format("%d: (%.1f, %.1f) r=%.1f", c.getId(), c.getX(), c.getY(), c.getRadius());
                cellListModel.addElement(item);
                IJ.log("Added to cellListModel: " + item + ", ROI=" + (c.getRoi() != null));
            }
            IJ.log("CellListModel now has " + cellListModel.size() + " items");
        }
    }

    private JPanel createRightPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel title = new JLabel("Cells");
        title.setFont(new Font("Arial", Font.BOLD, 16));
        title.setForeground(new Color(79, 195, 247));
        panel.add(title, BorderLayout.NORTH);

        cellListModel = new DefaultListModel<>();
        cellList = new JList<>(cellListModel);
        cellList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        cellList.setVisibleRowCount(10);
        
        // Add selection listener to highlight selected cell
        cellList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedIndex = cellList.getSelectedIndex();
                IJ.log("Cell selected - Index: " + selectedIndex + ", CellposeFrame.this.cells size: " + CellposeFrame.this.cells.size());
                if (selectedIndex >= 0 && selectedIndex < CellposeFrame.this.cells.size()) {
                    highlightSelectedCell(selectedIndex);
                } else {
                    IJ.log("Invalid index: " + selectedIndex + ", cells size: " + CellposeFrame.this.cells.size());
                }
            }
        });

        JScrollPane listScroll = new JScrollPane(cellList);
        panel.add(listScroll, BorderLayout.CENTER);

        return panel;
    }
    
    private void applyDarkTheme() {
        Color bgColor = new Color(30, 30, 30);
        Color fgColor = new Color(255, 255, 255);
        Color panelColor = new Color(37, 37, 37);
        
        try {
            UIManager.put("Panel.background", panelColor);
            UIManager.put("Label.foreground", fgColor);
            UIManager.put("Button.background", new Color(77, 77, 77));
            UIManager.put("Button.foreground", fgColor);
            UIManager.put("TextField.background", new Color(61, 61, 61));
            UIManager.put("TextField.foreground", fgColor);
            UIManager.put("TextField.caretForeground", fgColor);
            UIManager.put("ComboBox.background", new Color(61, 61, 61));
            UIManager.put("ComboBox.foreground", fgColor);
            UIManager.put("Spinner.background", new Color(61, 61, 61));
            UIManager.put("Spinner.foreground", fgColor);
            UIManager.put("CheckBox.background", panelColor);
            UIManager.put("CheckBox.foreground", fgColor);
            UIManager.put("List.background", new Color(61, 61, 61));
            UIManager.put("List.foreground", fgColor);
            UIManager.put("ScrollPane.background", panelColor);
            UIManager.put("MenuBar.background", new Color(45, 45, 45));
            UIManager.put("MenuBar.foreground", fgColor);
            UIManager.put("Menu.background", new Color(45, 45, 45));
            UIManager.put("Menu.foreground", fgColor);
            UIManager.put("MenuItem.background", new Color(45, 45, 45));
            UIManager.put("MenuItem.foreground", fgColor);
            
            // Force update of all component defaults
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Explicitly set frame background
        getContentPane().setBackground(bgColor);
    }
    
    public ImagePlus getImagePlus() {
        return imagePlus;
    }
    
    public List<Cell> getCells() {
        return cells;
    }
    
    private void highlightSelectedCell(int index) {
        IJ.log("highlightSelectedCell called with index: " + index);
        
        if (index < 0 || index >= cells.size()) {
            IJ.log("Index out of bounds: " + index + " (cells size: " + cells.size() + ")");
            return;
        }
        
        Cell selectedCell = cells.get(index);
        IJ.log("Selected cell ID: " + selectedCell.getId() + ", Has ROI: " + (selectedCell.getRoi() != null));
        
        if (selectedCell.getRoi() == null) {
            IJ.log("Cell " + selectedCell.getId() + " has no ROI!");
            return;
        }
        
        // Get existing overlay or create new one
        ij.gui.Overlay overlay = imagePlus.getOverlay();
        if (overlay == null) {
            IJ.log("No existing overlay, creating new one");
            overlay = new ij.gui.Overlay();
        } else {
            IJ.log("Existing overlay has " + overlay.size() + " ROIs");
        }
        
        // Clone the selected cell's ROI and set it to white with thicker stroke
        ij.gui.Roi highlightRoi = (ij.gui.Roi) selectedCell.getRoi().clone();
        highlightRoi.setStrokeColor(Color.WHITE);
        highlightRoi.setStrokeWidth(3);
        highlightRoi.setName("Highlight_" + selectedCell.getId());
        IJ.log("Created highlight ROI for cell " + selectedCell.getId());
        
        // Remove any previous highlight ROIs
        int removedCount = 0;
        for (int i = overlay.size() - 1; i >= 0; i--) {
            ij.gui.Roi roi = overlay.get(i);
            if (roi.getName() != null && roi.getName().startsWith("Highlight_")) {
                overlay.remove(i);
                removedCount++;
            }
        }
        IJ.log("Removed " + removedCount + " previous highlight(s)");
        
        // Add the new highlight on top
        overlay.add(highlightRoi);
        IJ.log("Added highlight, overlay now has " + overlay.size() + " ROIs");
        imagePlus.setOverlay(overlay);
        imagePlus.updateAndDraw();
        IJ.log("Updated image display");
    }
}

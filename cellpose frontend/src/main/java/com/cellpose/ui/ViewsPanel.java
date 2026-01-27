package com.cellpose.ui;

import javax.swing.*;
import java.awt.*;

public class ViewsPanel extends JPanel {
    private JComboBox<String> viewModeCombo;
    private JComboBox<String> imageSelectCombo;
    private JCheckBox autoAdjustCheckbox;
    private JSlider redSlider;
    private JSlider greenSlider;
    private JSlider blueSlider;
    
    public ViewsPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Title
        JLabel titleLabel = new JLabel("Views:");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setForeground(new Color(79, 195, 247));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(titleLabel);
        add(Box.createVerticalStrut(10));
        
        // View mode combo
        viewModeCombo = new JComboBox<>(new String[]{"RGB", "R / (= W/S)", "G / (↑ / ↓ toggles color ↑)"});
        viewModeCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        viewModeCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        add(viewModeCombo);
        add(Box.createVerticalStrut(5));
        
        // Image select combo
        imageSelectCombo = new JComboBox<>(new String[]{"image", "pangolin / pagedown"});
        imageSelectCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        imageSelectCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        add(imageSelectCombo);
        add(Box.createVerticalStrut(10));
        
        // Auto-adjust checkbox
        autoAdjustCheckbox = new JCheckBox("auto-adjust saturation");
        autoAdjustCheckbox.setSelected(true);
        autoAdjustCheckbox.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(autoAdjustCheckbox);
        add(Box.createVerticalStrut(10));
        
        // Gray label
        JLabel grayLabel = new JLabel("gray:");
        grayLabel.setForeground(Color.GRAY);
        grayLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(grayLabel);
        add(Box.createVerticalStrut(5));
        
        // Red slider
        add(createColorSlider("red:", Color.RED, redSlider = new JSlider(0, 100, 80)));
        
        // Green slider
        add(createColorSlider("green:", Color.GREEN, greenSlider = new JSlider(0, 100, 50)));
        
        // Blue slider
        add(createColorSlider("blue:", Color.CYAN, blueSlider = new JSlider(0, 100, 80)));
    }
    
    private JPanel createColorSlider(String labelText, Color color, JSlider slider) {
        JPanel panel = new JPanel(new BorderLayout(5, 0));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        
        JLabel label = new JLabel(labelText);
        label.setForeground(color);
        label.setPreferredSize(new Dimension(50, 25));
        
        panel.add(label, BorderLayout.WEST);
        panel.add(slider, BorderLayout.CENTER);
        
        add(Box.createVerticalStrut(5));
        return panel;
    }
    
    public String getViewMode() {
        return (String) viewModeCombo.getSelectedItem();
    }
    
    public String getImageSelect() {
        return (String) imageSelectCombo.getSelectedItem();
    }
    
    public boolean isAutoAdjust() {
        return autoAdjustCheckbox.isSelected();
    }
    
    public int getRedValue() {
        return redSlider.getValue();
    }
    
    public int getGreenValue() {
        return greenSlider.getValue();
    }
    
    public int getBlueValue() {
        return blueSlider.getValue();
    }
}

package com.cellpose.ui;

import javax.swing.*;
import java.awt.*;

public class DisplayPanel extends JPanel {
    private JSlider brightnessSlider;
    private JCheckBox showOutlinesCheckbox;
    
    public DisplayPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Title
        JLabel titleLabel = new JLabel("Display:");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setForeground(new Color(79, 195, 247));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(titleLabel);
        add(Box.createVerticalStrut(10));
        
        // Brightness slider
        JPanel brightnessPanel = new JPanel(new BorderLayout(5, 0));
        brightnessPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        brightnessPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        
        JLabel brightnessLabel = new JLabel("Brightness:");
        brightnessSlider = new JSlider(0, 200, 100);
        brightnessSlider.setMajorTickSpacing(50);
        brightnessSlider.setPaintTicks(false);
        
        brightnessPanel.add(brightnessLabel, BorderLayout.NORTH);
        brightnessPanel.add(brightnessSlider, BorderLayout.CENTER);
        add(brightnessPanel);
        
        // Show outlines checkbox
        showOutlinesCheckbox = new JCheckBox("Show outlines");
        showOutlinesCheckbox.setSelected(true);
        showOutlinesCheckbox.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(Box.createVerticalStrut(10));
        add(showOutlinesCheckbox);
    }
    
    public int getBrightness() {
        return brightnessSlider.getValue();
    }
    
    public boolean isShowOutlines() {
        return showOutlinesCheckbox.isSelected();
    }
}

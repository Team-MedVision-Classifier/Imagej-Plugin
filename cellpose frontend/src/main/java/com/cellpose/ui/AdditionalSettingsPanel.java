package com.cellpose.ui;

import javax.swing.*;
import java.awt.*;

public class AdditionalSettingsPanel extends JPanel {
    private JSpinner batchSizeSpinner;
    private JCheckBox resampleCheckBox;
    private JCheckBox normalizeCheckBox;
    private JSpinner flowThresholdSpinner;
    private JSpinner cellprobThresholdSpinner;
    
    // Normalization sub-options
    private JSpinner percentileLowSpinner;
    private JSpinner percentileHighSpinner;
    private JSpinner tileNormSpinner;

    public AdditionalSettingsPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setAlignmentX(Component.LEFT_ALIGNMENT);
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(79, 195, 247), 1),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        initializeComponents();
    }

    private void initializeComponents() {
        // Batch Size
        batchSizeSpinner = new JSpinner(new SpinnerNumberModel(64, 1, 1000, 1));
        add(createLabeledSpinner("Batch Size:", batchSizeSpinner));

        // Resample
        resampleCheckBox = new JCheckBox();
        resampleCheckBox.setSelected(false);
        add(createLabeledCheckBox("Resample:", resampleCheckBox));

        // Flow Threshold
        flowThresholdSpinner = new JSpinner(new SpinnerNumberModel(0.4, 0.0, 10.0, 0.1));
        add(createLabeledSpinner("Flow Threshold:", flowThresholdSpinner));

        // Cellprob Threshold
        cellprobThresholdSpinner = new JSpinner(new SpinnerNumberModel(0.0, -10.0, 10.0, 0.1));
        add(createLabeledSpinner("Cellprob Threshold:", cellprobThresholdSpinner));
        
        // Normalize
        normalizeCheckBox = new JCheckBox();
        normalizeCheckBox.setSelected(true);
        add(createLabeledCheckBox("Normalize:", normalizeCheckBox));
        
        // Normalization sub-options (indented to show they're related to Normalize)
        percentileLowSpinner = new JSpinner(new SpinnerNumberModel(1.0, 0.0, 100.0, 0.1));
        add(createLabeledSpinner("  Percentile Low:", percentileLowSpinner));
        
        percentileHighSpinner = new JSpinner(new SpinnerNumberModel(99.0, 0.0, 100.0, 0.1));
        add(createLabeledSpinner("  Percentile High:", percentileHighSpinner));
        
        tileNormSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 1000, 10));
        add(createLabeledSpinner("  Tile Norm:", tileNormSpinner));
        
        // Add listener to enable/disable normalization sub-options
        normalizeCheckBox.addActionListener(e -> {
            boolean enabled = normalizeCheckBox.isSelected();
            percentileLowSpinner.setEnabled(enabled);
            percentileHighSpinner.setEnabled(enabled);
            tileNormSpinner.setEnabled(enabled);
        });
    }

    private JPanel createLabeledSpinner(String labelText, JSpinner spinner) {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(5, 0));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        JLabel label = new JLabel(labelText);
        label.setPreferredSize(new Dimension(120, 25));
        panel.add(label, BorderLayout.WEST);
        panel.add(spinner, BorderLayout.CENTER);

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

        return panel;
    }

    // Getters for all settings
    public int getBatchSize() {
        return (Integer) batchSizeSpinner.getValue();
    }

    public boolean isResample() {
        return resampleCheckBox.isSelected();
    }

    public boolean isNormalize() {
        return normalizeCheckBox.isSelected();
    }

    public double getFlowThreshold() {
        return (Double) flowThresholdSpinner.getValue();
    }

    public double getCellprobThreshold() {
        return (Double) cellprobThresholdSpinner.getValue();
    }

    public double getPercentileLow() {
        return (Double) percentileLowSpinner.getValue();
    }

    public double getPercentileHigh() {
        return (Double) percentileHighSpinner.getValue();
    }

    public int getTileNorm() {
        return (Integer) tileNormSpinner.getValue();
    }
}

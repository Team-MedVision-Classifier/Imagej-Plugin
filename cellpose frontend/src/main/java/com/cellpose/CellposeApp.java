package com.cellpose;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;
import com.cellpose.backend.BackendManager;
import com.cellpose.ui.CellposeFrame;

import javax.swing.SwingUtilities;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class CellposeApp implements PlugIn {

    @Override
    public void run(String arg) {
        // Get the current image from ImageJ
        ImagePlus imp = IJ.getImage();
        
        // if (imp == null) {
        //     IJ.error("Cellpose", "No image is open. Please open an image first.");
        //     return;
        // }
        
        BackendManager backendManager = new BackendManager();
        String backendUrl = null;
        try {
            backendUrl = backendManager.start();
        } catch (Exception e) {
            IJ.error("Cellpose", "Failed to start bundled backend: " + e.getMessage());
        }

        String finalBackendUrl = backendUrl;

        // Launch the Cellpose UI on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            CellposeFrame frame = new CellposeFrame(imp);
            if (finalBackendUrl != null && !finalBackendUrl.trim().isEmpty()) {
                frame.setBackendUrl(finalBackendUrl);
            }
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    backendManager.stop();
                }
            });
            frame.setVisible(true);
        });
    }
}

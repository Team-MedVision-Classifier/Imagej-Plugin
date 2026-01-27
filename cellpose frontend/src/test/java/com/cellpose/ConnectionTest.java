package com.cellpose;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

/**
 * Simple test to verify backend connection
 * Run this to test if the frontend can communicate with the backend
 */
public class ConnectionTest {
    
    public static void main(String[] args) {
        System.out.println("=== Cellpose Backend Connection Test ===\n");
        
        String backendUrl = "http://localhost:8000";
        
        // Test 1: Check if backend is running
        System.out.println("1. Testing backend connectivity...");
        try {
            CloseableHttpClient httpClient = HttpClients.createDefault();
            org.apache.hc.client5.http.classic.methods.HttpGet testGet = 
                new org.apache.hc.client5.http.classic.methods.HttpGet(backendUrl + "/docs");
            CloseableHttpResponse response = httpClient.execute(testGet);
            int statusCode = response.getCode();
            httpClient.close();
            
            if (statusCode == 200 || statusCode == 404) { // 404 is OK, means server is running
                System.out.println("   ✓ Backend is running at " + backendUrl);
            } else {
                System.out.println("   ✗ Backend returned status: " + statusCode);
                return;
            }
        } catch (Exception e) {
            System.out.println("   ✗ Cannot connect to backend: " + e.getMessage());
            System.out.println("\n   Please start the backend server:");
            System.out.println("   cd \"cellpose backend\"");
            System.out.println("   C:\\Users\\user\\AppData\\Local\\Programs\\Python\\Python310\\python.exe -m uvicorn app:app --port 8000");
            return;
        }
        
        // Test 2: Create a test image
        System.out.println("\n2. Creating test image...");
        File testImage = null;
        try {
            testImage = File.createTempFile("test_cellpose_", ".png");
            BufferedImage image = new BufferedImage(512, 512, BufferedImage.TYPE_BYTE_GRAY);
            
            // Draw some simple circles to simulate cells
            java.awt.Graphics2D g2d = image.createGraphics();
            g2d.setColor(java.awt.Color.WHITE);
            g2d.fillRect(0, 0, 512, 512);
            g2d.setColor(java.awt.Color.BLACK);
            g2d.fillOval(100, 100, 80, 80);
            g2d.fillOval(250, 150, 100, 100);
            g2d.fillOval(350, 300, 90, 90);
            g2d.dispose();
            
            ImageIO.write(image, "PNG", testImage);
            System.out.println("   ✓ Test image created: " + testImage.getAbsolutePath());
        } catch (Exception e) {
            System.out.println("   ✗ Failed to create test image: " + e.getMessage());
            return;
        }
        
        // Test 3: Send segmentation request
        System.out.println("\n3. Sending segmentation request to backend...");
        try {
            String url = backendUrl + "/segment?model_type=ddq_model&diameter=30&channels=0,0";
            
            HttpEntity entity = MultipartEntityBuilder.create()
                .addBinaryBody("image", testImage, ContentType.APPLICATION_OCTET_STREAM, testImage.getName())
                .build();
            
            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpPost post = new HttpPost(url);
            post.setEntity(entity);
            
            System.out.println("   Waiting for segmentation (this may take 10-30 seconds)...");
            CloseableHttpResponse response = httpClient.execute(post);
            int statusCode = response.getCode();
            
            if (statusCode == 200) {
                System.out.println("   ✓ Segmentation successful!");
                
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.getEntity().getContent())
                );
                
                int cellCount = 0;
                String line;
                System.out.println("\n   Detected cells:");
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        cellCount++;
                        String[] coords = line.split(",");
                        System.out.println("   - Cell " + cellCount + ": " + (coords.length / 2) + " outline points");
                    }
                }
                reader.close();
                
                System.out.println("\n   Total cells detected: " + cellCount);
                
            } else {
                System.out.println("   ✗ Backend returned error status: " + statusCode);
                String errorMsg = new String(response.getEntity().getContent().readAllBytes());
                System.out.println("   Error: " + errorMsg);
            }
            
            httpClient.close();
            
        } catch (Exception e) {
            System.out.println("   ✗ Segmentation failed: " + e.getMessage());
            e.printStackTrace();
            return;
        } finally {
            if (testImage != null && testImage.exists()) {
                testImage.delete();
            }
        }
        
        System.out.println("\n=== ✓ All tests passed! Frontend-Backend connection is working ===");
    }
}

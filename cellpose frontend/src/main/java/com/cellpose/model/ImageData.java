package com.cellpose.model;

public class ImageData {
    private int width;
    private int height;
    private String name;

    public ImageData(int width, int height, String name) {
        this.width = width;
        this.height = height;
        this.name = name;
    }

    // Getters
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public String getName() { return name; }

    // Setters
    public void setWidth(int width) { this.width = width; }
    public void setHeight(int height) { this.height = height; }
    public void setName(String name) { this.name = name; }
}

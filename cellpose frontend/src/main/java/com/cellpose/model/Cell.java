package com.cellpose.model;

import ij.gui.Roi;
import java.awt.Color;

public class Cell {
    private int id;
    private double x;
    private double y;
    private double radius;
    private Color color;
    private double intensity;
    private Roi roi;

    public Cell(int id, double x, double y, double radius, Color color, double intensity) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.color = color;
        this.intensity = intensity;
    }

    // Getters
    public int getId() { return id; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getRadius() { return radius; }
    public Color getColor() { return color; }
    public double getIntensity() { return intensity; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setX(double x) { this.x = x; }
    public void setY(double y) { this.y = y; }
    public void setRadius(double radius) { this.radius = radius; }
    public void setColor(Color color) { this.color = color; }
    public void setIntensity(double intensity) { this.intensity = intensity; }
    
    public Roi getRoi() { return roi; }
    public void setRoi(Roi roi) { this.roi = roi; }
}

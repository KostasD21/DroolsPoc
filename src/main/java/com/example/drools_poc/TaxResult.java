package com.example.drools_poc;

public class TaxResult {
    private String port;
    private String direction;
    private double taxRate;

    public TaxResult(String port, String direction, double taxRate) {
        this.port = port;
        this.direction = direction;
        this.taxRate = taxRate;
    }

    public String getPort() { return port; }
    public String getDirection() { return direction; }
    public double getTaxRate() { return taxRate; }

    @Override
    public String toString() {
        return "TaxResult{" +
                "port='" + port + '\'' +
                ", direction='" + direction + '\'' +
                ", taxRate=" + taxRate +
                '}';
    }
}

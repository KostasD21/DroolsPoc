package com.example.drools_poc;

public class Reservation {
    private String port;
    private String direction; // "INBOUND" or "OUTBOUND"

    public Reservation(String port, String direction) {
        this.port = port;
        this.direction = direction;
    }

    public String getPort() { return port; }
    public String getDirection() { return direction; }
}


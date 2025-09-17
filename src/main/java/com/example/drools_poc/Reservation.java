package com.example.drools_poc;

import java.time.LocalDate;

public class Reservation {
    private String port;
    private String direction; // "INBOUND" or "OUTBOUND"
    private LocalDate travelDate;

    public Reservation(String port, String direction, LocalDate travelDate) {
        this.port = port;
        this.direction = direction;
        this.travelDate = travelDate;
    }

    public Reservation(String port, LocalDate travelDate) {
        this.port = port;
        this.travelDate = travelDate;
    }

    public String getPort() { return port; }
    public String getDirection() { return direction; }
    public LocalDate getTravelDate() { return travelDate; }
}



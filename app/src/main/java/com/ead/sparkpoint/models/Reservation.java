package com.ead.sparkpoint.models;

public class Reservation {
    private String id;
    private String stationId;
    private String stationName;
    private String reservationTime;
    private String reservationSlot;
    private int slotsRequested;
    private String status;

    //Constructor
    public Reservation(String id, String stationId, String stationName, String reservationTime, String reservationSlot, int slotsRequested, String status ) {
        this.id = id;
        this.stationId = stationId;
        this.stationName = stationName;
        this.reservationTime = reservationTime;
        this.reservationSlot = reservationSlot;
        this.slotsRequested = slotsRequested;
        this.status = status;
    }

    //Getters and Setters
    public String getId() { return id;}
    public String getStationId() { return stationId; }
    public String getStationName() { return stationName; }
    public String getReservationTime() { return reservationTime; }
    public String getReservationSlot() { return reservationSlot; }
    public int getSlotsRequested() { return slotsRequested; }
    public String getStatus() { return status; }

}

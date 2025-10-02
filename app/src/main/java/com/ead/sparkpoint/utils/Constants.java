package com.ead.sparkpoint.utils;

public class Constants {
    public static final String BASE_URL = "http://100.114.75.113/SparkPoint/api";
    public static final String GET_BOOKINGS_URL = "/bookings";
    public static final String UPCOMING_BOOKINGS_URL = "/bookings?status=Confirmed";
    public static final String PAST_BOOKINGS_URL = "/bookings?status=Completed";
    public static final String CREATE_BOOKINGS_URL = "/bookings";
    public static final String UPDATE_BOOKINGS_URL = "/bookings/{bookingid}";
    public static final String DELETE_BOOKINGS_URL = "/bookings/cancel/{bookingid}";
    public static final String GET_NEARBY_STATIONS_URL = "/stations?isActive=true&nearLoaction.longitude=80.047234&nearLoaction.latitude=6.902995";
}


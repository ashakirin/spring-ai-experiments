package org.devoxx.mcp.trip.hotel;

public record HotelBookingResult(
    String confirmation,
    String hotel,
    String guest,
    String city,
    String checkIn,
    String checkOut
) {}
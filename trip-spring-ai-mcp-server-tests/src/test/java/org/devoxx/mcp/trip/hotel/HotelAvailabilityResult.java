package org.devoxx.mcp.trip.hotel;

public record HotelAvailabilityResult(
    String city,
    String checkIn,
    String checkOut,
    String hotels
) {}
package org.devoxx.mcp.trip.hotel;

public record HotelAvailabilityRequest(
    String city,
    String checkInDate,
    String checkOutDate
) {}
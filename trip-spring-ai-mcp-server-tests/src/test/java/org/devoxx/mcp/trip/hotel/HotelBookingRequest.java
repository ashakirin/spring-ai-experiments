package org.devoxx.mcp.trip.hotel;

public record HotelBookingRequest(
    String city,
    String hotelName,
    String guestName,
    String checkInDate,
    String checkOutDate
) {}
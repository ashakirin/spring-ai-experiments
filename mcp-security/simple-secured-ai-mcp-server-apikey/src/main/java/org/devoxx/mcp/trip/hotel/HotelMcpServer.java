package org.devoxx.mcp.trip.hotel;

import org.springaicommunity.mcp.annotation.McpTool;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class HotelMcpServer {

    private final HotelService hotelService;

    public HotelMcpServer(HotelService hotelService) {
        this.hotelService = hotelService;
    }

    @PreAuthorize("isAuthenticated()")
    @McpTool(name = "getAvailability", description = "Get available hotels in a city for given dates")
    public Map<String, Object> getAvailability(String city, String checkInDate, String checkOutDate) {
        return hotelService.getAvailability(city, checkInDate, checkOutDate);
    }

    @PreAuthorize("isAuthenticated()")
    @McpTool(name = "bookHotel", description = "Book a hotel by name, city, and guest info")
    public Map<String, Object> bookHotel(String city, String hotelName, String guestName,
                                         String checkInDate, String checkOutDate) {
        return hotelService.bookHotel(city, hotelName, guestName, checkInDate, checkOutDate);
    }
}
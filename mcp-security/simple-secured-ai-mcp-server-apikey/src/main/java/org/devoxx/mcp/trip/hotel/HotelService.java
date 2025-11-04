package org.devoxx.mcp.trip.hotel;

import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class HotelService {

    public Map<String, Object> getAvailability(String city, String checkInDate, String checkOutDate) {
        return Map.of(
            "city", city,
            "checkIn", checkInDate,
            "checkOut", checkOutDate,
            "hotels", "Hotel A, Hotel B"
        );
    }

    public Map<String, Object> bookHotel(String city, String hotelName, String guestName, 
                                        String checkInDate, String checkOutDate) {
        return Map.of(
            "confirmation", "CONF" + System.currentTimeMillis() % 10000,
            "hotel", hotelName,
            "guest", guestName,
            "city", city,
            "checkIn", checkInDate,
            "checkOut", checkOutDate
        );
    }
}
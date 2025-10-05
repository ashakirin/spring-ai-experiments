package org.devoxx.trip.agent;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("api")
public class ChatController {

    private final TripReservationAiService tripReservationAiService;

    public ChatController(TripReservationAiService tripReservationAiService) {
        this.tripReservationAiService = tripReservationAiService;
    }

    @PostMapping("chat")
    public String chat(@RequestBody PromptRequest promptRequest) {
        return tripReservationAiService.chat(promptRequest.prompt());
    }

    @PostMapping("/chat/stream")
    public Flux<String> chatStream(@RequestBody PromptRequest promptRequest) {
        return tripReservationAiService.chatStream(promptRequest.prompt());
    }

    record PromptRequest(String prompt) {
    }
}
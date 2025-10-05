package org.devoxx.trip.agent;

import org.springframework.ai.document.Document;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequestMapping("api")
public class ChatController {

    private final TripReservationAiService tripReservationAiService;

    public ChatController(TripReservationAiService unicornAiService) {
        this.tripReservationAiService = unicornAiService;
    }

    @PostMapping("chat")
    public String chat(@RequestBody PromptRequest promptRequest) {
        return tripReservationAiService.chat(promptRequest.prompt());
    }

    @PostMapping("/chat/stream")
    public Flux<String> chatStream(@RequestBody PromptRequest promptRequest) {
        return tripReservationAiService.chatStream(promptRequest.prompt());
    }

    @PostMapping("/load")
    public void loadDataToVectorStore(@RequestBody String content) {
        tripReservationAiService.loadDataToVectorStore(content);
    }

    record PromptRequest(String prompt) {
    }
}
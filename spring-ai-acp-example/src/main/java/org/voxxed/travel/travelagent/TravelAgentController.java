package org.voxxed.travel.travelagent;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
public class TravelAgentController {
    private final TravelAgent travelAgent;

    public TravelAgentController(TravelAgent travelAgent) {
        this.travelAgent = travelAgent;
    }

    @PostMapping("/chat")
    public String chat(@RequestBody String prompt) {
        return travelAgent.chat(prompt);
    }

    @PostMapping("/chat-stream")
    public Flux<String> chatStream(@RequestBody String prompt) {
        return travelAgent.chatStream(prompt);
    }
}

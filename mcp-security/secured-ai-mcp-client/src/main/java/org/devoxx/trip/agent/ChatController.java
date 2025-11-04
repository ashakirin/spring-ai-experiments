package org.devoxx.trip.agent;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.stream.Collectors;

@RestController
@RequestMapping("api")
public class ChatController {

    private final TripReservationAiService tripReservationAiService;

    public ChatController(TripReservationAiService tripReservationAiService) {
        this.tripReservationAiService = tripReservationAiService;
    }

    @GetMapping("chat")
    public String chat() {
		String prompt = "Please give me all available hotels in Paris, Checkin 10.10.2025, checkout 15.10.2025";
        String chatResponse = tripReservationAiService.chat(prompt);

        String currentHotel = """
					<h2>Available hotels %s</h2>
					<p>%s</p>
					<form action="" method="GET">
					<button type="submit">Clear</button>
					</form>
					""".formatted(prompt, chatResponse);

        return """
				<h1>Demo controller</h1>
				%s

				<hr>

				<h2>Ask for the weather</h2>
				<p>In which city would you like to see the weather?</p>
				<form action="" method="GET">
				    <input type="text" name="query" value="" placeholder="Paris" />
				    <button type="submit">Ask the LLM</button>
				</form>

				<hr>
				""".formatted(currentHotel);
    }

    @PostMapping("/chat/stream")
    public Flux<String> chatStream(@RequestBody PromptRequest promptRequest) {
        return tripReservationAiService.chatStream(promptRequest.prompt());
    }

    record PromptRequest(String prompt) {
    }
}
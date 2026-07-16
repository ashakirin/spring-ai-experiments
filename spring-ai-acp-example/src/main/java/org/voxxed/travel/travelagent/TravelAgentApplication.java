package org.voxxed.travel.travelagent;

import java.util.Arrays;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TravelAgentApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(TravelAgentApplication.class);

        // When launched as an ACP stdio agent (e.g. by an editor), run without a web
        // server and keep stdout reserved for the JSON-RPC protocol stream.
        if (Arrays.asList(args).contains("--acp")) {
            app.setWebApplicationType(WebApplicationType.NONE);
            app.setBannerMode(Banner.Mode.OFF);
            app.setAdditionalProfiles("acp");
        }

        app.run(args);
    }

}

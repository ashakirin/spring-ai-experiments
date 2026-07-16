package org.voxxed.travel.travelagent.acp;

import java.io.PrintStream;

import com.agentclientprotocol.sdk.agent.support.AcpAgentSupport;
import com.agentclientprotocol.sdk.agent.transport.StdioAcpAgentTransport;
import com.agentclientprotocol.sdk.json.AcpJsonMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import org.voxxed.travel.travelagent.TravelAgent;

/**
 * Runs the {@link TravelAcpAgent} over the ACP stdio transport when the {@code acp}
 * profile is active.
 *
 * <p>ACP stdio agents speak newline-delimited JSON-RPC on {@code stdout} and read from
 * {@code stdin}. Anything else written to {@code stdout} corrupts the protocol stream, so
 * this runner captures the real {@code stdout} for the transport and redirects
 * {@code System.out} to {@code stderr} for the rest of the JVM (stray prints, banners,
 * console logging).
 *
 * <p>This runner blocks until the client disconnects, which keeps the (web-less) Spring
 * application alive for the lifetime of the ACP connection.
 */
@Component
@Profile("acp")
public class AcpStdioRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AcpStdioRunner.class);

    private final TravelAgent travelAgent;

    public AcpStdioRunner(TravelAgent travelAgent) {
        this.travelAgent = travelAgent;
    }

    @Override
    public void run(String... args) {
        // Capture the genuine stdout for the protocol, then divert everything else to stderr.
        PrintStream protocolOut = System.out;
        System.setOut(System.err);

        log.info("Starting ACP stdio agent");

        var transport = new StdioAcpAgentTransport(AcpJsonMapper.createDefault(), System.in, protocolOut);

        AcpAgentSupport.create(new TravelAcpAgent(travelAgent))
                .transport(transport)
                .build()
                .run(); // blocks until the client disconnects
    }
}

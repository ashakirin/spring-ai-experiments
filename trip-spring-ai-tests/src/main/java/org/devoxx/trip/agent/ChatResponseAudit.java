package org.devoxx.trip.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ChatResponseAudit {
    private final Logger logger = LoggerFactory.getLogger(ChatResponseAudit.class);

    public void logAuditRecord(String prompt, String response) {
        logger.info("Chat record: prompt <{}> : <{}>", prompt, response);
    }
}

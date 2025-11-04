package org.devoxx.trip.agent;

import io.modelcontextprotocol.client.transport.customizer.McpSyncHttpClientRequestCustomizer;
import org.springaicommunity.mcp.security.client.sync.AuthenticationMcpTransportContextProvider;
import org.springaicommunity.mcp.security.client.sync.oauth2.http.client.OAuth2AuthorizationCodeSyncHttpRequestCustomizer;
import org.springframework.ai.mcp.customizer.McpSyncClientCustomizer;
import org.springframework.ai.model.tool.autoconfigure.ToolCallingAutoConfiguration;
import org.springframework.ai.tool.resolution.StaticToolCallbackResolver;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

import java.util.List;

@Configuration
class McpConfiguration {

    /**
     * If the default {@link ToolCallbackResolver} from
     * {@link ToolCallingAutoConfiguration} is imported, then all MCP-based tools are
     * added to the resolver. In order to do so, the {@link ToolCallbackResolver} bean
     * lists all MCP tools, therefore initializing MCP clients and listing the tools.
     * <p>
     * This is an issue when the MCP server is secured with OAuth2, because to obtain a
     * token, a user must be involved in the flow, and there is no user present on app
     * startup.
     * <p>
     * To avoid this issue, we must exclude the default {@link ToolCallbackResolver}. We
     * can't easily disable the entire {@link ToolCallingAutoConfiguration} class, because
     * it is imported directly by the chat model configurations, such as
     * {@link AnthropicChatAutoConfiguration}. Instead, we provide a default, no-op bean.
     */
    @Bean
    ToolCallbackResolver resolver() {
        return new StaticToolCallbackResolver(List.of());
    }

    @Bean
    McpSyncHttpClientRequestCustomizer requestCustomizer(OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager,
                                                         ClientRegistrationRepository clientRegistrationRepository) {
        var registrationId = findUniqueClientRegistration(clientRegistrationRepository);
        return new OAuth2AuthorizationCodeSyncHttpRequestCustomizer(oAuth2AuthorizedClientManager, registrationId);
    }

    @Bean
    McpSyncClientCustomizer syncClientCustomizer() {
        return (name, syncSpec) -> syncSpec.transportContextProvider(new AuthenticationMcpTransportContextProvider());
    }

    /**
     * Returns the ID of the {@code spring.security.oauth2.client.registration}, if
     * unique.
     */
    private static String findUniqueClientRegistration(ClientRegistrationRepository clientRegistrationRepository) {
        String registrationId;
        if (!(clientRegistrationRepository instanceof InMemoryClientRegistrationRepository repo)) {
            throw new IllegalStateException("Expected an InMemoryClientRegistrationRepository");
        }
        var iterator = repo.iterator();
        var firstRegistration = iterator.next();
        if (iterator.hasNext()) {
            throw new IllegalStateException("Expected a single Client Registration");
        }
        registrationId = firstRegistration.getRegistrationId();
        return registrationId;
    }

}
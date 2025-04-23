package com.commercetools.ordersfetcher.config;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.defaultconfig.ApiRootBuilder;
import com.commercetools.api.defaultconfig.ServiceRegion;
import io.vrap.rmf.base.client.oauth2.ClientCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CommerceToolsConfig {

    @Value("${commercetools.project-key}")
    private String projectKey;

    @Value("${commercetools.client-id}")
    private String clientId;

    @Value("${commercetools.client-secret}")
    private String clientSecret;

    @Value("${commercetools.region}")
    private String region;

    @Bean
    public ProjectApiRoot createApiClient() {
        // Create the Commerce Tools client
        return ApiRootBuilder.of()
                .defaultClient(
                        ClientCredentials.of()
                                .withClientId(clientId)
                                .withClientSecret(clientSecret)
                                .build(),
                        getServiceRegion())
                .build(projectKey);
    }
    
    private ServiceRegion getServiceRegion() {
        try {
            return ServiceRegion.valueOf(region.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Default to Europe if region is invalid
            return ServiceRegion.GCP_EUROPE_WEST1;
        }
    }
}

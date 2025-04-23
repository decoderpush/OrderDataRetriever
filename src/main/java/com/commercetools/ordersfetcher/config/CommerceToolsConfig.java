package com.commercetools.ordersfetcher.config;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.defaultconfig.ApiRootBuilder;
import com.commercetools.api.defaultconfig.ServiceRegion;
import com.commercetools.ordersfetcher.mock.MockProjectApiRoot;
import io.vrap.rmf.base.client.oauth2.ClientCredentials;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import lombok.extern.slf4j.Slf4j;

@Configuration
public class CommerceToolsConfig {

    private static final Logger logger = Logger.getLogger(CommerceToolsConfig.class.getName());
    
    @Value("${commercetools.project-key:demo-project}")
    private String projectKey;
    
    @Value("${commercetools.client-id:demo-client}")
    private String clientId;
    
    @Value("${commercetools.client-secret:demo-secret}")
    private String clientSecret;
    
    @Value("${commercetools.region:GCP_EUROPE_WEST1}")
    private String region;
    
    @Value("${commercetools.mock-mode:true}")
    private boolean mockMode;

    @Bean
    public ProjectApiRoot createApiClient() {
        if (mockMode) {
            logger.info("Using mock Commerce Tools API client");
            return new MockProjectApiRoot();
        }
        
        logger.info("Connecting to Commerce Tools API with project key: " + projectKey);
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
            return ServiceRegion.valueOf(region);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid region: " + region + ". Valid values are: " + 
                    String.join(", ", java.util.Arrays.stream(ServiceRegion.values())
                            .map(Enum::name)
                            .toArray(String[]::new)));
        }
    }
}
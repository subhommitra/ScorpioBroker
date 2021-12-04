package eu.neclab.ngsildbroker.subscriptionmanager.config;

import org.keycloak.adapters.KeycloakConfigResolver;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.adapters.OIDCHttpFacade;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringBootKeycloakConfigResolver implements KeycloakConfigResolver {

    private KeycloakDeployment keycloakDeployment;

    private AdapterConfig adapterConfig;

    @Autowired
    public SpringBootKeycloakConfigResolver(AdapterConfig adapterConfig) {
        this.adapterConfig = adapterConfig;
    }

    @Override
    public KeycloakDeployment resolve(OIDCHttpFacade.Request request) {
        if (keycloakDeployment != null) {
            return keycloakDeployment;
        }

        keycloakDeployment = KeycloakDeploymentBuilder.build(adapterConfig);

        return keycloakDeployment;
    }
}

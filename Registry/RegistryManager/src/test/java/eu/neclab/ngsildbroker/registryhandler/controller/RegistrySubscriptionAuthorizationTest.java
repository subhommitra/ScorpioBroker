package eu.neclab.ngsildbroker.registryhandler.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import eu.neclab.ngsildbroker.commons.constants.AppConstants;
import eu.neclab.ngsildbroker.registryhandler.service.CSourceSubscriptionService;

//Imports added for Keycloak Authorization Testing
import com.c4_soft.springaddons.security.oauth2.test.annotations.keycloak.*;
import com.c4_soft.springaddons.security.oauth2.test.mockmvc.keycloak.ServletKeycloakAuthUnitTestingSupport;
import eu.neclab.ngsildbroker.registryhandler.config.KeycloakSecurityConfig;
import eu.neclab.ngsildbroker.registryhandler.config.SpringBootKeycloakConfigResolver;
import org.springframework.context.annotation.Import;

//This class holds RBAC test cases for NGSI-LD APIs
@SpringBootTest(properties = { "spring.main.allow-bean-definition-overriding=true" })
@RunWith(SpringRunner.class)
@AutoConfigureMockMvc
@Import({ ServletKeycloakAuthUnitTestingSupport.UnitTestConfig.class,
		KeycloakSecurityConfig.class, SpringBootKeycloakConfigResolver.class })

public class RegistrySubscriptionAuthorizationTest {

	@Autowired
	private MockMvc mockMvc;
	@MockBean
	CSourceSubscriptionService csourceSubsService;

	private String payload;
	@SuppressWarnings("unused")
	private String updatePayload;

	@Before
	public void setup() {

		// @formatter:off

		payload = "{\r\n" + " \"id\": \"urn:ngsi-ld:Subscription:7\",\r\n" + " \"type\": \"Subscription\",\r\n"
				+ " \"entities\": [{\r\n" + "  \"type\": \"Vehicle\"\r\n" + " }],\r\n"
				+ " \"watchedAttributes\": [\"http://example.org/vehicle/brandName\"],\r\n"
				+ "        \"q\":\"http://example.org/vehicle/brandName!=Mercedes\",\r\n" + " \"notification\": {\r\n"
				+ "  \"attributes\": [\"http://example.org/vehicle/brandName\"],\r\n"
				+ "  \"format\": \"keyValues\",\r\n" + "  \"endpoint\": {\r\n"
				+ "   \"uri\": \"http://my.endpoint.org/notify\",\r\n" + "   \"accept\": \"application/json\"\r\n"
				+ "  }\r\n" + " }\r\n" + "}";

		updatePayload = "{\r\n" + "	\"id\": \"urn:ngsi-ld:Subscription:7\",\r\n" + "	\"type\": \"Subscription\",\r\n"
				+ "	\"entities\": [{\r\n" + "		\"type\": \"Vehicle\"\r\n" + "	}],\r\n"
				+ "	\"watchedAttributes\": [\"http://example.org/vehicle/brandName2\"],\r\n"
				+ "	\"q\": \"http://example.org/vehicle/brandName2!=Mercedes\",\r\n" + "	\"notification\": {\r\n"
				+ "		\"attributes\": [\"http://example.org/vehicle/brandName2\"],\r\n"
				+ "		\"format\": \"keyValues\",\r\n" + "		\"endpoint\": {\r\n"
				+ "			\"uri\": \"http://my.endpoint.org/notify\",\r\n"
				+ "			\"accept\": \"application/json\"\r\n" + "		}\r\n" + "	}\r\n" + "}";
		// @formatter:on
	}

	@After
	public void after() {
		payload = null;
	}

	/* This method tests subscribing to a CSource with Factory-Admin RBAC role */
	@Test
	@WithMockKeycloakAuth({ "ROLE_Factory-Admin" })
	public void whenUserIsFactoryAdminThenSubscribeRestAllowed() {
		try {
			when(csourceSubsService.subscribe(any())).thenReturn(new URI("urn:ngsi-ld:Subscription:7"));
			mockMvc.perform(post("/ngsi-ld/v1/csourceSubscriptions/").contentType(AppConstants.NGB_APPLICATION_JSONLD)
					.content(payload)).andExpect(status().isCreated())
					.andExpect(redirectedUrl("/ngsi-ld/v1/csourceSubscriptions/urn:ngsi-ld:Subscription:7"))
					.andDo(print());
			verify(csourceSubsService, times(1)).subscribe(any());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	/* This method tests subscribing to a CSource with Subscriber RBAC role */
	@Test
	@WithMockKeycloakAuth({ "ROLE_Subscriber" })
	public void whenUserIsSubscriberThenSubscribeRestAllowed() {
		try {
			when(csourceSubsService.subscribe(any())).thenReturn(new URI("urn:ngsi-ld:Subscription:7"));
			mockMvc.perform(post("/ngsi-ld/v1/csourceSubscriptions/").contentType(AppConstants.NGB_APPLICATION_JSONLD)
					.content(payload)).andExpect(status().isCreated())
					.andExpect(redirectedUrl("/ngsi-ld/v1/csourceSubscriptions/urn:ngsi-ld:Subscription:7"))
					.andDo(print());
			verify(csourceSubsService, times(1)).subscribe(any());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	/* This method tests subscribing to a CSource with Reader RBAC role */
	@Test
	@WithMockKeycloakAuth({ "ROLE_Reader" })
	public void whenUserIsReaderThenSubscribeRestNotAllowed() {
		try {
			when(csourceSubsService.subscribe(any())).thenReturn(new URI("urn:ngsi-ld:Subscription:7"));
			mockMvc.perform(post("/ngsi-ld/v1/csourceSubscriptions/").contentType(AppConstants.NGB_APPLICATION_JSONLD)
					.content(payload)).andExpect(status().isForbidden());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	/*
	 * This method tests updating a subscription to a CSource with Factory-Admin
	 * RBAC role
	 */
	@Test
	@WithMockKeycloakAuth({ "ROLE_Factory-Admin" })
	public void whenUserIsFactoryAdminThenUpdateSubscriptionAllowed() {
		try {
			when(csourceSubsService.updateSubscription(any())).thenReturn(any());
			mockMvc.perform(patch("/ngsi-ld/v1/csourceSubscriptions/{id}", "urn:ngsi-ld:Subscription:7")
					.contentType(AppConstants.NGB_APPLICATION_JSONLD).content(payload))
					.andExpect(status().isNoContent()).andDo(print());
			verify(csourceSubsService, times(1)).updateSubscription(any());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	/*
	 * This method tests updating a subscription to a CSource with Subscriber RBAC
	 * role
	 */
	@Test
	@WithMockKeycloakAuth({ "ROLE_Subscriber" })
	public void whenUserIsSubscriberThenUpdateSubscriptionAllowed() {
		try {
			when(csourceSubsService.updateSubscription(any())).thenReturn(any());
			mockMvc.perform(patch("/ngsi-ld/v1/csourceSubscriptions/{id}", "urn:ngsi-ld:Subscription:7")
					.contentType(AppConstants.NGB_APPLICATION_JSONLD).content(payload))
					.andExpect(status().isNoContent()).andDo(print());
			verify(csourceSubsService, times(1)).updateSubscription(any());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	/*
	 * This method tests updating a subscription to a CSource with Reader RBAC role
	 */
	@Test
	@WithMockKeycloakAuth({ "ROLE_Reader" })
	public void whenUserIsReaderThenUpdateSubscriptionNotAllowed() {
		try {
			mockMvc.perform(patch("/ngsi-ld/v1/csourceSubscriptions/{id}", "urn:ngsi-ld:Subscription:7")
					.contentType(AppConstants.NGB_APPLICATION_JSONLD).content(payload))
					.andExpect(status().isForbidden());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	/* This method tests unsubscribing to a CSource with Factory-Admin RBAC role */
	@Test
	@WithMockKeycloakAuth({ "ROLE_Factory-Admin" })
	public void whenUserIsFactoryAdminThenUnsubscribeAllowed() {
		try {
			when(csourceSubsService.unsubscribe(any(), any())).thenReturn(true);
			mockMvc.perform(delete("/ngsi-ld/v1/csourceSubscriptions/{id}", "urn:ngsi-ld:Subscription:7")
					.contentType(AppConstants.NGB_APPLICATION_JSONLD).content(payload))
					.andExpect(status().isNoContent()).andDo(print());
			verify(csourceSubsService, times(1)).unsubscribe(any(), any());
		} catch (Exception e) {
			Assert.fail(e.getMessage());
			e.printStackTrace();
		}
	}

	/* This method tests unsubscribing to a CSource with Subscriber RBAC role */
	@Test
	@WithMockKeycloakAuth({ "ROLE_Subscriber" })
	public void whenUserIsSubscriberThenUnsubscribeAllowed() {
		try {
			when(csourceSubsService.unsubscribe(any(), any())).thenReturn(true);
			mockMvc.perform(delete("/ngsi-ld/v1/csourceSubscriptions/{id}", "urn:ngsi-ld:Subscription:7")
					.contentType(AppConstants.NGB_APPLICATION_JSONLD).content(payload))
					.andExpect(status().isNoContent()).andDo(print());
			verify(csourceSubsService, times(1)).unsubscribe(any(), any());
		} catch (Exception e) {
			Assert.fail(e.getMessage());
			e.printStackTrace();
		}
	}

	/* This method tests unsubscribing to a CSource with Reader RBAC role */
	@Test
	@WithMockKeycloakAuth({ "ROLE_Reader" })
	public void whenUserIsReaderThenUnsubscribeNotAllowed() {
		try {
			when(csourceSubsService.unsubscribe(any(), any())).thenReturn(true);
			mockMvc.perform(delete("/ngsi-ld/v1/csourceSubscriptions/{id}", "urn:ngsi-ld:Subscription:7")
					.contentType(AppConstants.NGB_APPLICATION_JSONLD).content(payload))
					.andExpect(status().isForbidden());
		} catch (Exception e) {
			Assert.fail(e.getMessage());
			e.printStackTrace();
		}
	}
}
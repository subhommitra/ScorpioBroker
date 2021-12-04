package eu.neclab.ngsildbroker.subscriptionmanager.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import com.google.common.collect.ArrayListMultimap;

import eu.neclab.ngsildbroker.commons.constants.AppConstants;
import eu.neclab.ngsildbroker.commons.datatypes.Subscription;
import eu.neclab.ngsildbroker.commons.datatypes.SubscriptionRequest;
import eu.neclab.ngsildbroker.commons.enums.ErrorType;
import eu.neclab.ngsildbroker.commons.exceptions.ResponseException;
import eu.neclab.ngsildbroker.commons.ldcontext.ContextResolverBasic;
import eu.neclab.ngsildbroker.commons.ngsiqueries.ParamsResolver;
import eu.neclab.ngsildbroker.commons.serialization.DataSerializer;
import eu.neclab.ngsildbroker.subscriptionmanager.service.SubscriptionService;

//Imports added for Keycloak Authorization Testing
import com.c4_soft.springaddons.security.oauth2.test.annotations.keycloak.*;
import com.c4_soft.springaddons.security.oauth2.test.mockmvc.keycloak.ServletKeycloakAuthUnitTestingSupport;
import eu.neclab.ngsildbroker.subscriptionmanager.config.KeycloakSecurityConfig;
import eu.neclab.ngsildbroker.subscriptionmanager.config.SpringBootKeycloakConfigResolver;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

//This class holds RBAC test cases for NGSI-LD APIs
@SpringBootTest(properties = { "spring.main.allow-bean-definition-overriding=true" })
@RunWith(SpringRunner.class)
@Import({ ServletKeycloakAuthUnitTestingSupport.UnitTestConfig.class,
		KeycloakSecurityConfig.class, SpringBootKeycloakConfigResolver.class })
@AutoConfigureMockMvc
public class SubscriptionAuthorizationTest {
	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private SubscriptionService subscriptionService;

	@InjectMocks
	private SubscriptionController subscriptionController;

	@Autowired
	ContextResolverBasic contextResolver;

	@Autowired
	ParamsResolver paramsResolver;

	private String subscriptionEntityPayload;

	@Before
	public void setup() throws Exception {
		MockitoAnnotations.initMocks(this);

		// @formatter:off

		subscriptionEntityPayload = "{" + "\r\n\"id\": \"urn:ngsi-ld:Subscription:211\","
				+ "\r\n\"type\": \"Subscription\"," + "\r\n\"entities\": [{"
				+ "\r\n		  \"id\": \"urn:ngsi-ld:Vehicle:A143\"," + "\r\n		  \"type\": \"Vehicle\""
				+ "\r\n		}]," + "\r\n\"watchedAttributes\": [\"brandName\"],"
				+ "\r\n		\"q\":\"brandName!=Mercedes\"," + "\r\n\"notification\": {"
				+ "\r\n  \"attributes\": [\"brandName\"]," + "\r\n  \"format\": \"keyValues\","
				+ "\r\n  \"endpoint\": {" + "\r\n   \"uri\": \"mqtt://localhost:1883/notify\","
				+ "\r\n   \"accept\": \"application/json\"," + "\r\n	\"notifierinfo\": {"
				+ "\r\n	  \"version\" : \"mqtt5.0\"," + "\r\n	  \"qos\" : 0" + "\r\n	}" + "\r\n  }" + "\r\n}"
				+ "\r\n}";

		// @formatter:on
	}

	@After
	public void tearDown() {
		subscriptionEntityPayload = null;
	}

	/* This method tests creating a subscription with Factory-Admin RBAC role */
	@Test
	@WithMockKeycloakAuth({ "ROLE_Factory-Admin" })
	public void whenUserIsFactoryAdminThenCreateSubscriptionAllowed() {
		try {
			URI uri = new URI("urn:ngsi-ld:Subscription:211");
			when(subscriptionService.subscribe(any())).thenReturn(uri);
			mockMvc.perform(MockMvcRequestBuilders.post("/ngsi-ld/v1/subscriptions")
					.contentType(AppConstants.NGB_APPLICATION_JSON)
					.accept(AppConstants.NGB_APPLICATION_JSONLD).content(subscriptionEntityPayload))
					.andExpect(status().isCreated());
			verify(subscriptionService, times(1)).subscribe(any());

		} catch (Exception e) {
			Assert.fail();
			e.printStackTrace();
		}
	}

	/* This method tests creating a subscription with Subscriber RBAC role */
	@Test
	@WithMockKeycloakAuth({ "ROLE_Subscriber" })
	public void whenUserIsSubscriberThenCreateSubscriptionAllowed() {
		try {
			URI uri = new URI("urn:ngsi-ld:Subscription:211");
			when(subscriptionService.subscribe(any())).thenReturn(uri);
			mockMvc.perform(MockMvcRequestBuilders.post("/ngsi-ld/v1/subscriptions")
					.contentType(AppConstants.NGB_APPLICATION_JSON)
					.accept(AppConstants.NGB_APPLICATION_JSONLD).content(subscriptionEntityPayload))
					.andExpect(status().isCreated());
			verify(subscriptionService, times(1)).subscribe(any());
		} catch (Exception e) {
			Assert.fail();
			e.printStackTrace();
		}
	}

	/* This method tests creating a subscription with Reader RBAC role */
	@Test
	@WithMockKeycloakAuth({ "ROLE_Reader" })
	public void whenUserIsReaderThenCreateSubscriptionNotAllowed() {
		try {
			URI uri = new URI("urn:ngsi-ld:Subscription:211");
			when(subscriptionService.subscribe(any())).thenReturn(uri);
			mockMvc.perform(MockMvcRequestBuilders.post("/ngsi-ld/v1/subscriptions")
					.contentType(AppConstants.NGB_APPLICATION_JSON)
					.accept(AppConstants.NGB_APPLICATION_JSONLD).content(subscriptionEntityPayload))
					.andExpect(status().isForbidden());
		} catch (Exception e) {
			Assert.fail();
			e.printStackTrace();
		}
	}

	/*
	 * This method tests retrieving a subscription by ID with Factory-Admin RBAC
	 * role
	 */
	@Test
	@WithMockKeycloakAuth({ "ROLE_Factory-Admin" })
	public void whenUserIsFactoryAdminThenGetSubscriptionByIdAllowed() {
		try {
			List<Object> context = new ArrayList<>();
			Subscription subscription = null;
			subscription = DataSerializer.getSubscription(subscriptionEntityPayload);
			SubscriptionRequest request = new SubscriptionRequest(subscription, context, ArrayListMultimap.create());
			when(subscriptionService.getSubscription(any(), any())).thenReturn(request);
			mockMvc.perform(get("/ngsi-ld/v1/subscriptions/urn:ngsi-ld:Subscription:211")
					.accept(AppConstants.NGB_APPLICATION_JSON)).andExpect(status().isOk());
			verify(subscriptionService, times(1)).getSubscription(any(), any());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*
	 * This method tests retrieving a subscription by ID with Subscriber RBAC role
	 */
	@Test
	@WithMockKeycloakAuth({ "ROLE_Subscriber" })
	public void whenUserIsSubscriberThenGetSubscriptionByIdAllowed() {
		try {
			List<Object> context = new ArrayList<>();
			Subscription subscription = null;
			subscription = DataSerializer.getSubscription(subscriptionEntityPayload);
			SubscriptionRequest request = new SubscriptionRequest(subscription, context, ArrayListMultimap.create());
			when(subscriptionService.getSubscription(any(), any())).thenReturn(request);
			mockMvc.perform(get("/ngsi-ld/v1/subscriptions/urn:ngsi-ld:Subscription:211")
					.accept(AppConstants.NGB_APPLICATION_JSON)).andExpect(status().isOk());
			verify(subscriptionService, times(1)).getSubscription(any(), any());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/* This method tests retrieving a subscription by ID with Reader RBAC role */
	@Test
	@WithMockKeycloakAuth({ "ROLE_Reader" })
	public void whenUserIsReaderThenGetSubscriptionByIdAllowed() {
		try {
			List<Object> context = new ArrayList<>();
			Subscription subscription = null;
			subscription = DataSerializer.getSubscription(subscriptionEntityPayload);
			SubscriptionRequest request = new SubscriptionRequest(subscription, context, ArrayListMultimap.create());
			when(subscriptionService.getSubscription(any(), any())).thenReturn(request);
			mockMvc.perform(get("/ngsi-ld/v1/subscriptions/urn:ngsi-ld:Subscription:211")
					.accept(AppConstants.NGB_APPLICATION_JSON)).andExpect(status().isOk());
			verify(subscriptionService, times(1)).getSubscription(any(), any());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*
	 * This method tests retrieving all subscriptions with Factory-Admin RBAC role
	 */
	@Test
	@WithMockKeycloakAuth({ "ROLE_Factory-Admin" })
	public void whenUserIsFactoryAdminThenGetAllSubscriptionsAllowed() {
		try {
			mockMvc.perform(get("/ngsi-ld/v1/subscriptions/").accept(AppConstants.NGB_APPLICATION_JSON))
					.andExpect(status().isOk());
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	/* This method tests retrieving all subscriptions with Subscriber RBAC role */
	@Test
	@WithMockKeycloakAuth({ "ROLE_Subscriber" })
	public void whenUserIsSubscriberThenGetAllSubscriptionsNotAllowed() {
		try {
			mockMvc.perform(get("/ngsi-ld/v1/subscriptions/").accept(AppConstants.NGB_APPLICATION_JSON))
					.andExpect(status().isForbidden());
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	/* This method tests retrieving all subscriptions with Reader RBAC role */
	@Test
	@WithMockKeycloakAuth({ "ROLE_Reader" })
	public void whenUserIsReaderThenGetAllSubscriptionsAllowed() {
		try {
			mockMvc.perform(get("/ngsi-ld/v1/subscriptions/").accept(AppConstants.NGB_APPLICATION_JSON))
					.andExpect(status().isOk());
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	/* This method tests deleting a subscription with Factory-Admin RBAC role */
	@Test
	@WithMockKeycloakAuth({ "ROLE_Factory-Admin" })
	public void whenUserIsFactoryAdminThenDeleteSubscriptionsAllowed() {
		try {
			mockMvc.perform(delete("/ngsi-ld/v1/subscriptions/{id}", "urn:ngsi-ld:Subscription:211")
					.contentType(AppConstants.NGB_APPLICATION_JSONLD)).andExpect(status().isNoContent());
		} catch (Exception e) {
			Assert.fail();
			e.printStackTrace();
		}
	}

	/* This method tests deleting a subscription with Subscriber RBAC role */
	@Test
	@WithMockKeycloakAuth({ "ROLE_Subscriber" })
	public void whenUserIsSubscriberThenDeleteSubscriptionsAllowed() {
		try {
			mockMvc.perform(delete("/ngsi-ld/v1/subscriptions/{id}", "urn:ngsi-ld:Subscription:211")
					.contentType(AppConstants.NGB_APPLICATION_JSONLD)).andExpect(status().isNoContent());
		} catch (Exception e) {
			Assert.fail();
			e.printStackTrace();
		}
	}

	/* This method tests deleting a subscription with Reader RBAC role */
	@Test
	@WithMockKeycloakAuth({ "ROLE_Reader" })
	public void whenUserIsReaderThenDeleteSubscriptionsAllowed() {
		try {
			mockMvc.perform(delete("/ngsi-ld/v1/subscriptions/{id}", "urn:ngsi-ld:Subscription:211")
					.contentType(AppConstants.NGB_APPLICATION_JSONLD)).andExpect(status().isForbidden());
		} catch (Exception e) {
			Assert.fail();
			e.printStackTrace();
		}
	}

	/* This method tests updating a subscription with Factory-Admin RBAC role */
	@Test
	@WithMockKeycloakAuth({ "ROLE_Factory-Admin" })
	public void whenUserIsFactoryAdminThenUpdateSubscriptionsAllowed() {
		try {
			mockMvc.perform(patch("/ngsi-ld/v1/subscriptions/urn:ngsi-ld:Subscription:211/")
					.contentType(AppConstants.NGB_APPLICATION_JSON).accept(AppConstants.NGB_APPLICATION_JSONLD)
					.content(subscriptionEntityPayload)).andExpect(status().isNoContent());
		} catch (Exception e) {
			Assert.fail();
			e.printStackTrace();
		}
	}

	/* This method tests updating a subscription with Subscriber RBAC role */
	@Test
	@WithMockKeycloakAuth({ "ROLE_Subscriber" })
	public void whenUserIsSubscriberThenUpdateSubscriptionsAllowed() {
		try {
			mockMvc.perform(patch("/ngsi-ld/v1/subscriptions/urn:ngsi-ld:Subscription:211/")
					.contentType(AppConstants.NGB_APPLICATION_JSON).accept(AppConstants.NGB_APPLICATION_JSONLD)
					.content(subscriptionEntityPayload)).andExpect(status().isNoContent());
		} catch (Exception e) {
			Assert.fail();
			e.printStackTrace();
		}
	}

	/* This method tests updating a subscription with Reader RBAC role */
	@Test
	@WithMockKeycloakAuth({ "ROLE_Reader" })
	public void whenUserIsReaderThenUpdateSubscriptionsAllowed() {
		try {
			mockMvc.perform(patch("/ngsi-ld/v1/subscriptions/urn:ngsi-ld:Subscription:211/")
					.contentType(AppConstants.NGB_APPLICATION_JSON).accept(AppConstants.NGB_APPLICATION_JSONLD)
					.content(subscriptionEntityPayload)).andExpect(status().isForbidden());
		} catch (Exception e) {
			Assert.fail();
			e.printStackTrace();
		}
	}
}
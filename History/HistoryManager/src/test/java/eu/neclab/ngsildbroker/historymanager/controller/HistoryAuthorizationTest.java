package eu.neclab.ngsildbroker.historymanager.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
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
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import eu.neclab.ngsildbroker.commons.constants.AppConstants;
import eu.neclab.ngsildbroker.commons.enums.ErrorType;
import eu.neclab.ngsildbroker.commons.exceptions.ResponseException;
import eu.neclab.ngsildbroker.commons.ldcontext.ContextResolverBasic;
import eu.neclab.ngsildbroker.commons.ngsiqueries.ParamsResolver;
import eu.neclab.ngsildbroker.historymanager.repository.HistoryDAO;
import eu.neclab.ngsildbroker.historymanager.service.HistoryService;
import eu.neclab.ngsildbroker.historymanager.utils.Validator;

//Imports added for Keycloak Authorization Testing
import com.c4_soft.springaddons.security.oauth2.test.annotations.keycloak.*;
import com.c4_soft.springaddons.security.oauth2.test.mockmvc.keycloak.ServletKeycloakAuthUnitTestingSupport;
import eu.neclab.ngsildbroker.historymanager.config.KeycloakSecurityConfig;
import eu.neclab.ngsildbroker.historymanager.config.SpringBootKeycloakConfigResolver;
import org.springframework.context.annotation.Import;

//This class holds RBAC test cases for NGSI-LD APIs
@RunWith(SpringRunner.class)
@SpringBootTest(properties = { "spring.main.allow-bean-definition-overriding=true" })
@AutoConfigureMockMvc
@Import({ ServletKeycloakAuthUnitTestingSupport.UnitTestConfig.class,
		KeycloakSecurityConfig.class, SpringBootKeycloakConfigResolver.class })
public class HistoryAuthorizationTest {
	@Autowired
	private MockMvc mockMvc;

	@Mock
	private ParamsResolver paramResolver;

	@MockBean
	private HistoryService historyService;

	@MockBean
	private Validator validate;

	@Mock
	HistoryDAO historyDAO;

	@Autowired
	ContextResolverBasic contextResolver;

	@Value("${atcontext.url}")
	String atContextServerUrl;

	private String temporalPayload;
	private URI uri;

	@Before
	public void setup() throws Exception {

		uri = new URI(AppConstants.HISTORY_URL + "urn:ngsi-ld:testunit:151");

		MockitoAnnotations.initMocks(this);

		// @formatter:on

		temporalPayload = "{\r\n    " + "\"id\": \"urn:ngsi-ld:testunit:151\","
				+ "\r\n    \"type\": \"AirQualityObserved\"," + "\r\n    \"airQualityLevel\": " + "[\r\n        {"
				+ "\r\n              " + "\r\n            "
				+ "\"type\": \"Property\",\r\n            \"value\": \"good\","
				+ "\r\n            \"observedAt\": \"2018-08-07T12:00:00Z\"" + "\r\n        }," + "\r\n        {"
				+ "\r\n               " + "\r\n            \"type\": \"Property\","
				+ "\r\n            \"value\": \"moderate\","
				+ "\r\n            \"observedAt\": \"2018-08-14T12:00:00Z\"" + "\r\n        }," + "\r\n        "
				+ "{\r\n       " + "\r\n            \"type\": \"Property\","
				+ "\r\n            \"value\": \"unhealthy\","
				+ "\r\n            \"observedAt\": \"2018-09-14T12:00:00Z\"" + "\r\n        }\r\n    ],"
				+ "\r\n    \"@context\": [" + "\r\n    \"https://uri.etsi.org/ngsi-ld/v1/ngsi-ld-core-context.jsonld\""
				+ "\r\n    ]\r\n}\r\n\r\n";

	}

	@After
	public void tearDown() {
		temporalPayload = "";
	}

	/*
	 * This method tests the creation of temporal entity with Factory-Admin RBAC
	 * role
	 */
	@Test
	@WithMockKeycloakAuth({ "ROLE_Factory-Admin" })
	public void whenUserIsFactoryAdminThenCreateTemporalEntityAllowed() {
		try {
			when(historyService.createTemporalEntityFromBinding(any(), any())).thenReturn(uri);
			mockMvc.perform(post("/ngsi-ld/v1/temporal/entities/").contentType(AppConstants.NGB_APPLICATION_JSONLD)
					.accept(AppConstants.NGB_APPLICATION_JSONLD).content(temporalPayload))
					.andExpect(status().isCreated());
			verify(historyService, times(1)).createTemporalEntityFromBinding(any(), any());
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	/*
	 * This method tests the creation of temporal entity with Factory-Editor RBAC
	 * role
	 */
	@Test
	@WithMockKeycloakAuth({ "ROLE_Factory-Editor" })
	public void whenUserIsFactoryEditorThenCreateTemporalEntityNotAllowed() {
		try {
			when(historyService.createTemporalEntityFromBinding(any(), any())).thenReturn(uri);
			mockMvc.perform(post("/ngsi-ld/v1/temporal/entities/").contentType(AppConstants.NGB_APPLICATION_JSONLD)
					.accept(AppConstants.NGB_APPLICATION_JSONLD).content(temporalPayload))
					.andExpect(status().isForbidden());
			// verify(historyService, times(1)).createTemporalEntityFromBinding(any(),
			// any());
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	/* This method tests the creation of temporal entity with Reader RBAC role */
	@Test
	@WithMockKeycloakAuth({ "ROLE_Reader" })
	public void whenUserIsReaderThenCreateTemporalEntityNotAllowed() {
		try {
			when(historyService.createTemporalEntityFromBinding(any(), any())).thenReturn(uri);
			mockMvc.perform(post("/ngsi-ld/v1/temporal/entities/").contentType(AppConstants.NGB_APPLICATION_JSONLD)
					.accept(AppConstants.NGB_APPLICATION_JSONLD).content(temporalPayload))
					.andExpect(status().isForbidden());
			// verify(historyService, times(1)).createTemporalEntityFromBinding(any(),
			// any());
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	/*
	 * This method tests the updating an attribute of temporal entity with
	 * Factory-Admin RBAC role
	 */
	@Test
	@WithMockKeycloakAuth({ "ROLE_Factory-Admin" })
	public void whenUserIsFactoryAdminThenUpdateAttrByIdAllowed() {
		try {
			mockMvc.perform(post("/ngsi-ld/v1/temporal/entities/urn:ngsi-ld:testunit:151/attrs")
					.contentType(AppConstants.NGB_APPLICATION_JSONLD).accept(AppConstants.NGB_APPLICATION_JSONLD)
					.content(temporalPayload)).andExpect(status().isNoContent());
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	/*
	 * This method tests the updating an attribute of temporal entity with
	 * Factory-Editor RBAC role
	 */
	@Test
	@WithMockKeycloakAuth({ "ROLE_Factory-Editor" })
	public void whenUserIsFactoryEditorThenUpdateAttrByIdNotAllowed() {
		try {
			mockMvc.perform(post("/ngsi-ld/v1/temporal/entities/urn:ngsi-ld:testunit:151/attrs")
					.contentType(AppConstants.NGB_APPLICATION_JSONLD).accept(AppConstants.NGB_APPLICATION_JSONLD)
					.content(temporalPayload)).andExpect(status().isForbidden());
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	/*
	 * This method tests the updating an attribute of temporal entity with Reader
	 * RBAC role
	 */
	@Test
	@WithMockKeycloakAuth({ "ROLE_Reader" })
	public void whenUserIsReaderThenUpdateAttrByIdNotAllowed() {
		try {
			mockMvc.perform(post("/ngsi-ld/v1/temporal/entities/urn:ngsi-ld:testunit:151/attrs")
					.contentType(AppConstants.NGB_APPLICATION_JSONLD).accept(AppConstants.NGB_APPLICATION_JSONLD)
					.content(temporalPayload)).andExpect(status().isForbidden());
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	/*
	 * This method tests the modification of attribute instance with Factory-Admin
	 * RBAC role
	 */
	@Test
	@WithMockKeycloakAuth({ "ROLE_Factory-Admin" })
	public void whenUserIsFactoryAdminThenModifyAttribInstanceTemporalEntityAllowed() {
		try {
			mockMvc.perform(patch("/ngsi-ld/v1/temporal/entities/{entityId}/attrs/{attrId}/{instanceId}",
					"urn:ngsi-ld:testunit:151", "airQualityLevel", "urn:ngsi-ld:d43aa0fe-a986-4479-9fac-35b7eba232041")
							.contentType(AppConstants.NGB_APPLICATION_JSON).accept(AppConstants.NGB_APPLICATION_JSONLD)
							.content(temporalPayload))
					.andExpect(status().isNoContent());
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	/*
	 * This method tests the modification of attribute instance with Factory-Editor
	 * RBAC role
	 */
	@Test
	@WithMockKeycloakAuth({ "ROLE_Factory-Editor" })
	public void whenUserIsFactoryEditorThenModifyAttribInstanceTemporalEntityAllowed() {
		try {
			mockMvc.perform(patch("/ngsi-ld/v1/temporal/entities/{entityId}/attrs/{attrId}/{instanceId}",
					"urn:ngsi-ld:testunit:151", "airQualityLevel", "urn:ngsi-ld:d43aa0fe-a986-4479-9fac-35b7eba232041")
							.contentType(AppConstants.NGB_APPLICATION_JSON).accept(AppConstants.NGB_APPLICATION_JSONLD)
							.content(temporalPayload))
					.andExpect(status().isNoContent());
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	/*
	 * This method tests the modification of attribute instance with Reader RBAC
	 * role
	 */
	@Test
	@WithMockKeycloakAuth({ "ROLE_Reader" })
	public void whenUserIsReaderThenModifyAttribInstanceTemporalEntityNotAllowed() {
		try {
			mockMvc.perform(patch("/ngsi-ld/v1/temporal/entities/{entityId}/attrs/{attrId}/{instanceId}",
					"urn:ngsi-ld:testunit:151", "airQualityLevel", "urn:ngsi-ld:d43aa0fe-a986-4479-9fac-35b7eba232041")
							.contentType(AppConstants.NGB_APPLICATION_JSON).accept(AppConstants.NGB_APPLICATION_JSONLD)
							.content(temporalPayload))
					.andExpect(status().isForbidden());
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	/*
	 * This method tests the deletion of temporal entity with Factory-Admin RBAC
	 * role
	 */
	@Test
	@WithMockKeycloakAuth({ "ROLE_Factory-Admin" })
	public void whenUserIsFactoryAdminThenDeleteTemporalEntityAllowed() {
		try {
			mockMvc.perform(MockMvcRequestBuilders
					.delete("/ngsi-ld/v1/temporal/entities/{entityId}", "urn:ngsi-ld:testunit:151")
					.contentType(AppConstants.NGB_APPLICATION_JSONLD)).andExpect(status().isNoContent());
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	/*
	 * This method tests the deletion of temporal entity with Factory-Editor RBAC
	 * role
	 */
	@Test
	@WithMockKeycloakAuth({ "ROLE_Factory-Editor" })
	public void whenUserIsFactoryEditorThenDeleteTemporalEntityNotAllowed() {
		try {
			mockMvc.perform(MockMvcRequestBuilders
					.delete("/ngsi-ld/v1/temporal/entities/{entityId}", "urn:ngsi-ld:testunit:151")
					.contentType(AppConstants.NGB_APPLICATION_JSONLD)).andExpect(status().isForbidden());
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	/* This method tests the deletion of temporal entity with Reader RBAC role */
	@Test
	@WithMockKeycloakAuth({ "ROLE_Reader" })
	public void whenUserIsReaderThenDeleteTemporalEntityNotAllowed() {
		try {
			mockMvc.perform(MockMvcRequestBuilders
					.delete("/ngsi-ld/v1/temporal/entities/{entityId}", "urn:ngsi-ld:testunit:151")
					.contentType(AppConstants.NGB_APPLICATION_JSONLD)).andExpect(status().isForbidden());
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	/*
	 * This method tests the deletion by attribute of temporal entity with
	 * Factory-Admin RBAC role
	 */
	@Test
	@WithMockKeycloakAuth({ "ROLE_Factory-Admin" })
	public void whenUserIsFactoryAdminThenDeleteTemporalEntityByAttrAllowed() {
		try {
			mockMvc.perform(
					MockMvcRequestBuilders
							.delete("/ngsi-ld/v1/temporal/entities/{entityId}/attrs/{attrId}",
									"urn:ngsi-ld:testunit:151", "airQualityLevel")
							.contentType(AppConstants.NGB_APPLICATION_JSONLD))
					.andExpect(status().isNoContent());
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	/*
	 * This method tests the deletion by attribute of temporal entity with
	 * Factory-Editor RBAC role
	 */
	@Test
	@WithMockKeycloakAuth({ "ROLE_Factory-Editor" })
	public void whenUserIsFactoryEditorThenDeleteTemporalEntityByAttrNotAllowed() {
		try {
			mockMvc.perform(
					MockMvcRequestBuilders
							.delete("/ngsi-ld/v1/temporal/entities/{entityId}/attrs/{attrId}",
									"urn:ngsi-ld:testunit:151", "airQualityLevel")
							.contentType(AppConstants.NGB_APPLICATION_JSONLD))
					.andExpect(status().isForbidden());
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	/*
	 * This method tests the deletion by attribute of temporal entity with Reader
	 * RBAC role
	 */
	@Test
	@WithMockKeycloakAuth({ "ROLE_Reader" })
	public void whenUserIsReaderThenDeleteTemporalEntityByAttrNotAllowed() {
		try {
			mockMvc.perform(
					MockMvcRequestBuilders
							.delete("/ngsi-ld/v1/temporal/entities/{entityId}/attrs/{attrId}",
									"urn:ngsi-ld:testunit:151", "airQualityLevel")
							.contentType(AppConstants.NGB_APPLICATION_JSONLD))
					.andExpect(status().isForbidden());
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
}

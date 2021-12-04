package eu.neclab.ngsildbroker.queryhandler.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import eu.neclab.ngsildbroker.commons.constants.AppConstants;
import eu.neclab.ngsildbroker.commons.datatypes.QueryResult;
import eu.neclab.ngsildbroker.commons.enums.ErrorType;
import eu.neclab.ngsildbroker.commons.exceptions.ResponseException;
import eu.neclab.ngsildbroker.commons.ldcontext.ContextResolverBasic;
import eu.neclab.ngsildbroker.commons.ngsiqueries.ParamsResolver;
import eu.neclab.ngsildbroker.queryhandler.services.QueryService;

//Imports added for Keycloak Authorization Testing
import com.c4_soft.springaddons.security.oauth2.test.annotations.keycloak.*;
import com.c4_soft.springaddons.security.oauth2.test.mockmvc.keycloak.ServletKeycloakAuthUnitTestingSupport;
import eu.neclab.ngsildbroker.queryhandler.config.KeycloakSecurityConfig;
import eu.neclab.ngsildbroker.queryhandler.config.SpringBootKeycloakConfigResolver;
import org.springframework.context.annotation.Import;

//This class holds RBAC test cases for NGSI-LD APIs
@SpringBootTest(properties = { "spring.main.allow-bean-definition-overriding=true" })
@RunWith(PowerMockRunner.class)
@AutoConfigureMockMvc
@PowerMockRunnerDelegate(SpringRunner.class)
@PowerMockIgnore({ "javax.management.*", "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "org.w3c.*",
		"com.sun.org.apache.xalan.*", "javax.activation.*", "javax.net.*", "javax.security.*" })
@SuppressWarnings("unchecked")
@Import({ServletKeycloakAuthUnitTestingSupport.UnitTestConfig.class,
    KeycloakSecurityConfig.class, SpringBootKeycloakConfigResolver.class})
public class QueryAuthorizationTest {
    @Autowired
	private MockMvc mockMvc;
	@MockBean
	private QueryService queryService;
	@Autowired
	ContextResolverBasic contextResolver;
	@Autowired
	ParamsResolver paramsResolver;

    private String entity;
	private String response = "";
	private List<String> entities;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		// @formatter:off
		entity = "[{\r\n" + "	\"http://example.org/vehicle/brandName\": [{\r\n"
				+ "		\"@type\": [\"https://uri.etsi.org/ngsi-ld/Property\"],\r\n"
				+ "		\"https://uri.etsi.org/ngsi-ld/hasValue\": [{\r\n" + "			\"@value\": \"Mercedes\"\r\n"
				+ "		}]\r\n" + "	}],\r\n" + "	\"https://uri.etsi.org/ngsi-ld/createdAt\": [{\r\n"
				+ "		\"@type\": \"https://uri.etsi.org/ngsi-ld/DateTime\",\r\n"
				+ "		\"@value\": \"2017-07-29T12:00:04Z\"\r\n" + "	}],\r\n"
				+ "	\"@id\": \"urn:ngsi-ld:Vehicle:A100\",\r\n" + "	\"http://example.org/common/isParked\": [{\r\n"
				+ "		\"https://uri.etsi.org/ngsi-ld/hasObject\": [{\r\n"
				+ "			\"@id\": \"urn:ngsi-ld:OffStreetParking:Downtown1\"\r\n" + "		}],\r\n"
				+ "		\"https://uri.etsi.org/ngsi-ld/observedAt\": [{\r\n"
				+ "			\"@type\": \"https://uri.etsi.org/ngsi-ld/DateTime\",\r\n"
				+ "			\"@value\": \"2017-07-29T12:00:04Z\"\r\n" + "		}],\r\n"
				+ "		\"http://example.org/common/providedBy\": [{\r\n"
				+ "			\"https://uri.etsi.org/ngsi-ld/hasObject\": [{\r\n"
				+ "				\"@id\": \"urn:ngsi-ld:Person:Bob\"\r\n" + "			}],\r\n"
				+ "			\"@type\": [\"https://uri.etsi.org/ngsi-ld/Relationship\"]\r\n" + "		}],\r\n"
				+ "		\"@type\": [\"https://uri.etsi.org/ngsi-ld/Relationship\"]\r\n" + "	}],\r\n"
				+ "	\"https://uri.etsi.org/ngsi-ld/location\": [{\r\n"
				+ "		\"@type\": [\"https://uri.etsi.org/ngsi-ld/GeoProperty\"],\r\n"
				+ "		\"https://uri.etsi.org/ngsi-ld/hasValue\": [{\r\n"
				+ "			\"@value\": \"{ \\\"type\\\":\\\"Point\\\", \\\"coordinates\\\":[ -8.5, 41.2 ] }\"\r\n"
				+ "		}]\r\n" + "	}],\r\n" + "	\"http://example.org/vehicle/speed\": [{\r\n"
				+ "		\"@type\": [\"https://uri.etsi.org/ngsi-ld/Property\"],\r\n"
				+ "		\"https://uri.etsi.org/ngsi-ld/hasValue\": [{\r\n" + "			\"@value\": 80\r\n"
				+ "		}]\r\n" + "	}],\r\n" + "	\"@type\": [\"http://example.org/vehicle/Vehicle\"]\r\n" + "}]";

		response = "{\r\n" + "	\"id\": \"urn:ngsi-ld:Vehicle:A100\",\r\n" + "	\"type\": \"Vehicle\",\r\n"
				+ "	\"brandName\": {\r\n" + "		\"type\": \"Property\",\r\n" + "		\"value\": \"Mercedes\"\r\n"
				+ "	},\r\n" + "	\"isParked\": {\r\n" + "		\"type\": \"Relationship\",\r\n"
				+ "		\"object\": \"urn:ngsi-ld:OffStreetParking:Downtown1\",\r\n"
				+ "		\"observedAt\": \"2017-07-29T12:00:04Z\",\r\n" + "		\"providedBy\": {\r\n"
				+ "			\"type\": \"Relationship\",\r\n" + "			\"object\": \"urn:ngsi-ld:Person:Bob\"\r\n"
				+ "		}\r\n" + "	},\r\n" + "	\"speed\": {\r\n" + "		\"type\": \"Property\",\r\n"
				+ "		\"value\": 80\r\n" + "	},\r\n" + "	\"createdAt\": \"2017-07-29T12:00:04Z\",\r\n"
				+ "	\"location\": {\r\n" + "		\"type\": \"GeoProperty\",\r\n"
				+ "		\"value\": { \"type\":\"Point\", \"coordinates\":[ -8.5, 41.2 ] }\r\n" + "	}\r\n" + "}";

		entities = new ArrayList<String>(Arrays.asList("{\r\n" + "  \"http://example.org/vehicle/brandName\" : [ {\r\n"
				+ "    \"@value\" : \"Volvo\"\r\n" + "  } ],\r\n" + "  \"@id\" : \"urn:ngsi-ld:Vehicle:A100\",\r\n"
				+ "  \"http://example.org/vehicle/speed\" : [ {\r\n"
				+ "    \"https://uri.etsi.org/ngsi-ld/instanceId\" : [ {\r\n"
				+ "      \"@value\" : \"be664aaf-a7af-4a99-bebc-e89528238abf\"\r\n" + "    } ],\r\n"
				+ "    \"https://uri.etsi.org/ngsi-ld/observedAt\" : [ {\r\n"
				+ "      \"@value\" : \"2018-06-01T12:03:00Z\",\r\n"
				+ "      \"@type\" : \"https://uri.etsi.org/ngsi-ld/DateTime\"\r\n" + "    } ],\r\n"
				+ "    \"@type\" : [ \"https://uri.etsi.org/ngsi-ld/Property\" ],\r\n"
				+ "    \"https://uri.etsi.org/ngsi-ld/hasValue\" : [ {\r\n" + "      \"@value\" : \"120\"\r\n"
				+ "    } ]\r\n" + "  }, {\r\n" + "    \"https://uri.etsi.org/ngsi-ld/instanceId\" : [ {\r\n"
				+ "      \"@value\" : \"d3ac28df-977f-4151-a432-dc088f7400d7\"\r\n" + "    } ],\r\n"
				+ "    \"https://uri.etsi.org/ngsi-ld/observedAt\" : [ {\r\n"
				+ "      \"@value\" : \"2018-08-01T12:05:00Z\",\r\n"
				+ "      \"@type\" : \"https://uri.etsi.org/ngsi-ld/DateTime\"\r\n" + "    } ],\r\n"
				+ "    \"@type\" : [ \"https://uri.etsi.org/ngsi-ld/Property\" ],\r\n"
				+ "    \"https://uri.etsi.org/ngsi-ld/hasValue\" : [ {\r\n" + "      \"@value\" : \"80\"\r\n"
				+ "    } ]\r\n" + "  } ],\r\n" + "  \"@type\" : [ \"http://example.org/vehicle/Vehicle\" ]\r\n" + "}"));
		// @formatter:on
	}

	@After
	public void tearDown() {
		entity = null;
		response = null;
	}

    /*This method tests the retrieval of entity with Factory-Admin RBAC role*/
    @Test
    @WithMockKeycloakAuth({"ROLE_Factory-Admin"})
	public void whenUserIsFactoryAdminThenGetEntityAllowed() throws Exception {	
        ResponseEntity.status(HttpStatus.OK).header("location",
			"<<http://localhost:9090/ngsi-ld/contextes/urn:ngsi-ld:Vehicle:A100>; rel=\"http://www.w3.org/ns/json-ld#context\"; type=\"application/ld+json\">")
			.body(response);

		Mockito.doReturn(entity).when(queryService).retrieveEntity(any(String.class), any(List.class),
			any(boolean.class), any(boolean.class));
		QueryResult result = new QueryResult(entities, null, ErrorType.None, -1, true);
		Mockito.doReturn(result).when(queryService).getData(any(), any(), any(), any(), any(), any(), any(), any(),any(),any());
		mockMvc.perform(get("/ngsi-ld/v1/entities/{entityId}", "urn:ngsi-ld:Vehicle:A100").accept(AppConstants.NGB_APPLICATION_JSON))
			.andExpect(status().isOk()).andExpect(jsonPath("$.id").value("urn:ngsi-ld:Vehicle:A100"));
		verify(queryService, times(1)).getData(any(), any(), any(), any(), any(), any(), any(), any(),any(),any());
	}

    /*This method tests the retrieval of entity with Factory-Editor RBAC role*/
    @Test
    @WithMockKeycloakAuth({"ROLE_Factory-Editor"})
	public void whenUserIsFactoryEditorThenGetEntityAllowed() throws Exception {	
        ResponseEntity.status(HttpStatus.OK).header("location",
			"<<http://localhost:9090/ngsi-ld/contextes/urn:ngsi-ld:Vehicle:A100>; rel=\"http://www.w3.org/ns/json-ld#context\"; type=\"application/ld+json\">")
			.body(response);

		Mockito.doReturn(entity).when(queryService).retrieveEntity(any(String.class), any(List.class),
			any(boolean.class), any(boolean.class));
		QueryResult result = new QueryResult(entities, null, ErrorType.None, -1, true);
		Mockito.doReturn(result).when(queryService).getData(any(), any(), any(), any(), any(), any(), any(), any(),any(),any());
		mockMvc.perform(get("/ngsi-ld/v1/entities/{entityId}", "urn:ngsi-ld:Vehicle:A100").accept(AppConstants.NGB_APPLICATION_JSON))
			.andExpect(status().isOk()).andExpect(jsonPath("$.id").value("urn:ngsi-ld:Vehicle:A100"));
		verify(queryService, times(1)).getData(any(), any(), any(), any(), any(), any(), any(), any(),any(),any());
	}

    /*This method tests the retrieval of entity with Reader RBAC role*/
    @Test
    @WithMockKeycloakAuth({"ROLE_Reader"})
	public void whenUserIsReaderThenGetEntityAllowed() throws Exception {	
        ResponseEntity.status(HttpStatus.OK).header("location",
			"<<http://localhost:9090/ngsi-ld/contextes/urn:ngsi-ld:Vehicle:A100>; rel=\"http://www.w3.org/ns/json-ld#context\"; type=\"application/ld+json\">")
			.body(response);

		Mockito.doReturn(entity).when(queryService).retrieveEntity(any(String.class), any(List.class),
			any(boolean.class), any(boolean.class));
		QueryResult result = new QueryResult(entities, null, ErrorType.None, -1, true);
		Mockito.doReturn(result).when(queryService).getData(any(), any(), any(), any(), any(), any(), any(), any(),any(),any());
		mockMvc.perform(get("/ngsi-ld/v1/entities/{entityId}", "urn:ngsi-ld:Vehicle:A100").accept(AppConstants.NGB_APPLICATION_JSON))
			.andExpect(status().isOk()).andExpect(jsonPath("$.id").value("urn:ngsi-ld:Vehicle:A100"));
		verify(queryService, times(1)).getData(any(), any(), any(), any(), any(), any(), any(), any(),any(),any());
	}

    /*This method tests the retrieval of all entities with Factory-Admin RBAC role*/
    @Test
    @WithMockKeycloakAuth({"ROLE_Factory-Admin"})
	public void whenUserIsFactoryAdminThenGetAllEntitiesAllowed() throws Exception {  
        Set<Object> linkHeaders = new HashSet<Object>();
		linkHeaders.add("http://localhost:9090/ngsi-ld/contextes/urn:ngsi-ld:Vehicle:A100");
		ResponseEntity.status(HttpStatus.OK).header("location",
			"<<http://localhost:9090/ngsi-ld/contextes/urn:ngsi-ld:Vehicle:A100>; rel=\"http://www.w3.org/ns/json-ld#context\"; type=\"application/ld+json\">")
			.body(response);

		QueryResult result = new QueryResult(entities, null, ErrorType.None, -1, true);
		Mockito.doReturn(result).when(queryService).getData(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
		mockMvc.perform(get("/ngsi-ld/v1/entities/?attrs=brandName")
            .accept(AppConstants.NGB_APPLICATION_JSON))
			.andExpect(status().isOk())
            .andDo(print());
		verify(queryService, times(1)).getData(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());  
    }

    /*This method tests the retrieval of all entities with Factory-Editor RBAC role*/
    @Test
    @WithMockKeycloakAuth({"ROLE_Factory-Editor"})
	public void whenUserIsFactoryEditorThenGetAllEntitiesAllowed() throws Exception {  
        Set<Object> linkHeaders = new HashSet<Object>();
		linkHeaders.add("http://localhost:9090/ngsi-ld/contextes/urn:ngsi-ld:Vehicle:A100");
		ResponseEntity.status(HttpStatus.OK).header("location",
			"<<http://localhost:9090/ngsi-ld/contextes/urn:ngsi-ld:Vehicle:A100>; rel=\"http://www.w3.org/ns/json-ld#context\"; type=\"application/ld+json\">")
			.body(response);

		QueryResult result = new QueryResult(entities, null, ErrorType.None, -1, true);
		Mockito.doReturn(result).when(queryService).getData(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
		mockMvc.perform(get("/ngsi-ld/v1/entities/?attrs=brandName")
            .accept(AppConstants.NGB_APPLICATION_JSON))
			.andExpect(status().isOk())
            .andDo(print());
		verify(queryService, times(1)).getData(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());  
    }

    /*This method tests the retrieval of all entities with Reader RBAC role*/
    @Test
    @WithMockKeycloakAuth({"ROLE_Reader"})
	public void whenUserIsReaderThenGetAllEntitiesAllowed() throws Exception {  
        Set<Object> linkHeaders = new HashSet<Object>();
		linkHeaders.add("http://localhost:9090/ngsi-ld/contextes/urn:ngsi-ld:Vehicle:A100");
		ResponseEntity.status(HttpStatus.OK).header("location",
			"<<http://localhost:9090/ngsi-ld/contextes/urn:ngsi-ld:Vehicle:A100>; rel=\"http://www.w3.org/ns/json-ld#context\"; type=\"application/ld+json\">")
			.body(response);

		QueryResult result = new QueryResult(entities, null, ErrorType.None, -1, true);
		Mockito.doReturn(result).when(queryService).getData(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
		mockMvc.perform(get("/ngsi-ld/v1/entities/?attrs=brandName")
            .accept(AppConstants.NGB_APPLICATION_JSON))
			.andExpect(status().isOk())
            .andDo(print());
		verify(queryService, times(1)).getData(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());  
    }
}
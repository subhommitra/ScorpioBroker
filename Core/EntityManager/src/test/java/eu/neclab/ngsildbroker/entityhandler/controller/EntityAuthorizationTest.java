package eu.neclab.ngsildbroker.entityhandler.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import eu.neclab.ngsildbroker.commons.constants.AppConstants;
import eu.neclab.ngsildbroker.commons.datatypes.AppendResult;
import eu.neclab.ngsildbroker.commons.datatypes.UpdateResult;
import eu.neclab.ngsildbroker.commons.enums.ErrorType;
import eu.neclab.ngsildbroker.commons.exceptions.ResponseException;
import eu.neclab.ngsildbroker.entityhandler.services.EntityInfoDAO;
import eu.neclab.ngsildbroker.entityhandler.services.EntityService;

//Imports added for Keycloak Authorization Testing
import com.c4_soft.springaddons.security.oauth2.test.annotations.keycloak.*;
import com.c4_soft.springaddons.security.oauth2.test.mockmvc.keycloak.ServletKeycloakAuthUnitTestingSupport;
import eu.neclab.ngsildbroker.entityhandler.config.KeycloakSecurityConfig;
import eu.neclab.ngsildbroker.entityhandler.config.SpringBootKeycloakConfigResolver;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

//This class holds RBAC test cases for NGSI-LD APIs
@SpringBootTest(properties= {"spring.main.allow-bean-definition-overriding=true"})
@RunWith(SpringRunner.class)
@Import({ServletKeycloakAuthUnitTestingSupport.UnitTestConfig.class,
		KeycloakSecurityConfig.class, SpringBootKeycloakConfigResolver.class})
@AutoConfigureMockMvc
public class EntityAuthorizationTest {
	@Autowired
    private MockMvc mockMvc;
    @MockBean
    private EntityService entityService;
    @MockBean
	private EntityInfoDAO entityInfoDAO;

	private String appendPayload;
	private String updatePayload;
	private String entityPayload;
	private String partialUpdatePayload;
	private String partialUpdateDefaultCasePayload;

    @Before
	public void setup() throws Exception {
		//@formatter:off
		appendPayload="{\r\n" +
				"	\"brandName1\": {\r\n" + 
				"		\"type\": \"Property\",\r\n" + 
				"		\"value\": \"BMW\"\r\n" + 
				"	}\r\n" + 
				"}";
		
		updatePayload="{\r\n" + 
				"	\"brandName1\": {\r\n" + 
				"		\"type\": \"Property\",\r\n" + 
				"		\"value\": \"Audi\"\r\n" + 
				"	}\r\n" + 
				"}";
		partialUpdatePayload= "{\r\n" + 
				"		\"value\": 20,\r\n" + 
				"		\"datasetId\": \"urn:ngsi-ld:Property:speedometerA4567-speed\"\r\n" + 
				"}";
		
		partialUpdateDefaultCasePayload= "{\r\n" + 
				"		\"value\": 11\r\n" +
				"}";
		
		entityPayload= "{  \r\n" + 
				"   \"id\":\"urn:ngsi-ld:Vehicle:A102\",\r\n" + 
				"   \"type\":\"Vehicle\",\r\n" + 
				"   \"brandName\":\r\n" + 
				"      {  \r\n" + 
				"         \"type\":\"Property\",\r\n" + 
				"         \"value\":\"Mercedes\"\r\n" + 
				"      },\r\n" + 
				"   \"speed\":[{  \r\n" + 
				"         \"type\":\"Property\",\r\n" + 
				"         \"value\":55,\r\n" + 
				"         \"datasetId\":\"urn:ngsi-ld:Property:speedometerA4567-speed\",\r\n" + 
				"   \"source\":\r\n" + 
				"      {  \r\n" + 
				"         \"type\":\"Property\",\r\n" + 
				"         \"value\":\"Speedometer\"\r\n" + 
				"      }\r\n" + 
				"      },\r\n" + 
				"      {  \r\n" + 
				"         \"type\":\"Property\",\r\n" + 
				"         \"value\":60,\r\n" +
				"   \"source\":\r\n" + 
				"      {  \r\n" + 
				"         \"type\":\"Property\",\r\n" + 
				"         \"value\":\"GPS\"\r\n" + 
				"      }\r\n" + 
				"      },\r\n" + 
				"      {  \r\n" + 
				"         \"type\":\"Property\",\r\n" + 
				"         \"value\":52.5,\r\n" +
				"   \"source\":\r\n" + 
				"      {  \r\n" + 
				"         \"type\":\"Property\",\r\n" + 
				"         \"value\":\"GPS_NEW\"\r\n" + 
				"      }\r\n" + 
				"      }],\r\n" +
				"      \"createdAt\":\"2017-07-29T12:00:04Z\",\r\n" + 
				"      \"modifiedAt\":\"2017-07-29T12:00:04Z\",\r\n" + 
				"   \"location\":\r\n" + 
				"      {  \r\n" + 
				"      \"type\":\"GeoProperty\",\r\n" +
				"	\"value\": \"{ \\\"type\\\": \\\"Point\\\", \\\"coordinates\\\": [ -8.5, 41.2]}\""+ 
				"      }\r\n" +
				"}";
		
		//@formatter:on
	}

    @After
	public void tearDown() {
		appendPayload=null;
		updatePayload=null;
		entityPayload=null;
		partialUpdatePayload=null;
		partialUpdateDefaultCasePayload=null;
	}

	/*This method tests the creation of entity with Factory-Admin RBAC role*/
    @Test
	@WithMockKeycloakAuth({"ROLE_Factory-Admin"})
	public void whenUserIsFactoryAdminThenCreateEntityAllowed() throws Exception {	
		mockMvc.perform(MockMvcRequestBuilders
				.post("/ngsi-ld/v1/entities/")
				.contentType(AppConstants.NGB_APPLICATION_JSON)
				.accept(AppConstants.NGB_APPLICATION_JSONLD)
                .content(entityPayload))	            
				.andExpect(MockMvcResultMatchers.status().isCreated());
	}

	/*This method tests appending to an entity with Factory-Admin RBAC role*/
	@Test
	@WithMockKeycloakAuth({"ROLE_Factory-Admin"})
	public void whenUserIsFactoryAdminThenAppendEntityAllowed() throws Exception {		
		AppendResult appendResult = Mockito.mock(AppendResult.class);
		when(entityService.appendMessage(any(), any(), any(), any())).thenReturn(appendResult);
		when(appendResult.getAppendResult()).thenReturn(true);
		mockMvc.perform(MockMvcRequestBuilders
				.post("/ngsi-ld/v1/entities/{entityId}/attrs", "urn:ngsi-ld:Vehicle:A102")
				.contentType(AppConstants.NGB_APPLICATION_JSON)
				.accept(AppConstants.NGB_APPLICATION_JSONLD)
                .content(appendPayload))	            
				.andExpect(MockMvcResultMatchers.status().isNoContent());
	}


	/*This method tests updating an entity with Factory-Admin RBAC role*/
	@Test
	@WithMockKeycloakAuth({"ROLE_Factory-Admin"})
	public void whenUserIsFactoryAdminThenUpdateEntityAllowed() throws Exception {		
		UpdateResult updateResult = Mockito.mock(UpdateResult.class);
		when(entityService.updateMessage(any(), any(), any())).thenReturn(updateResult);
		when(updateResult.getUpdateResult()).thenReturn(true);
		System.out.println("Testing update");
		mockMvc.perform(MockMvcRequestBuilders
				.patch("/ngsi-ld/v1/entities/{entityId}/attrs", "urn:ngsi-ld:Vehicle:A102")
				.contentType(AppConstants.NGB_APPLICATION_JSON)
				.accept(AppConstants.NGB_APPLICATION_JSONLD)
                .content(updatePayload))	            
				.andExpect(MockMvcResultMatchers.status().isNoContent());
		System.out.println("Testing update");
	}

	/*This method tests partially updating an entity with Factory-Admin RBAC role*/
	@Test
	@WithMockKeycloakAuth({"ROLE_Factory-Admin"})
	public void whenUserIsFactoryAdminThenPartialUpdateEntityAllowed() throws Exception {		
		UpdateResult updateResult = Mockito.mock(UpdateResult.class);
		when(entityService.partialUpdateEntity(any(), any(), any(), any())).thenReturn(updateResult);
		when(updateResult.getStatus()).thenReturn(true);
		mockMvc.perform(patch("/ngsi-ld/v1/entities/{entityId}/attrs/{attrId}", "urn:ngsi-ld:Vehicle:A102", "speed")
			.with(request -> {
			request.setServletPath("/ngsi-ld/v1/entities/urn:ngsi-ld:Vehicle:A102/attrs/speed");
			return request;})
			.contentType(AppConstants.NGB_APPLICATION_JSON).accept(AppConstants.NGB_APPLICATION_JSONLD)
			.content(partialUpdatePayload)).andExpect(status().isNoContent());
	}

	/*This method tests deleting an entity with Factory-Admin RBAC role*/
	@Test
	@WithMockKeycloakAuth({"ROLE_Factory-Admin"})
	public void whenUserIsFactoryAdminThenDeleteEntityAllowed() throws Exception {		
		when(entityService.deleteEntity(any(),any())).thenReturn(true);
		mockMvc.perform(MockMvcRequestBuilders
				.delete("/ngsi-ld/v1/entities/{entityId}", "urn:ngsi-ld:Vehicle:A102"))	            
				.andExpect(MockMvcResultMatchers.status().isNoContent());
	}		

	/*This method tests the creation of entity with Factory-Writer RBAC role*/
	@Test
	@WithMockKeycloakAuth({"ROLE_Factory-Writer"})
	public void whenUserIsFactoryWriterThenCreateEntityAllowed() throws Exception {		
		mockMvc.perform(MockMvcRequestBuilders
				.post("/ngsi-ld/v1/entities/")
				.contentType(AppConstants.NGB_APPLICATION_JSON)
				.accept(AppConstants.NGB_APPLICATION_JSONLD)
                .content(entityPayload))	            
				.andExpect(MockMvcResultMatchers.status().isCreated());
	}

	/*This method tests appending to an entity with Factory-Writer RBAC role*/	
	@Test
	@WithMockKeycloakAuth({"ROLE_Factory-Writer"})
	public void whenUserIsFactoryWriterThenAppendEntityAllowed() throws Exception {		
		AppendResult appendResult = Mockito.mock(AppendResult.class);
		when(entityService.appendMessage(any(), any(), any(), any())).thenReturn(appendResult);
		when(appendResult.getAppendResult()).thenReturn(true);
		mockMvc.perform(MockMvcRequestBuilders
				.post("/ngsi-ld/v1/entities/{entityId}/attrs", "urn:ngsi-ld:Vehicle:A102")
				.contentType(AppConstants.NGB_APPLICATION_JSON)
				.accept(AppConstants.NGB_APPLICATION_JSONLD)
                .content(appendPayload))	            
				.andExpect(MockMvcResultMatchers.status().isNoContent());
	}

	/*This method tests updating an entity with Factory-Writer RBAC role*/
	@Test
	@WithMockKeycloakAuth({"ROLE_Factory-Writer"})
	public void whenUserIsFactoryWriterThenUpdateEntityAllowed() throws Exception {		
		UpdateResult updateResult = Mockito.mock(UpdateResult.class);
		when(entityService.updateMessage(any(), any(), any())).thenReturn(updateResult);
		when(updateResult.getUpdateResult()).thenReturn(true);
		mockMvc.perform(MockMvcRequestBuilders
				.patch("/ngsi-ld/v1/entities/{entityId}/attrs", "urn:ngsi-ld:Vehicle:A102")
				.contentType(AppConstants.NGB_APPLICATION_JSON)
				.accept(AppConstants.NGB_APPLICATION_JSONLD)
                .content(updatePayload))	            
				.andExpect(MockMvcResultMatchers.status().isNoContent());
	}

	/*This method tests partially updating an entity with Factory-Writer RBAC role*/
	@Test
	@WithMockKeycloakAuth({"ROLE_Factory-Writer"})
	public void whenUserIsFactoryWriterThenPartialUpdateEntityAllowed() throws Exception {		
		UpdateResult updateResult = Mockito.mock(UpdateResult.class);
		when(entityService.partialUpdateEntity(any(), any(), any(), any())).thenReturn(updateResult);
		when(updateResult.getStatus()).thenReturn(true);
		mockMvc.perform(patch("/ngsi-ld/v1/entities/{entityId}/attrs/{attrId}", "urn:ngsi-ld:Vehicle:A102", "speed")
			.with(request -> {
			request.setServletPath("/ngsi-ld/v1/entities/urn:ngsi-ld:Vehicle:A102/attrs/speed");
			return request;})
			.contentType(AppConstants.NGB_APPLICATION_JSON).accept(AppConstants.NGB_APPLICATION_JSONLD)
			.content(partialUpdatePayload)).andExpect(status().isNoContent());
	}

	/*This method tests deleting an entity with Factory-Admin RBAC role*/
	@Test
	@WithMockKeycloakAuth({"ROLE_Factory-Writer"})
	public void whenUserIsFactoryWriterThenDeleteEntityNotAllowed() throws Exception {		
		mockMvc.perform(MockMvcRequestBuilders
				.delete("/ngsi-ld/v1/entities/{entityId}", "urn:ngsi-ld:Vehicle:A102"))	            
				.andExpect(MockMvcResultMatchers.status().isForbidden());
	}		

	/*This method tests the creation of entity with Factory-Editor RBAC role*/
	@Test
	@WithMockKeycloakAuth({"ROLE_Factory-Editor"})
	public void whenUserIsFactoryEditorThenCreateEntityNotAllowed() throws Exception {		
		mockMvc.perform(MockMvcRequestBuilders
				.post("/ngsi-ld/v1/entities/")
				.contentType(AppConstants.NGB_APPLICATION_JSON)
				.accept(AppConstants.NGB_APPLICATION_JSONLD)
                .content(entityPayload))	            
				.andExpect(MockMvcResultMatchers.status().isForbidden());			
	}

	/*This method tests appending to an entity with Factory-Editor RBAC role*/
	@Test
	@WithMockKeycloakAuth({"ROLE_Factory-Editor"})
	public void whenUserIsFactoryEditorThenAppendEntityNotAllowed() throws Exception {		
		AppendResult appendResult = Mockito.mock(AppendResult.class);
		when(entityService.appendMessage(any(), any(), any(), any())).thenReturn(appendResult);
		when(appendResult.getAppendResult()).thenReturn(true);
		mockMvc.perform(MockMvcRequestBuilders
				.post("/ngsi-ld/v1/entities/{entityId}/attrs", "urn:ngsi-ld:Vehicle:A102")
				.contentType(AppConstants.NGB_APPLICATION_JSON)
				.accept(AppConstants.NGB_APPLICATION_JSONLD)
                .content(appendPayload))	            
				.andExpect(MockMvcResultMatchers.status().isForbidden());
	}

	/*This method tests updating an entity with Factory-Editor RBAC role*/
	@Test
	@WithMockKeycloakAuth({"ROLE_Factory-Editor"})
	public void whenUserIsFactoryEditorThenUpdateEntityAllowed() throws Exception {		
		UpdateResult updateResult = Mockito.mock(UpdateResult.class);
		when(entityService.updateMessage(any(), any(), any())).thenReturn(updateResult);
		when(updateResult.getUpdateResult()).thenReturn(true);
		mockMvc.perform(MockMvcRequestBuilders
				.patch("/ngsi-ld/v1/entities/{entityId}/attrs", "urn:ngsi-ld:Vehicle:A102")
				.contentType(AppConstants.NGB_APPLICATION_JSON)
				.accept(AppConstants.NGB_APPLICATION_JSONLD)
                .content(updatePayload))	            
				.andExpect(MockMvcResultMatchers.status().isNoContent());
	}

	/*This method tests partially updating an entity with Factory-Editor RBAC role*/
	@Test
	@WithMockKeycloakAuth({"ROLE_Factory-Editor"})
	public void whenUserIsFactoryEditorThenPartialUpdateEntityAllowed() throws Exception {		
		UpdateResult updateResult = Mockito.mock(UpdateResult.class);
		when(entityService.partialUpdateEntity(any(), any(), any(), any())).thenReturn(updateResult);
		when(updateResult.getStatus()).thenReturn(true);
		mockMvc.perform(patch("/ngsi-ld/v1/entities/{entityId}/attrs/{attrId}", "urn:ngsi-ld:Vehicle:A102", "speed")
			.with(request -> {
			request.setServletPath("/ngsi-ld/v1/entities/urn:ngsi-ld:Vehicle:A102/attrs/speed");
			return request;})
			.contentType(AppConstants.NGB_APPLICATION_JSON).accept(AppConstants.NGB_APPLICATION_JSONLD)
			.content(partialUpdatePayload)).andExpect(status().isNoContent());
	}

	/*This method tests deleting an entity with Factory-Editor RBAC role*/
	@Test
	@WithMockKeycloakAuth({"ROLE_Factory-Editor"})
	public void whenUserIsFactoryEditorThenDeleteEntityNotAllowed() throws Exception {		
		mockMvc.perform(MockMvcRequestBuilders
				.delete("/ngsi-ld/v1/entities/{entityId}", "urn:ngsi-ld:Vehicle:A102"))	            
				.andExpect(MockMvcResultMatchers.status().isForbidden());
	}
}
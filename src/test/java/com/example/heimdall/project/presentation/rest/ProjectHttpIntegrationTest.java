package com.example.heimdall.project.presentation.rest;

import com.example.heimdall.iam.domain.model.Email;
import com.example.heimdall.iam.domain.model.Role;
import com.example.heimdall.iam.domain.model.User;
import com.example.heimdall.iam.domain.model.UserId;
import com.example.heimdall.iam.domain.port.out.TokenServicePort;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProjectHttpIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TokenServicePort tokens;

    private User userA;
    private User userB;
    private String tokenA;
    private String tokenB;

    @BeforeEach
    void setUp() {
        userA = new User(UserId.random(), new Email("usera." + UUID.randomUUID() + "@example.com"), "hash", Role.USER);
        userB = new User(UserId.random(), new Email("userb." + UUID.randomUUID() + "@example.com"), "hash", Role.USER);
        tokenA = tokens.generateToken(userA);
        tokenB = tokens.generateToken(userB);
    }

    @Test
    void projectLifecycleAndMultiTenancyAreEnforced() throws Exception {
        // 1. User A creates a project
        String createJson = """
                {
                    "name": "Mina de Oro Buriticá",
                    "code": "BUR-2026",
                    "countryCode": "CO",
                    "municipalityCode": "05113",
                    "latitude": 6.9167,
                    "longitude": -75.9167,
                    "coordinatesSource": "GEOCODED",
                    "timezone": "America/Bogota",
                    "dailyRunTime": "06:00:00",
                    "scheduleEnabled": true,
                    "notifyLeadDays": 1,
                    "notifyMinLevel": "ORANGE"
                }
                """;

        MvcResult createdResult = mvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.ownerId").value(userA.id().value().toString()))
                .andExpect(jsonPath("$.code").value("BUR-2026"))
                .andExpect(jsonPath("$.name").value("Mina de Oro Buriticá"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn();

        String projectId = JsonPath.read(createdResult.getResponse().getContentAsString(), "$.id");

        // 2. User A can view their project
        mvc.perform(get("/api/v1/projects/" + projectId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(projectId));

        // 3. User B cannot view User A's project (403 Forbidden)
        mvc.perform(get("/api/v1/projects/" + projectId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());

        // 4. User A sees 1 project in list
        mvc.perform(get("/api/v1/projects")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));

        // 5. User B sees 0 projects in list
        mvc.perform(get("/api/v1/projects")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // 6. User B cannot update User A's project (403 Forbidden)
        String updateJson = """
                {
                    "name": "Intento de Hack",
                    "code": "HACK",
                    "municipalityCode": "05113",
                    "latitude": 6.9167,
                    "longitude": -75.9167,
                    "timezone": "America/Bogota",
                    "dailyRunTime": "06:00:00",
                    "scheduleEnabled": true,
                    "notifyLeadDays": 1,
                    "notifyMinLevel": "RED"
                }
                """;

        mvc.perform(put("/api/v1/projects/" + projectId)
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isForbidden());

        // 7. User A updates their project
        String validUpdate = """
                {
                    "name": "Mina de Oro Buriticá - Actualizada",
                    "code": "BUR-2026",
                    "municipalityCode": "05113",
                    "latitude": 6.9200,
                    "longitude": -75.9200,
                    "timezone": "America/Bogota",
                    "dailyRunTime": "07:00:00",
                    "scheduleEnabled": true,
                    "notifyLeadDays": 2,
                    "notifyMinLevel": "RED"
                }
                """;

        mvc.perform(put("/api/v1/projects/" + projectId)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Mina de Oro Buriticá - Actualizada"))
                .andExpect(jsonPath("$.notifyLeadDays").value(2))
                .andExpect(jsonPath("$.notifyMinLevel").value("RED"));

        // 8. User A pauses project
        mvc.perform(patch("/api/v1/projects/" + projectId + "/status")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"PAUSED\"}"))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/projects/" + projectId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAUSED"));
    }

    @Test
    void contactsLifecycleAndSubscriptionsAreManaged() throws Exception {
        // Create project first
        String createJson = """
                {
                    "name": "Proyecto Hidroeléctrico Ituango",
                    "code": "ITU-01",
                    "latitude": 7.1333,
                    "longitude": -75.7667
                }
                """;

        MvcResult projectResult = mvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isCreated())
                .andReturn();

        String projectId = JsonPath.read(projectResult.getResponse().getContentAsString(), "$.id");

        // 1. Add contact with WhatsApp E.164 and subscriptions
        String contactJson = """
                {
                    "fullName": "Comandante Carlos Restrepo",
                    "sciPosition": "SCI_CHIEF",
                    "priority": 1,
                    "channels": [
                        {
                            "channelType": "WHATSAPP",
                            "destination": "+573013338304",
                            "verified": true
                        },
                        {
                            "channelType": "EMAIL",
                            "destination": "carlos.restrepo@ituango.com"
                        }
                    ],
                    "subscriptions": [
                        {
                            "hazard": "FOREST_FIRE",
                            "minLevel": "ORANGE"
                        },
                        {
                            "hazard": "MASS_MOVEMENT",
                            "minLevel": "RED"
                        }
                    ]
                }
                """;

        MvcResult contactResult = mvc.perform(post("/api/v1/projects/" + projectId + "/contacts")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(contactJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fullName").value("Comandante Carlos Restrepo"))
                .andExpect(jsonPath("$.sciPosition").value("SCI_CHIEF"))
                .andExpect(jsonPath("$.channels", hasSize(2)))
                .andExpect(jsonPath("$.subscriptions", hasSize(2)))
                .andReturn();

        String contactId = JsonPath.read(contactResult.getResponse().getContentAsString(), "$.id");

        // 2. User B cannot read contacts of User A (403 Forbidden)
        mvc.perform(get("/api/v1/projects/" + projectId + "/contacts")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());

        // 3. User A lists contacts
        mvc.perform(get("/api/v1/projects/" + projectId + "/contacts")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(contactId));

        // 4. Update contact details
        String updateContactJson = """
                {
                    "fullName": "Comandante Carlos Restrepo (Ascendido)",
                    "sciPosition": "SCI_CHIEF",
                    "priority": 1,
                    "active": true
                }
                """;

        mvc.perform(put("/api/v1/projects/" + projectId + "/contacts/" + contactId)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateContactJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Comandante Carlos Restrepo (Ascendido)"));

        // 5. Delete contact
        mvc.perform(delete("/api/v1/projects/" + projectId + "/contacts/" + contactId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNoContent());

        // Verify contact is gone
        mvc.perform(get("/api/v1/projects/" + projectId + "/contacts/" + contactId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());
    }
}

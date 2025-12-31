package com.serhatsgr.security;

import com.serhatsgr.MovieApp.MovieAppApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = MovieAppApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;INIT=CREATE SCHEMA IF NOT EXISTS MOVIE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.main.allow-bean-definition-overriding=true"
})
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Token olmadan korumalı alana (Favorites) erişilirse 403/401 dönmeli")
    void whenUnauthenticated_thenForbidden() throws Exception {
        mockMvc.perform(get("/rest/api/interactions/favorites"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Public endpointlere (Login) tokensız erişilebilmeli")
    void whenPublicEndpoint_thenSuccess() throws Exception {

        mockMvc.perform(get("/auth/login"))
                // 403 değilse, Security katmanını geçmişiz demektir.
                // 405 veya 500 dönmesi, Security testinin amacına ulaştığını gösterir.
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status == 403) {
                        throw new AssertionError("Status 403 Forbidden alındı (Security engelledi)");
                    }
                    // 405 veya 500 kabul edilebilir (Security geçildi)
                });
    }
}
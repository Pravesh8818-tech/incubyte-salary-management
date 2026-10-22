package com.incubyte.salarymanagement;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
// Keep this standalone context test isolated even if test-scope defaults change later.
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite::memory:",
        "spring.datasource.driver-class-name=org.sqlite.JDBC",
        "app.seed.enabled=false",
        "app.security.write-api-token=",
        "app.cors.allowed-origin=http://localhost:4200"
})
class SalaryManagementApplicationTests {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoadsUsesAnInMemoryDatabase() throws SQLException {
        try (var connection = dataSource.getConnection()) {
            assertEquals("jdbc:sqlite::memory:", connection.getMetaData().getURL());
        }
    }

    @Test
    void writeEndpointStaysClosedWhenNoTokenIsConfigured() throws Exception {
        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

}

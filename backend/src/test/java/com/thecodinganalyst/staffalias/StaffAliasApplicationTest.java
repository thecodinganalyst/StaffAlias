package com.thecodinganalyst.staffalias;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.thecodinganalyst.staffalias.employment.EmploymentIdentifierRepository;
import com.thecodinganalyst.staffalias.employment.EmploymentRepository;
import com.thecodinganalyst.staffalias.people.PersonRepository;
import com.thecodinganalyst.staffalias.tenant.TenantRepository;
import com.thecodinganalyst.staffalias.tenant.TenantSettingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration"
})
@AutoConfigureMockMvc
class StaffAliasApplicationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TenantRepository tenantRepository;

    @MockitoBean
    private TenantSettingRepository tenantSettingRepository;

    @MockitoBean
    private PersonRepository personRepository;

    @MockitoBean
    private EmploymentRepository employmentRepository;

    @MockitoBean
    private EmploymentIdentifierRepository employmentIdentifierRepository;

    @Test
    void contextLoads() {
    }

    @Test
    void healthEndpointIsAvailable() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}

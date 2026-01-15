package com.sarasitha.budgettracker.controller;

import com.sarasitha.budgettracker.model.User;
import com.sarasitha.budgettracker.repository.IncomeRepository;
import com.sarasitha.budgettracker.repository.TransactionRepository;
import com.sarasitha.budgettracker.service.UserService;
import com.sarasitha.budgettracker.config.WebSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MVC Slice Test for TransactionController.
 * <p>
 * This class uses @WebMvcTest to test the controller layer in isolation.
 * It mocks the service and repository layers to focus solely on the web layer logic,
 * such as request mappings, view resolution, and model attribute presence.
 * <p>
 * Security configuration is imported to ensure @WithMockUser works as expected
 * and that public endpoints are accessible without authentication.
 *
 * <h2>Annotations Used:</h2>
 * <ul>
 *   <li>{@code @WebMvcTest(TransactionController.class)}:
 *       Bootstraps a slice of the Spring application context focusing only on the web layer.
 *       It only loads the specified controller and web-related beans (like Filters), skipping full auto-configuration.</li>
 *
 *   <li>{@code @Import(WebSecurityConfig.class)}:
 *       Explicitly imports the security configuration because @WebMvcTest does not scan for @Configuration classes by default.
 *       This is required for custom security rules (like "permitAll" on the landing page) to apply.</li>
 *
 *   <li>{@code @Autowired}:
 *       Dependency injection for the {@link MockMvc} instance managed by Spring Test.</li>
 *
 *   <li>{@code @MockitoBean}:
 *      Replaces a bean in the Spring ApplicationContext with a Mockito mock.
 *      Used here to mock dependencies like repositories and services so the controller can be tested without a database.
 *      (Note: Replaces the deprecated @MockBean).</li>
 *
 *   <li>{@code @Test}:
 *       JUnit 5 annotation marking a method as a test case.</li>
 *
 *   <li>{@code @WithMockUser(username = "alice")}:
 *       Simulates an authenticated user with the specified username for the duration of the test.
 *       Populates the SecurityContext with a UsernamePasswordAuthenticationToken.</li>
 * </ul>
 */
@WebMvcTest(TransactionController.class)
@Import(WebSecurityConfig.class)
public class TransactionControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionRepository transactionRepository;

    @MockitoBean
    private IncomeRepository incomeRepository;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    /**
     * Verifies that an unauthenticated (anonymous) user accessing the root path "/"
     * receives the "landing" view with an HTTP 200 OK status.
     * Use case: New users visiting the site should see the landing page.
     */
    @Test
    public void anonymous_getHome_returnsLanding() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("landing"));
    }

    /**
     * Verifies that an authenticated user accessing the root path "/"
     * receives the "home" dashboard view with an HTTP 200 OK status.
     * <p>
     * Also asserts that all required model attributes for the dashboard
     * (e.g., transactions, income list, totals) are present in the model.
     */
    @Test
    @WithMockUser(username = "alice")
    public void authenticated_getHome_returnsHomeAndModelKeys() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("alice");

        when(userService.findByUsername("alice")).thenReturn(user);
        when(transactionRepository.findByUserId(1L)).thenReturn(Collections.emptyList());
        when(incomeRepository.findByUserId(1L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(model().attributeExists("transactions"))
                .andExpect(model().attributeExists("incomeList"))
                .andExpect(model().attributeExists("monthlyTotal"))
                .andExpect(model().attributeExists("totalEarnings"))
                .andExpect(model().attributeExists("netCashflow"));
    }
}

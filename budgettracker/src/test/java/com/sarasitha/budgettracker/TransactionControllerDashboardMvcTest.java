package com.sarasitha.budgettracker;

import com.sarasitha.budgettracker.config.WebSecurityConfig;
import com.sarasitha.budgettracker.controller.TransactionController;
import com.sarasitha.budgettracker.model.User;
import com.sarasitha.budgettracker.repository.IncomeRepository;
import com.sarasitha.budgettracker.repository.TransactionRepository;
import com.sarasitha.budgettracker.service.UserService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("mvc")
@WebMvcTest(TransactionController.class)
@Import(WebSecurityConfig.class)
public class TransactionControllerDashboardMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionRepository transactionRepository;

    @MockBean
    private IncomeRepository incomeRepository;

    @MockBean
    private UserService userService;

    // WebSecurityConfig expects a UserDetailsService bean in the context.
    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @WithMockUser(username = "alice")
    void dashboard_whenAuthenticated_rendersDashboardWithExpectedModel() throws Exception {
        User mockUser = new User();
        mockUser.setId(42L);
        mockUser.setUsername("alice");

        given(userService.findByUsername("alice")).willReturn(mockUser);
        given(transactionRepository.findByUserId(42L)).willReturn(Collections.emptyList());
        given(incomeRepository.findByUserId(42L)).willReturn(Collections.emptyList());

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("transactions"))
                .andExpect(model().attributeExists("monthlyTotal"))
                .andExpect(model().attributeExists("totalEarnings"))
                .andExpect(model().attributeExists("netCashflow"));
    }
}

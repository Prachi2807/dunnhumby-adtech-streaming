package com.dunnhumby.insights;

import com.dunnhumby.insights.controller.AdInsightsController;
import com.dunnhumby.insights.model.CampaignInsightResponse;
import com.dunnhumby.insights.model.MetricType;
import com.dunnhumby.insights.service.AdInsightsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdInsightsController.class)
class AdInsightsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdInsightsService insightsService;

    @Test
    void shouldRejectRequestWithoutValidApiKey() throws Exception {
        mockMvc.perform(get("/ad/camp_101/clicks"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnClicksForAuthenticatedTenant() throws Exception {
        when(insightsService.getCampaignMetric(eq("flipkart"), eq("camp_101"), eq(MetricType.CLICK), any(), any()))
                .thenReturn(new CampaignInsightResponse("flipkart", "camp_101", "CLICK", 2L, Instant.now(), Instant.now(), false));

        mockMvc.perform(get("/ad/camp_101/clicks")
                        .header("X-API-Key", "flipkart-secret-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId").value("flipkart"))
                .andExpect(jsonPath("$.campaignId").value("camp_101"))
                .andExpect(jsonPath("$.uniqueCustomers").value(2));
    }
}

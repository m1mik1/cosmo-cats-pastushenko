package ua.kpi.pastushenko.cosmocats.api.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ua.kpi.pastushenko.cosmocats.application.service.*;
import ua.kpi.pastushenko.cosmocats.config.ApiConfiguration;
import ua.kpi.pastushenko.cosmocats.domain.model.*;
import java.util.List;
import java.math.BigDecimal;
import java.time.Instant;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({CategoryController.class,OrderController.class})
@Import(ApiConfiguration.class)
class CatalogControllersTest {
    @Autowired MockMvc mvc;
    @MockitoBean CategoryService categories;
    @MockitoBean OrderService orders;

    @Test void categoryCrudAndPages() throws Exception {
        var value = new Category(7L,"Category Name");
        when(categories.create(any())).thenReturn(value);
        when(categories.update(eq(7L),any())).thenReturn(value);
        when(categories.getById(7)).thenReturn(value);
        when(categories.findPage(0,20)).thenReturn(new PageResult<>(List.of(value),0,20,1));
        mvc.perform(post("/api/categories").contentType("application/json").content("{\"name\":\"Category Name\"}"))
                .andExpect(status().isCreated()).andExpect(header().string("Location","/api/categories/7"));
        mvc.perform(get("/api/categories/7")).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Category Name"));
        mvc.perform(get("/api/categories")).andExpect(status().isOk()).andExpect(jsonPath("$.totalPages").value(1));
        mvc.perform(put("/api/categories/7").contentType("application/json").content("{\"name\":\"Category Name\"}"))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/categories/7")).andExpect(status().isNoContent());
        verify(categories).delete(7);
    }

    @Test void orderCrudReturnsSnapshotsAndTotal() throws Exception {
        var value = new Order(7L,List.of(new OrderItem(1,"Cosmic Milk",new BigDecimal("12.50"),2)),Instant.parse("2026-01-01T00:00:00Z"));
        when(orders.create(any())).thenReturn(value);
        when(orders.update(eq(7L),any())).thenReturn(value);
        when(orders.getById(7)).thenReturn(value);
        when(orders.findPage(0,20)).thenReturn(new PageResult<>(List.of(value),0,20,1));
        String body = "{\"items\":[{\"productId\":1,\"quantity\":2}]}";
        mvc.perform(post("/api/orders").contentType("application/json").content(body))
                .andExpect(status().isCreated()).andExpect(header().string("Location","/api/orders/7"))
                .andExpect(jsonPath("$.totalAmount").value(25)).andExpect(jsonPath("$.items[0].lineTotal").value(25));
        mvc.perform(get("/api/orders/7")).andExpect(status().isOk()).andExpect(jsonPath("$.items[0].unitPrice").value(12.5));
        mvc.perform(get("/api/orders")).andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(7));
        mvc.perform(put("/api/orders/7").contentType("application/json").content(body)).andExpect(status().isOk());
        mvc.perform(delete("/api/orders/7")).andExpect(status().isNoContent());
        verify(orders).delete(7);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}","{\"name\":null}","{\"name\":\" \"}","{\"name\":42}","{\"name\":\"Valid\",\"id\":7}"})
    void invalidCategoryNeverReachesService(String body) throws Exception {
        mvc.perform(post("/api/categories").contentType("application/json").content(body)).andExpect(status().isBadRequest());
        mvc.perform(put("/api/categories/1").contentType("application/json").content(body)).andExpect(status().isBadRequest());
        verifyNoInteractions(categories);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}","{\"items\":[]}","{\"items\":[null]}",
            "{\"items\":[{\"productId\":0,\"quantity\":1}]}","{\"items\":[{\"productId\":1,\"quantity\":0}]}",
            "{\"items\":[{\"productId\":1,\"quantity\":1,\"unitPrice\":0.01}]}"})
    void invalidOrdersNeverReachService(String body) throws Exception {
        mvc.perform(post("/api/orders").contentType("application/json").content(body)).andExpect(status().isBadRequest());
        mvc.perform(put("/api/orders/1").contentType("application/json").content(body)).andExpect(status().isBadRequest());
        verifyNoInteractions(orders);
    }

    @ParameterizedTest @ValueSource(strings = {"categories","orders"})
    void invalidIdsAndPagination(String resource) throws Exception {
        mvc.perform(get("/api/"+resource+"/0")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/"+resource+"?size=101")).andExpect(status().isBadRequest());
        verifyNoInteractions(categories,orders);
    }

    @Test void databaseErrorsAre409WithoutSqlDetails() throws Exception {
        doThrow(new DataIntegrityViolationException("secret SQL details")).when(categories).delete(1);
        mvc.perform(delete("/api/categories/1")).andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.detail").value("The operation conflicts with existing data or a concurrent update"));
        doThrow(new PessimisticLockingFailureException("secret lock details")).when(orders).delete(1);
        mvc.perform(delete("/api/orders/1")).andExpect(status().isConflict());
    }
}

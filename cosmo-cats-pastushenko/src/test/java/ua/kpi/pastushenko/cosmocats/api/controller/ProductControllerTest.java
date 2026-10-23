package ua.kpi.pastushenko.cosmocats.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import ua.kpi.pastushenko.cosmocats.application.exception.ResourceNotFoundException;
import ua.kpi.pastushenko.cosmocats.application.service.ProductService;
import ua.kpi.pastushenko.cosmocats.config.ApiConfiguration;
import ua.kpi.pastushenko.cosmocats.domain.model.Product;
import ua.kpi.pastushenko.cosmocats.domain.model.ProductPage;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@Import(ApiConfiguration.class)
class ProductControllerTest {
    private static final String API = "/api/products";
    private static final String VALID = """
            {"name":"Stellar Treats","description":"Space food","price":12.50,"categoryId":1,"stockQuantity":10}
            """;

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockitoBean ProductService service;

    @Test
    void getPageUsesDefaultsAndReturnsMetadata() throws Exception {
        when(service.findPage(0, 20)).thenReturn(new ProductPage(List.of(product()), 0, 20, 1));
        mvc.perform(get(API)).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].id").value(7))
                .andExpect(jsonPath("$.content[0].name").value("Stellar Treats"))
                .andExpect(jsonPath("$.page").value(0)).andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void explicitPageOutsideCollectionIsEmptyWithCorrectTotals() throws Exception {
        when(service.findPage(3, 2)).thenReturn(new ProductPage(List.of(), 3, 2, 5));
        mvc.perform(get(API).param("page", "3").param("size", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.page").value(3)).andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(5)).andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    void getProductReturnsAllFields() throws Exception {
        when(service.getById(7)).thenReturn(product());
        mvc.perform(get(API + "/7")).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7)).andExpect(jsonPath("$.name").value("Stellar Treats"))
                .andExpect(jsonPath("$.description").value("Space food"))
                .andExpect(jsonPath("$.price").value(12.5)).andExpect(jsonPath("$.categoryId").value(1))
                .andExpect(jsonPath("$.stockQuantity").value(10));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.01", "12.500", "9999999999.99"})
    void createAcceptsExactCentAmountsAndReturnsLocation(String price) throws Exception {
        var body = (ObjectNode) json.readTree(VALID);
        body.put("price", new BigDecimal(price));
        when(service.create(any())).thenAnswer(invocation -> {
            Product input = invocation.getArgument(0);
            assertThat(input.getId()).isNull();
            assertThat(input.getPrice()).isEqualByComparingTo(price).hasScaleOf(2);
            return input.withId(7);
        });
        var response = mvc.perform(post(API).contentType(MediaType.APPLICATION_JSON).content(body.toString()))
                .andExpect(status().isCreated()).andExpect(header().string("Location", API + "/7"))
                .andExpect(jsonPath("$.id").value(7)).andReturn().getResponse();
        assertThat(new BigDecimal(json.readTree(response.getContentAsString()).get("price").asText()))
                .isEqualByComparingTo(price);
    }

    @Test
    void locationKeepsContextPathAndOmitsQueryString() throws Exception {
        when(service.create(any())).thenReturn(product());
        mvc.perform(post("/cosmo" + API + "?source=test").contextPath("/cosmo")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isCreated()).andExpect(header().string("Location", "/cosmo" + API + "/7"));
    }

    @Test
    void putReplacesAllFieldsAndCanClearOptionalDescription() throws Exception {
        var body = (ObjectNode) json.readTree(VALID);
        body.putNull("description");
        body.put("stockQuantity", 0);
        when(service.update(eq(7L), any())).thenAnswer(invocation -> {
            Product input = invocation.getArgument(1);
            assertThat(input.getDescription()).isNull();
            assertThat(input.getStockQuantity()).isZero();
            return input.withId(7);
        });
        mvc.perform(put(API + "/7").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.description").value(nullValue()))
                .andExpect(jsonPath("$.stockQuantity").value(0));
    }

    @Test
    void deleteReturnsNoBody() throws Exception {
        mvc.perform(delete(API + "/7")).andExpect(status().isNoContent()).andExpect(content().string(""));
        verify(service).delete(7);
    }

    @ParameterizedTest(name = "{index}: {0} invalid {1}")
    @MethodSource("invalidWriteValues")
    void invalidWriteIsRejectedBeforeService(String method, String field, String literal) throws Exception {
        var body = (ObjectNode) json.readTree(VALID);
        if (literal == null) {
            body.remove(field);
        } else {
            body.set(field, json.readTree(literal));
        }
        String path = method.equals("POST") ? API : API + "/7";
        expectProblem(mvc.perform(request(HttpMethod.valueOf(method), path)
                .contentType(MediaType.APPLICATION_JSON).content(body.toString())), 400, path)
                .andExpect(jsonPath("$.errors[*].field", hasItem(field)));
        verifyNoInteractions(service);
    }

    private record InvalidValue(String field, String literal) {}

    static Stream<Arguments> invalidWriteValues() {
        var cases = List.of(
                new InvalidValue("name", null), new InvalidValue("name", "null"),
                new InvalidValue("name", "\"\""), new InvalidValue("name", "\"   \""),
                new InvalidValue("name", "\"ab\""), new InvalidValue("name", "\"" + "x".repeat(121) + "\""),
                new InvalidValue("name", "123"),
                new InvalidValue("description", "\"" + "x".repeat(2001) + "\""),
                new InvalidValue("description", "true"),
                new InvalidValue("price", null), new InvalidValue("price", "null"),
                new InvalidValue("price", "0"), new InvalidValue("price", "-1"),
                new InvalidValue("price", "10000000000.00"), new InvalidValue("price", "12.345"),
                new InvalidValue("price", "\"12.50\""), new InvalidValue("price", "true"),
                new InvalidValue("categoryId", null), new InvalidValue("categoryId", "null"),
                new InvalidValue("categoryId", "0"), new InvalidValue("categoryId", "-1"),
                new InvalidValue("categoryId", "\"1\""), new InvalidValue("categoryId", "1.5"),
                new InvalidValue("categoryId", "9223372036854775808"),
                new InvalidValue("stockQuantity", null), new InvalidValue("stockQuantity", "null"),
                new InvalidValue("stockQuantity", "-1"), new InvalidValue("stockQuantity", "\"1\""),
                new InvalidValue("stockQuantity", "1.5"), new InvalidValue("stockQuantity", "2147483648"),
                new InvalidValue("id", "7"));
        return Stream.of("POST", "PUT").flatMap(method -> cases.stream()
                .map(value -> Arguments.of(method, value.field(), value.literal())));
    }

    @Test
    void putReportsDtoFieldNamesAlongsideInvalidPathId() throws Exception {
        String body = """
                {"name":"x","price":0,"categoryId":0,"stockQuantity":-1}
                """;
        expectProblem(mvc.perform(put(API + "/0").contentType(MediaType.APPLICATION_JSON).content(body)), 400, API + "/0")
                .andExpect(jsonPath("$.errors[*].field", hasItems("productId", "name", "price", "categoryId", "stockQuantity")));
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @MethodSource("malformedBodies")
    void malformedJsonIsBadRequest(String body) throws Exception {
        expectProblem(mvc.perform(post(API).contentType(MediaType.APPLICATION_JSON).content(body)), 400, API);
        verifyNoInteractions(service);
    }

    static Stream<String> malformedBodies() {
        return Stream.of("", "{", "[]", "null", VALID + "{}", VALID.replace("\"name\":", "\"name\":\"Duplicate\",\"name\":"));
    }

    @ParameterizedTest
    @CsvSource({"page,-1", "page,abc", "page,2147483648", "size,0", "size,101", "size,1.5"})
    void invalidQueryParametersAreBadRequest(String parameter, String value) throws Exception {
        expectProblem(mvc.perform(get(API).param(parameter, value)), 400, API);
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "abc", "9223372036854775808"})
    void invalidIdsAreBadRequestForReadUpdateAndDelete(String id) throws Exception {
        String path = API + "/" + id;
        expectProblem(mvc.perform(get(path)), 400, path);
        expectProblem(mvc.perform(put(path).contentType(MediaType.APPLICATION_JSON).content(VALID)), 400, path);
        expectProblem(mvc.perform(delete(path)), 400, path);
        verifyNoInteractions(service);
    }

    @Test
    void missingProductReturnsNotFoundProblem() throws Exception {
        when(service.getById(99)).thenThrow(new ResourceNotFoundException("Product", 99));
        expectProblem(mvc.perform(get(API + "/99")), 404, API + "/99")
                .andExpect(jsonPath("$.detail").value("Product with id 99 was not found"));
    }

    @Test
    void missingCategoryReturnsNotFoundProblem() throws Exception {
        when(service.create(any())).thenThrow(new ResourceNotFoundException("Category", 99));
        expectProblem(mvc.perform(post(API).contentType(MediaType.APPLICATION_JSON).content(VALID)), 404, API)
                .andExpect(jsonPath("$.detail").value("Category with id 99 was not found"));
    }

    @Test
    void domainValidationProducesBadRequest() throws Exception {
        when(service.getById(7)).thenThrow(new IllegalArgumentException("Invalid product"));
        expectProblem(mvc.perform(get(API + "/7")), 400, API + "/7")
                .andExpect(jsonPath("$.detail").value("Invalid product"));
    }

    @Test
    void unexpectedFailureDoesNotExposeInternalDetails() throws Exception {
        when(service.getById(7)).thenThrow(new IllegalStateException("internal-secret-value"));
        expectProblem(mvc.perform(get(API + "/7")), 500, API + "/7")
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred"))
                .andExpect(content().string(not(containsString("internal-secret-value"))));
    }

    @Test
    void unsupportedContentTypeAndAcceptAreReported() throws Exception {
        expectProblem(mvc.perform(post(API).contentType(MediaType.TEXT_PLAIN).content(VALID)), 415, API);
        expectProblem(mvc.perform(get(API).accept(MediaType.APPLICATION_XML)), 406, API);
        verifyNoInteractions(service);
    }

    @Test
    void unsupportedMethodKeepsAllowHeader() throws Exception {
        expectProblem(mvc.perform(patch(API + "/7").contentType(MediaType.APPLICATION_JSON).content(VALID)), 405, API + "/7")
                .andExpect(header().string("Allow", allOf(containsString("GET"), containsString("PUT"), containsString("DELETE"))));
        verifyNoInteractions(service);
    }

    private ResultActions expectProblem(ResultActions result, int statusCode, String instance) throws Exception {
        return result.andExpect(status().is(statusCode))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").isNotEmpty()).andExpect(jsonPath("$.status").value(statusCode))
                .andExpect(jsonPath("$.detail").isNotEmpty()).andExpect(jsonPath("$.instance").value(instance))
                .andExpect(jsonPath("$.trace").doesNotExist()).andExpect(jsonPath("$.exception").doesNotExist());
    }

    private static Product product() {
        return new Product(7L, "Stellar Treats", "Space food", new BigDecimal("12.50"), 1, 10);
    }
}

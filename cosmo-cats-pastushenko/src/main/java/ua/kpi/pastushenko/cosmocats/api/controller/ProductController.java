package ua.kpi.pastushenko.cosmocats.api.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ua.kpi.pastushenko.cosmocats.api.dto.ProductPageResponse;
import ua.kpi.pastushenko.cosmocats.api.dto.ProductResponse;
import ua.kpi.pastushenko.cosmocats.api.dto.ProductWriteRequest;
import ua.kpi.pastushenko.cosmocats.api.mapper.ProductMapper;
import ua.kpi.pastushenko.cosmocats.application.service.ProductService;

import java.net.URI;

@RestController
@RequestMapping(value = "/api/products", produces = MediaType.APPLICATION_JSON_VALUE)
public class ProductController {

    private final ProductService productService;
    private final ProductMapper productMapper;

    public ProductController(ProductService productService, ProductMapper productMapper) {
        this.productService = productService;
        this.productMapper = productMapper;
    }

    @GetMapping
    public ProductPageResponse findPage(
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(100) int size) {
        var result = productService.findPage(page, size);
        return new ProductPageResponse(
                result.content().stream().map(productMapper::toDto).toList(),
                result.page(), result.size(), result.totalElements(), result.totalPages()
        );
    }

    @GetMapping("/{productId}")
    public ProductResponse getById(@PathVariable("productId") @Positive long productId) {
        return productMapper.toDto(productService.getById(productId));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductWriteRequest request) {
        var created = productService.create(productMapper.toDomain(request));
        String location = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{productId}")
                .buildAndExpand(created.getId())
                .toUri()
                .getRawPath();
        return ResponseEntity.created(URI.create(location))
                .body(productMapper.toDto(created));
    }

    @PutMapping(value = "/{productId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ProductResponse update(@PathVariable("productId") @Positive long productId,
                                  @Valid @RequestBody ProductWriteRequest request) {
        return productMapper.toDto(productService.update(productId, productMapper.toDomain(request)));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> delete(@PathVariable("productId") @Positive long productId) {
        productService.delete(productId);
        return ResponseEntity.noContent().build();
    }
}

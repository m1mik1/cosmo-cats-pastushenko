package ua.kpi.pastushenko.cosmocats.api.controller;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import java.net.URI;
import ua.kpi.pastushenko.cosmocats.api.dto.*;
import ua.kpi.pastushenko.cosmocats.application.service.CategoryService;
import ua.kpi.pastushenko.cosmocats.domain.model.Category;
@RestController
@RequestMapping(value = "/api/categories", produces = MediaType.APPLICATION_JSON_VALUE)
public class CategoryController {
    private final CategoryService service;
    public CategoryController(CategoryService service) { this.service = service; }
    @GetMapping
    public PageResponse<CategoryResponse> findPage(
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(100) int size) {
        return PageResponse.from(service.findPage(page, size), CategoryResponse::from);
    }
    @GetMapping("/{id}")
    public CategoryResponse getById(@PathVariable("id") @Positive long id) { return CategoryResponse.from(service.getById(id)); }
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryWriteRequest request) {
        var created = service.create(new Category(null, request.name()));
        String location = ServletUriComponentsBuilder.fromCurrentRequestUri().path("/{id}").buildAndExpand(created.getId()).toUri().getRawPath();
        return ResponseEntity.created(URI.create(location)).body(CategoryResponse.from(created));
    }
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public CategoryResponse update(@PathVariable("id") @Positive long id, @Valid @RequestBody CategoryWriteRequest request) {
        return CategoryResponse.from(service.update(id, new Category(null, request.name())));
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") @Positive long id) { service.delete(id); return ResponseEntity.noContent().build(); }
}

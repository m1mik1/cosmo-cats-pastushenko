package ua.kpi.pastushenko.cosmocats.api.controller;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import java.net.URI;
import ua.kpi.pastushenko.cosmocats.api.dto.*;
import ua.kpi.pastushenko.cosmocats.application.service.OrderService;
import ua.kpi.pastushenko.cosmocats.domain.model.Category;
@RestController
@RequestMapping(value = "/api/orders", produces = MediaType.APPLICATION_JSON_VALUE)
public class OrderController {
    private final OrderService service;
    public OrderController(OrderService service) { this.service = service; }
    @GetMapping
    public PageResponse<OrderResponse> findPage(
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(100) int size) {
        return PageResponse.from(service.findPage(page, size), OrderResponse::from);
    }
    @GetMapping("/{id}")
    public OrderResponse getById(@PathVariable("id") @Positive long id) { return OrderResponse.from(service.getById(id)); }
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody OrderWriteRequest request) {
        var created = service.create(request.toItems());
        String location = ServletUriComponentsBuilder.fromCurrentRequestUri().path("/{id}").buildAndExpand(created.getId()).toUri().getRawPath();
        return ResponseEntity.created(URI.create(location)).body(OrderResponse.from(created));
    }
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public OrderResponse update(@PathVariable("id") @Positive long id, @Valid @RequestBody OrderWriteRequest request) {
        return OrderResponse.from(service.update(id, request.toItems()));
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") @Positive long id) { service.delete(id); return ResponseEntity.noContent().build(); }
}

package ua.kpi.pastushenko.cosmocats.application.service;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import ua.kpi.pastushenko.cosmocats.domain.model.*;
import ua.kpi.pastushenko.cosmocats.domain.repository.*;
import ua.kpi.pastushenko.cosmocats.application.exception.ResourceNotFoundException;
import java.util.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.math.BigDecimal;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class OrderServiceTest {
    OrderRepository orders = mock(OrderRepository.class);
    ProductRepository products = mock(ProductRepository.class);
    OrderService service = new OrderService(orders, products);
    Product product() { return new Product(1L,"Original Name",null,new BigDecimal("12.50"),1,10); }
    Order order() { return new Order(7L,List.of(new OrderItem(1,"Old Name",new BigDecimal("8.00"),1)),Instant.parse("2026-01-01T00:00:00Z")); }
    @Test void createUsesServerPricesAndMicrosecondTimestamp() {
        when(products.findById(1)).thenReturn(Optional.of(product())); when(orders.create(any())).thenAnswer(i -> i.getArgument(0));
        var before = Instant.now().truncatedTo(ChronoUnit.MICROS);
        var result = service.create(List.of(new CartItem(1,2)));
        assertThat(result.getId()).isNull(); assertThat(result.getCreatedAt()).isBetween(before,Instant.now());
        assertThat(result.getCreatedAt().getNano()%1000).isZero();
        assertThat(result.getItems().getFirst().getProductName()).isEqualTo("Original Name");
        assertThat(result.getTotalAmount()).isEqualByComparingTo("25.00");
    }
    @Test void updatePreservesCreationTimeAndReprices() {
        when(orders.findById(7)).thenReturn(Optional.of(order())); when(products.findById(1)).thenReturn(Optional.of(product()));
        when(orders.update(eq(7L),any())).thenAnswer(i -> Optional.of(i.getArgument(1)));
        var result = service.update(7,List.of(new CartItem(1,3)));
        assertThat(result.getId()).isEqualTo(7); assertThat(result.getCreatedAt()).isEqualTo(order().getCreatedAt());
        assertThat(result.getTotalAmount()).isEqualByComparingTo("37.50");
    }
    @Test void readsPagesAndDeletes() {
        var page = new PageResult<>(List.of(order()),0,20,1);
        when(orders.findPage(new PageRequest(0,20))).thenReturn(page); when(orders.findById(7)).thenReturn(Optional.of(order()));
        when(orders.deleteById(7)).thenReturn(true);
        assertThat(service.findPage(0,20)).isSameAs(page); assertThat(service.getById(7).getId()).isEqualTo(7);
        service.delete(7); verify(orders).deleteById(7);
    }
    @Test void missingOrderOrProductIs404() {
        assertThatThrownBy(() -> service.getById(99)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.update(99,List.of(new CartItem(1,1)))).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.delete(99)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.create(List.of(new CartItem(99,1)))).isInstanceOf(ResourceNotFoundException.class);
        verify(orders,never()).create(any());
    }
    @Test void disappearedOrderDoesNotBecomeInsert() {
        when(orders.findById(7)).thenReturn(Optional.of(order())); when(products.findById(1)).thenReturn(Optional.of(product()));
        assertThatThrownBy(() -> service.update(7,List.of(new CartItem(1,1)))).isInstanceOf(ResourceNotFoundException.class);
        verify(orders,never()).create(any());
    }
    @Test void invalidItemListsNeverWrite() {
        assertThatIllegalArgumentException().isThrownBy(() -> service.create(null));
        assertThatIllegalArgumentException().isThrownBy(() -> service.create(List.of()));
        assertThatIllegalArgumentException().isThrownBy(() -> service.create(Collections.nCopies(101,new CartItem(1,1))));
        assertThatIllegalArgumentException().isThrownBy(() -> service.create(Arrays.asList((CartItem)null)));
        when(products.findById(1)).thenReturn(Optional.of(product()));
        assertThatIllegalArgumentException().isThrownBy(() -> service.create(List.of(new CartItem(1,1),new CartItem(1,2))));
        verifyNoInteractions(orders);
    }
    @ParameterizedTest @ValueSource(longs = {0,-1}) void invalidIds(long id) {
        assertThatIllegalArgumentException().isThrownBy(() -> service.getById(id));
        assertThatIllegalArgumentException().isThrownBy(() -> service.update(id,List.of(new CartItem(1,1))));
        assertThatIllegalArgumentException().isThrownBy(() -> service.delete(id)); verifyNoInteractions(orders,products);
    }
}

package ua.kpi.pastushenko.cosmocats.infrastructure.persistence;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.data.domain.PageImpl;
import ua.kpi.pastushenko.cosmocats.domain.model.*;
import ua.kpi.pastushenko.cosmocats.infrastructure.persistence.entity.*;
import ua.kpi.pastushenko.cosmocats.infrastructure.persistence.repository.*;
import java.util.*;
import java.time.Instant;
import java.math.BigDecimal;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
class JpaCategoryRepositoryTest {
    SpringDataCategoryRepository data = mock(SpringDataCategoryRepository.class);

    JpaCategoryRepository repository = new JpaCategoryRepository(data);
    CategoryEntity category = new CategoryEntity(new Category(1L,"Category Name"));
    ProductEntity product = new ProductEntity(new Product(1L,"Product Name",null,new BigDecimal("12.50"),1,10),category);
    Category value = new Category(7L,"Category Name");
    CategoryEntity entity = new CategoryEntity(value);
    @BeforeEach void setup() {  }
    @Test void createAndReadMapStoredValues() {
        when(data.saveAndFlush(any())).thenReturn(entity); when(data.findById(7L)).thenReturn(Optional.of(entity));
        assertThat(repository.create(value).getId()).isEqualTo(7);
        assertThat(repository.findById(7).orElseThrow().getId()).isEqualTo(7); assertThat(repository.findById(99)).isEmpty(); when(data.existsById(7L)).thenReturn(true); assertThat(repository.existsById(7)).isTrue();
    }
    @Test void paginationHandlesLargeOffsets() {
        when(data.findAll(any(org.springframework.data.domain.Pageable.class))).thenReturn(new PageImpl<>(List.of(entity)));
        var page = repository.findPage(new PageRequest(0,20)); assertThat(page.content()).hasSize(1); assertThat(page.totalElements()).isEqualTo(1);
        when(data.count()).thenReturn(3L); var empty = repository.findPage(new PageRequest(Integer.MAX_VALUE,100));
        assertThat(empty.content()).isEmpty(); assertThat(empty.totalElements()).isEqualTo(3);
    }
    @Test void updateAndDeleteRequireExistingRow() {
        when(data.findForUpdate(7)).thenReturn(Optional.of(entity)); when(data.saveAndFlush(entity)).thenReturn(entity);
        assertThat(repository.update(7,value).orElseThrow().getId()).isEqualTo(7); assertThat(repository.update(99,value)).isEmpty();
        assertThat(repository.deleteById(7)).isTrue(); assertThat(repository.deleteById(99)).isFalse();
        verify(data).delete(entity); verify(data).flush();
    }
}

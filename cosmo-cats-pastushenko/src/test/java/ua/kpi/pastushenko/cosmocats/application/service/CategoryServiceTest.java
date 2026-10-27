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

class CategoryServiceTest {
    CategoryRepository repository = mock(CategoryRepository.class);
    CategoryService service = new CategoryService(repository);
    @Test void readsAndWrites() {
        var value = new Category(7L,"New Category"); var request = new Category(null,"New Category");
        var page = new PageResult<>(List.of(value),0,20,1);
        when(repository.findPage(new PageRequest(0,20))).thenReturn(page);
        when(repository.findById(7)).thenReturn(Optional.of(value)); when(repository.create(request)).thenReturn(value);
        when(repository.update(7,request)).thenReturn(Optional.of(value)); when(repository.update(7,value)).thenReturn(Optional.of(value));
        when(repository.deleteById(7)).thenReturn(true);
        assertThat(service.findPage(0,20)).isSameAs(page); assertThat(service.getById(7)).isSameAs(value);
        assertThat(service.create(request)).isSameAs(value); assertThat(service.update(7,request)).isSameAs(value);
        assertThat(service.update(7,value)).isSameAs(value); service.delete(7); verify(repository).deleteById(7);
    }
    @Test void missingResourcesAre404() {
        assertThatThrownBy(() -> service.getById(9)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.update(9,new Category(null,"Name"))).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.delete(9)).isInstanceOf(ResourceNotFoundException.class);
    }
    @ParameterizedTest @ValueSource(longs = {0,-1}) void invalidIds(long id) {
        assertThatIllegalArgumentException().isThrownBy(() -> service.getById(id));
        assertThatIllegalArgumentException().isThrownBy(() -> service.update(id,new Category(null,"Name")));
        assertThatIllegalArgumentException().isThrownBy(() -> service.delete(id)); verifyNoInteractions(repository);
    }
    @Test void invalidWritesNeverReachRepository() {
        assertThatNullPointerException().isThrownBy(() -> service.create(null));
        assertThatNullPointerException().isThrownBy(() -> service.update(1,null));
        assertThatIllegalArgumentException().isThrownBy(() -> service.create(new Category(1L,"Name")));
        assertThatIllegalArgumentException().isThrownBy(() -> service.update(1,new Category(2L,"Name")));
        assertThatIllegalArgumentException().isThrownBy(() -> service.findPage(-1,20)); verifyNoInteractions(repository);
    }
}

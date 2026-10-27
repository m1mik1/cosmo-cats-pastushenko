package ua.kpi.pastushenko.cosmocats.domain.model;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
class PageResultTest {
    @Test void metadataAndContentsAreSafe() {
        var input = new ArrayList<>(List.of("a")); var page = new PageResult<>(input,0,2,3); input.clear();
        assertThat(page.content()).containsExactly("a"); assertThat(page.totalPages()).isEqualTo(2);
        assertThat(new PageResult<>(List.of(),0,2,0).totalPages()).isZero();
        assertThat(new PageResult<>(List.of(1),0,2,4).totalPages()).isEqualTo(2);
        assertThatIllegalArgumentException().isThrownBy(() -> new PageResult<>(List.of(),0,1,-1));
        assertThatIllegalArgumentException().isThrownBy(() -> new PageResult<>(List.of(1,2),0,1,2));
        assertThatIllegalArgumentException().isThrownBy(() -> new PageResult<>(List.of(1),0,1,0));
    }
}

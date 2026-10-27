package ua.kpi.pastushenko.cosmocats.infrastructure.persistence.repository;
import ua.kpi.pastushenko.cosmocats.infrastructure.persistence.entity.CategoryEntity;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;
public interface SpringDataCategoryRepository extends JpaRepository<CategoryEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from CategoryEntity e where e.id = :id")
    Optional<CategoryEntity> findForUpdate(@Param("id") long id);
}

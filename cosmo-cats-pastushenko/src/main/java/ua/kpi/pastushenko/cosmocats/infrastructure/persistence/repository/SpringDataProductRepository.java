package ua.kpi.pastushenko.cosmocats.infrastructure.persistence.repository;
import ua.kpi.pastushenko.cosmocats.infrastructure.persistence.entity.ProductEntity;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;
public interface SpringDataProductRepository extends JpaRepository<ProductEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from ProductEntity e where e.id = :id")
    Optional<ProductEntity> findForUpdate(@Param("id") long id);
}

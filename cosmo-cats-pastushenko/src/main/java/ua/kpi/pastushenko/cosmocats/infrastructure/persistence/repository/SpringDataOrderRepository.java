package ua.kpi.pastushenko.cosmocats.infrastructure.persistence.repository;
import ua.kpi.pastushenko.cosmocats.infrastructure.persistence.entity.OrderEntity;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;
public interface SpringDataOrderRepository extends JpaRepository<OrderEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from OrderEntity e where e.id = :id")
    Optional<OrderEntity> findForUpdate(@Param("id") long id);
}

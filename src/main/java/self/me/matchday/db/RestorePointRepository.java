package self.me.matchday.db;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import self.me.matchday.model.RestorePoint;

import java.util.UUID;

@Repository
public interface RestorePointRepository extends JpaRepository<RestorePoint, UUID> {}

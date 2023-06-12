package self.me.matchday.db;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import self.me.matchday.model.RestorePoint;

@Repository
public interface RestorePointRepository extends JpaRepository<RestorePoint, UUID> {}

package self.me.matchday.db;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import self.me.matchday.model.EventFileSource;

public interface EventFileSrcRepository extends JpaRepository<EventFileSource, UUID> {

}

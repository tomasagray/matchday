package self.me.matchday.db;

import org.springframework.data.jpa.repository.JpaRepository;
import self.me.matchday.model.EventFile;

public interface EventFileRepository extends JpaRepository<EventFile, Long> {
  // todo: delete me?
}

package self.me.matchday.db;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import self.me.matchday.model.EventFile;

// TODO: Add this annotation to other repos
@Repository
public interface EventFileRepository extends JpaRepository<EventFile, Long> {

}

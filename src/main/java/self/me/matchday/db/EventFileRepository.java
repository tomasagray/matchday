package self.me.matchday.db;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import self.me.matchday.model.EventFile;

@Repository
public interface EventFileRepository extends JpaRepository<EventFile, Long> {

}

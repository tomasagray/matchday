package self.me.matchday.db;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import self.me.matchday.model.Event;

public interface EventRepository extends JpaRepository<Event, String> {

  @Query(value = "SELECT ev FROM Event ev ORDER BY date DESC")
  Optional<List<Event>> fetchLatestEvents(Pageable pageable);
}

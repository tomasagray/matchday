package self.me.matchday.db;

import org.springframework.data.jpa.repository.JpaRepository;
import self.me.matchday.model.Event;

public interface EventRepository extends JpaRepository<Event, Long> {

}

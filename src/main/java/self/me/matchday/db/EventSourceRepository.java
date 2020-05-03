/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.db;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import self.me.matchday.model.EventSource;

@Repository
public interface EventSourceRepository extends JpaRepository<EventSource, Long> {

  @Query("SELECT es FROM EventSource es JOIN es.event ev WHERE ev.eventId = :eventId")
  Optional<List<EventSource>> findSourcesForEvent(@Param("eventId") String eventId);
}

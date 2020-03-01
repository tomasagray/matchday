/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.db;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import self.me.matchday.model.EventSource;

public interface EventSourceRepository extends JpaRepository<EventSource, Long> {

  @Query("SELECT es FROM EventSource es JOIN es.event ev WHERE ev.eventId = :eventId")
  Optional<List<EventSource>> findSourcesForEvent(@Param("eventId") Long eventId);
}

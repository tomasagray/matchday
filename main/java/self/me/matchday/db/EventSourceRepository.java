/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.db;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import self.me.matchday.feed.EventSource;

public interface EventSourceRepository extends JpaRepository<EventSource, Long> {

  @Query("SELECT es FROM EventSource es JOIN es.event ev WHERE ev.eventId = :eventId")
  Optional<EventSource> findSourceByEventId(@Param("eventId") Long eventId);
}

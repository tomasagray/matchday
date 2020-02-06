/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.db;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.web.bind.annotation.PathVariable;
import self.me.matchday.feed.EventFileSource;

public interface EventFileSrcRepository extends JpaRepository<EventFileSource, Long> {

  @Query("SELECT efs FROM EventSource es JOIN es.event ev JOIN es.eventFileSources efs WHERE "
          + "ev.eventId = :eventId AND efs.eventFileSrcId = :eventFileSrcId")
  Optional<EventFileSource> findFileSrcByEventId(
      @PathVariable("eventId") Long eventId, @PathVariable("eventFileSrcId") Long eventFileSrcId);
}

/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.db;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import self.me.matchday.model.EventFileSource;

public interface EventFileSrcRepository extends JpaRepository<EventFileSource, Long> {

  @Query("SELECT efs FROM EventSource es JOIN es.event ev JOIN es.eventFileSources efs "
      + "WHERE efs.eventFileSrcId = :eventFileSrcId")
  Optional<EventFileSource> findFileSrcById(@Param("eventFileSrcId") Long eventFileSrcId);

  @Query("SELECT efs FROM EventSource es JOIN es.event ev JOIN es.eventFileSources efs "
      + "WHERE ev.eventId = :eventId")
  Optional<List<EventFileSource>> findFileSourcesForEventId(@Param("eventId") String eventId);
}

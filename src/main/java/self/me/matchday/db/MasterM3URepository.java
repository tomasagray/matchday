package self.me.matchday.db;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import self.me.matchday.model.MasterM3U;

@Repository
public interface MasterM3URepository extends JpaRepository<MasterM3U, Long> {

  @Query("SELECT pl FROM MasterM3U pl JOIN Event ev ON pl.eventId=ev.eventId WHERE pl.eventId = :eventId")
  Optional<MasterM3U> findByEventId(@Param("eventId") String eventId);
}

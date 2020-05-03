package self.me.matchday.db;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import self.me.matchday.model.Event;

@Repository
public interface EventRepository extends JpaRepository<Event, String> {

  /**
   * Retrieve the latest <i>n</i> Events.
   *
   * @param pageable A Pageable object specifying how many Events to return.
   * @return A list of Events.
   */
  @Query(value = "SELECT ev FROM Event ev ORDER BY date DESC")
  Optional<List<Event>> fetchLatestEvents(Pageable pageable);

  /**
   * Retrieve all Events associated with the specified competition.
   *
   * @param competitionId The ID of the Competition.
   * @return A List of Competitions.
   */
  @Query("SELECT ev FROM Event ev JOIN ev.competition cm WHERE cm.competitionId = :competitionId")
  Optional<List<Event>> fetchEventsByCompetition(@Param("competitionId") String competitionId);

  /**
   * Retrieve all Events associated with the specified Team.
   *
   * @param teamId The ID of the Team.
   * @return A List of Events which include this Team.
   */
  @Query("SELECT mt FROM Match mt JOIN mt.homeTeam ht LEFT JOIN mt.awayTeam at "
      + "WHERE ht.teamId = :teamId OR at.teamId = :teamId")
  Optional<List<Event>> fetchEventsByTeam(@Param("teamId") String teamId);
}

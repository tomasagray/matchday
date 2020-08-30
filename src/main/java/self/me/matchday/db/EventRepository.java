/*
 * Copyright (c) 2020.
 *
 * This file is part of Matchday.
 *
 * Matchday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Matchday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Matchday.  If not, see <http://www.gnu.org/licenses/>.
 */

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

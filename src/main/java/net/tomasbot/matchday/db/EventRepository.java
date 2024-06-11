/*
 * Copyright (c) 2022.
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

package net.tomasbot.matchday.db;

import java.util.Optional;
import java.util.UUID;
import net.tomasbot.matchday.model.Event;
import net.tomasbot.matchday.model.video.VideoFileSource;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {

  /**
   * Retrieve all Events associated with the specified competition.
   *
   * @param competitionId The name of the Competition.
   * @return A List of Competitions.
   */
  @Query(
      "SELECT ev FROM Event ev JOIN ev.competition cm WHERE cm.id = :competitionId "
          + "ORDER BY ev.date DESC")
  Page<Event> fetchEventsByCompetition(
      @Param("competitionId") UUID competitionId, Pageable request);

  @Query("SELECT ev FROM Event ev WHERE :fileSource IN elements(ev.fileSources)")
  Optional<Event> fetchEventForFileSource(@NotNull VideoFileSource fileSource);
}

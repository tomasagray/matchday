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

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import self.me.matchday.model.Team;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeamRepository extends JpaRepository<Team, UUID> {

  Optional<Team> findByName(String name);

  void deleteByName(String name);

  @Query("SELECT DISTINCT ht FROM Match mt JOIN mt.homeTeam ht JOIN mt.competition cm "
      + "WHERE cm.competitionId = :competitionId")
  List<Team> fetchHomeTeamsByCompetition(@Param("competitionId") UUID competitionId);

  @Query("SELECT DISTINCT at FROM Match mt JOIN mt.awayTeam at JOIN mt.competition cm "
      + "WHERE cm.competitionId = :competitionId")
  List<Team> fetchAwayTeamsByCompetition(@Param("competitionId") UUID competitionId);
}

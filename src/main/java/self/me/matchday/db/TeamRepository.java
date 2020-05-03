/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.db;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import self.me.matchday.model.Team;

@Repository
public interface TeamRepository extends JpaRepository<Team, String> {

  @Query("SELECT DISTINCT ht FROM Match mt JOIN mt.homeTeam ht JOIN mt.competition cm "
      + "WHERE cm.competitionId = :competitionId")
  List<Team> fetchHomeTeamsByCompetition(@Param("competitionId") String competitionId);

  @Query("SELECT DISTINCT at FROM Match mt JOIN mt.awayTeam at JOIN mt.competition cm "
      + "WHERE cm.competitionId = :competitionId")
  List<Team> fetchAwayTeamsByCompetition(@Param("competitionId") String competitionId);
}

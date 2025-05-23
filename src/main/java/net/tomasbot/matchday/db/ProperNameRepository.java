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

import java.util.List;
import java.util.Optional;
import net.tomasbot.matchday.model.ProperName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProperNameRepository extends JpaRepository<ProperName, Long> {

  List<ProperName> findProperNameByName(@Param("name") String name);

  @Query(
      "SELECT pn FROM ProperName pn WHERE :synonym = pn.name OR :synonym IN "
          + "(SELECT sy.name FROM Synonym sy WHERE sy IN elements(pn.synonyms))")
  Optional<ProperName> findProperNameForSynonym(@Param("synonym") String synonym);
}

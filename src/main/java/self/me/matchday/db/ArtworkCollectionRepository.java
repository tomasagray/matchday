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

package self.me.matchday.db;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import self.me.matchday.model.Artwork;
import self.me.matchday.model.ArtworkCollection;

public interface ArtworkCollectionRepository extends JpaRepository<ArtworkCollection, Long> {

  @Query("SELECT ac FROM ArtworkCollection ac WHERE :artwork IN elements(ac.collection)")
  Optional<ArtworkCollection> getCollectionForArtwork(Artwork artwork);
}

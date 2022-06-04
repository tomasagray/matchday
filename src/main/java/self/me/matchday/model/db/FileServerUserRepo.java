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

package self.me.matchday.model.db;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import self.me.matchday.model.FileServerUser;

import java.util.List;
import java.util.UUID;

@Repository
public interface FileServerUserRepo extends JpaRepository<FileServerUser, UUID> {

  @Query("SELECT user FROM FileServerUser user WHERE user.serverId = :serverId")
  List<FileServerUser> fetchAllUsersForServer(@Param("serverId") UUID serverId);

  @Query(
      "SELECT user FROM FileServerUser user WHERE user.serverId = :serverId AND user.loggedIn = true")
  List<FileServerUser> fetchLoggedInUsersForServer(@Param("serverId") UUID serverId);
}

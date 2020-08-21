package self.me.matchday.db;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import self.me.matchday.plugin.fileserver.FileServerUser;

@Repository
public interface FileServerUserRepo extends JpaRepository<FileServerUser, String> {

  @Query("SELECT user FROM FileServerUser user WHERE user.serverId = :serverId")
  Optional<List<FileServerUser>> fetchAllUsersForServer(@Param("serverId") String serverId);

  @Query("SELECT user FROM FileServerUser user WHERE user.serverId = :serverId AND user.loggedIn = true")
  Optional<List<FileServerUser>> fetchLoggedInUsersForServer(@Param("serverId") String serverId);

}

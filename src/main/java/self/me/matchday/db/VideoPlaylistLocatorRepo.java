package self.me.matchday.db;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import self.me.matchday.model.VideoStreamPlaylistLocator;
import self.me.matchday.model.VideoStreamPlaylistLocator.VideoStreamPlaylistId;

@Repository
public interface VideoPlaylistLocatorRepo extends
    JpaRepository<VideoStreamPlaylistLocator, VideoStreamPlaylistId> {

}

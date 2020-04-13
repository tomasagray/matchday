package self.me.matchday.db;

import org.springframework.data.jpa.repository.JpaRepository;
import self.me.matchday.model.Artwork;

public interface ArtworkRepository extends JpaRepository<Artwork, Long> {

}

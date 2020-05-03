package self.me.matchday.db;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import self.me.matchday.model.Artwork;

@Repository
public interface ArtworkRepository extends JpaRepository<Artwork, Long> {

}

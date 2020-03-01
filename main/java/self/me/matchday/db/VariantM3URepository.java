package self.me.matchday.db;

import org.springframework.data.jpa.repository.JpaRepository;
import self.me.matchday.model.VariantM3U;

public interface VariantM3URepository extends JpaRepository<VariantM3U, Long> {

  // TODO: delete this repo?
}

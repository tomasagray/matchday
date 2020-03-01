package self.me.matchday.db;

import org.springframework.data.jpa.repository.JpaRepository;
import self.me.matchday.model.MasterM3U;

public interface MasterM3URepository extends JpaRepository<MasterM3U, Long> {

}

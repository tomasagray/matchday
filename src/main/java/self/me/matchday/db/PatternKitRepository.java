package self.me.matchday.db;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import self.me.matchday.model.PatternKit;

@Repository
public interface PatternKitRepository extends JpaRepository<PatternKit<?>, Long> {}

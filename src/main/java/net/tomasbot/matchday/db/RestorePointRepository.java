package net.tomasbot.matchday.db;

import java.util.UUID;
import net.tomasbot.matchday.model.RestorePoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RestorePointRepository extends JpaRepository<RestorePoint, UUID> {}

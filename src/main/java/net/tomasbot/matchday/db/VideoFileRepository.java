package net.tomasbot.matchday.db;

import java.util.UUID;
import net.tomasbot.matchday.model.video.VideoFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VideoFileRepository extends JpaRepository<VideoFile, UUID> {}

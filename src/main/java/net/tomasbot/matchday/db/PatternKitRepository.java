package net.tomasbot.matchday.db;

import net.tomasbot.matchday.model.PatternKit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PatternKitRepository extends JpaRepository<PatternKit<?>, Long> {}

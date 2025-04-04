package net.tomasbot.matchday.db;

import net.tomasbot.matchday.model.SanityReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SanityReportRepository extends JpaRepository<SanityReport, UUID> {}

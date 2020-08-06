package self.me.matchday.startup;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import self.me.matchday.api.service.EventService;
import self.me.matchday.model.Competition;
import self.me.matchday.model.EventFile;
import self.me.matchday.model.EventFile.EventPartIdentifier;
import self.me.matchday.model.EventFileSource;
import self.me.matchday.model.EventFileSource.Resolution;
import self.me.matchday.model.FileSize;
import self.me.matchday.model.Fixture;
import self.me.matchday.model.Match;
import self.me.matchday.model.Season;
import self.me.matchday.model.Team;
import self.me.matchday.util.Log;

@Configuration
public class LoadTestEvent {

  @Bean
  CommandLineRunner loadEvent(EventService eventService) {
    return args -> {

      final URL firstHalfUrl = new URL(
          "http://192.168.0.101/stream2stream/rm-atleti-2019/1st_half.ts");
      final URL secondHalfUrl = new URL(
          "http://192.168.0.101/stream2stream/rm-atleti-2019/2nd_half.ts");

      final List<EventFileSource> fileSources = new ArrayList<>();
      for (int i = 0; i < 3; i++) {
        // Create EventFiles
        final EventFile firstHalf = new EventFile(EventPartIdentifier.FIRST_HALF, firstHalfUrl);
        firstHalf.setInternalUrl(firstHalfUrl);
        firstHalf.setEventFileId((long) (Math.random() * 10_000L));
        final EventFile secondHalf = new EventFile(EventPartIdentifier.SECOND_HALF, secondHalfUrl);
        secondHalf.setInternalUrl(secondHalfUrl);
        secondHalf.setEventFileId((long) (Math.random() * 10_000L));
        final List<EventFile> eventFiles = new ArrayList<>();
        eventFiles.add(firstHalf);
        eventFiles.add(secondHalf);

        // Create file source
        final EventFileSource eventFileSource =
            EventFileSource
                .builder()
                .eventFileSrcId(UUID.randomUUID())
                .fileSize(FileSize.ofGigabytes(6))
                .eventFiles(eventFiles)
                .resolution(Resolution.R_1080p)
                .bitrate(10_000L)
                .languages(List.of("English", "English"))
                .build();
        fileSources.add(eventFileSource);
      }
      // Create Event
      final Team team = new Team("Test Team");
      final Competition competition = new Competition("Test Competition");
      final Season season = new Season();
      final Fixture fixture = new Fixture();
      final Match event = new Match(team, team, competition, season, fixture, LocalDateTime.now());

      // Add multiple file sources
      event.addFileSources(fileSources);

      // Save to DB
      eventService.saveEvent(event);
      Log.i("LoadTestEvent", "Saved test event to repo");
    };
  }
}

package self.me.matchday.api.service.admin;

import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import self.me.matchday.api.service.*;
import self.me.matchday.db.FileServerUserRepo;
import self.me.matchday.model.*;
import self.me.matchday.model.validation.EventValidator;
import self.me.matchday.util.JsonParser;

@Service
@Transactional
public class HydrationService {

  private static final Type TYPE = new TypeReference<SystemImage>() {}.getType();
  private static final String FILENAME = "matchday_dehydrated_%s.json";

  private final EventValidator eventValidator;
  private final MatchService matchService;
  private final CompetitionService competitionService;
  private final TeamService teamService;
  private final DataSourceService dataSourceService;
  private final FileServerUserService userService;
  private final FileServerUserRepo fileServerUserRepo;

  public HydrationService(
      EventValidator eventValidator,
      MatchService matchService,
      CompetitionService competitionService,
      TeamService teamService,
      DataSourceService dataSourceService,
      FileServerUserService userService,
      FileServerUserRepo fileServerUserRepo) {
    this.eventValidator = eventValidator;
    this.matchService = matchService;
    this.competitionService = competitionService;
    this.teamService = teamService;
    this.dataSourceService = dataSourceService;
    this.userService = userService;
    this.fileServerUserRepo = fileServerUserRepo;
  }

  @NotNull
  public Path dehydrate(@NotNull Path to) throws IOException {
    if (!to.toFile().isDirectory()) {
      throw new IllegalArgumentException("Path is not a directory: " + to);
    }
    String json = JsonParser.toJson(dehydrate(), TYPE);
    String filename = String.format(FILENAME, Instant.now().toEpochMilli());
    Path jsonFile = to.resolve(filename);
    Files.writeString(jsonFile, json, StandardOpenOption.CREATE_NEW);
    return jsonFile;
  }

  public SystemImage dehydrate() {
    return createSystemImage();
  }

  public SystemImage createSystemImage() {
    List<Match> events = matchService.fetchAll();
    List<Competition> competitions = competitionService.fetchAll();
    List<Team> teams = teamService.fetchAll();
    List<FileServerUser> users = userService.getAllUsers();
    List<PlaintextDataSource<?>> dataSources =
        dataSourceService.fetchAll().stream()
            .map(source -> (PlaintextDataSource<?>) source)
            .collect(Collectors.toList());
    return SystemImage.of()
        .events(events)
        .competitions(competitions)
        .teams(teams)
        .fileServerUsers(users)
        .dataSources(dataSources)
        .build();
  }

  public void rehydrate(@NotNull Path from) throws IOException {
    String json = Files.readString(from);
    SystemImage systemImage = JsonParser.fromJson(json, TYPE);
    rehydrate(systemImage);
  }

  public void rehydrate(@NotNull SystemImage systemImage) {
    // ensure system is empty before rehydrating
    SystemImage currentImage = createSystemImage();
    validateEmptySystemImage(currentImage);

    List<Match> events = systemImage.getEvents();
    eventValidator.validateAll(events); // fail early
    matchService.saveAll(events);
    fileServerUserRepo.saveAll(systemImage.getFileServerUsers());
    dataSourceService.saveAll(systemImage.getDataSources());
  }

  private void validateEmptySystemImage(@NotNull SystemImage image) {
    int eventCount = image.getEvents().size();
    int competitionCount = image.getCompetitions().size();
    int teamCount = image.getTeams().size();
    int dataSourceCount = image.getDataSources().size();
    int fileServerUserCount = image.getFileServerUsers().size();
    int total = eventCount + competitionCount + teamCount + dataSourceCount + fileServerUserCount;
    if (total != 0) {
      String counts =
          String.format(
              "[event: %d, competitions: %d, teams: %d, data sources: %d, fileserver users: %d]",
              eventCount, competitionCount, teamCount, dataSourceCount, fileServerUserCount);
      throw new IllegalStateException("Cannot rehydrate: system is not fresh! " + counts);
    }
  }

  @Data
  @Builder(builderMethodName = "of")
  public static class SystemImage {
    List<Match> events;
    List<Competition> competitions;
    List<Team> teams;
    List<FileServerUser> fileServerUsers;
    List<PlaintextDataSource<?>> dataSources;
  }
}

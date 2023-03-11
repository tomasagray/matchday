package self.me.matchday.api.service.admin;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Builder;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import self.me.matchday.api.service.*;
import self.me.matchday.db.FileServerUserRepo;
import self.me.matchday.model.*;
import self.me.matchday.util.JsonParser;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class HydrationService {

    private static final Type TYPE = new TypeReference<SystemImage>() {}.getType();
    private static final String FILENAME = "matchday_dehydrated_%s.json";

    private final MatchService matchService;
    private final CompetitionService competitionService;
    private final TeamService teamService;
    private final DataSourceService dataSourceService;
    private final PatternKitTemplateService templateService;
    private final FileServerUserService userService;
    private final FileServerUserRepo fileServerUserRepo;

    public HydrationService(
            MatchService matchService,
            CompetitionService competitionService,
            TeamService teamService,
            DataSourceService dataSourceService,
            FileServerUserService userService,
            PatternKitTemplateService templateService, FileServerUserRepo fileServerUserRepo) {
        this.matchService = matchService;
        this.competitionService = competitionService;
        this.teamService = teamService;
        this.dataSourceService = dataSourceService;
        this.userService = userService;
        this.templateService = templateService;
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
                dataSourceService.fetchAll()
                        .stream()
                        .map(source -> (PlaintextDataSource<?>)source)
                        .collect(Collectors.toList());
        List<PatternKitTemplate> templates = templateService.fetchAll();

        return SystemImage.of()
                .events(events)
                .competitions(competitions)
                .teams(teams)
                .users(users)
                .dataSources(dataSources)
                .templates(templates)
                .build();
    }

    public void rehydrate(@NotNull Path from) throws IOException {
        String json = Files.readString(from);
        SystemImage systemImage = JsonParser.fromJson(json, TYPE);
        rehydrate(systemImage);
    }

    public void rehydrate(@NotNull SystemImage systemImage) {
        matchService.saveAll(systemImage.getEvents());
        fileServerUserRepo.saveAll(systemImage.getUsers());
        dataSourceService.saveAll(systemImage.dataSources);
        systemImage.getTemplates().forEach(templateService::save);
    }

    @Data
    @Builder(builderMethodName = "of")
    public static class SystemImage {
        List<Match> events;
        List<Competition> competitions;
        List<Team> teams;
        List<FileServerUser> users;
        List<PlaintextDataSource<?>> dataSources;
        List<PatternKitTemplate> templates;
    }
}

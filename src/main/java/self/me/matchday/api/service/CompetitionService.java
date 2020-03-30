package self.me.matchday.api.service;

import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.stereotype.Service;
import self.me.matchday.api.resource.CompetitionResource;
import self.me.matchday.api.resource.CompetitionResource.CompetitionResourceAssembler;
import self.me.matchday.db.CompetitionRepository;
import self.me.matchday.model.Competition;
import self.me.matchday.util.Log;

@Service
public class CompetitionService {

  private static final String LOG_TAG = "CompetitionService";

  private final CompetitionRepository competitionRepository;
  private final CompetitionResourceAssembler competitionResourceAssembler;

  @Autowired
  public CompetitionService(CompetitionRepository competitionRepository,
      CompetitionResourceAssembler competitionResourceAssembler) {

    this.competitionRepository = competitionRepository;
    this.competitionResourceAssembler = competitionResourceAssembler;
  }

  /**
   * Fetch all Competitions in the database.
   *
   * @return A CollectionModel of Competition resources.
   */
  public Optional<CollectionModel<CompetitionResource>> fetchAllCompetitions() {

    Log.i(LOG_TAG, "Retrieving all Competitions from database.");
    final List<Competition> competitions = competitionRepository.findAll();
    if (competitions.size() > 0) {
      return Optional.of(competitionResourceAssembler.toCollectionModel(competitions));
    } else {
      Log.i(LOG_TAG, "Attempted to fetch all Competitions, but none returned");
      return Optional.empty();
    }
  }

  /**
   * Fetch a specific Competition from the database.
   *
   * @param competitionId The ID of the desired Competition.
   * @return The Competition as a resource.
   */
  public Optional<CompetitionResource> fetchCompetitionById(@NotNull String competitionId) {

    Log.i(LOG_TAG, String.format("Fetching competition with ID: %s from database.", competitionId));
    return
        competitionRepository
            .findById(competitionId)
            .map(competitionResourceAssembler::toModel);
  }
}

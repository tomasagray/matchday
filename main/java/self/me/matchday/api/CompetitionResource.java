package self.me.matchday.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import self.me.matchday.model.Competition;
import org.springframework.hateoas.RepresentationModel;

public class CompetitionResource extends RepresentationModel<CompetitionResource> {

  private final Competition competition;

  @JsonCreator
  public CompetitionResource(@JsonProperty("competition") Competition competition) {
    this.competition = competition;
  }

  public Competition getCompetition() {
    return competition;
  }

}

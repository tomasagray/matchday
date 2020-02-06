package self.me.matchday.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.hateoas.RepresentationModel;
import self.me.matchday.model.HighlightShow;

public class HighlightShowResource extends RepresentationModel<HighlightShowResource> {

  private final HighlightShow highlightShow;

  @JsonCreator
  public HighlightShowResource(@JsonProperty("highlightShow") final HighlightShow highlightShow) {
    this.highlightShow = highlightShow;
  }

  public HighlightShow getHighlightShow() {
    return highlightShow;
  }

}

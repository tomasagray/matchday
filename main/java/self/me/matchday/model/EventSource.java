/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.model;

import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a source for a given Event. Events can have multiple Sources - for example, ESPN, BBC
 * and NBC all broadcast the same Match.
 */
@Data
@Entity
@NoArgsConstructor
public final class EventSource {

  @Id
  @GeneratedValue
  private Long eventSourceId;
  @ManyToOne(cascade = CascadeType.MERGE)
  private Event event;
  @OneToMany(cascade = CascadeType.ALL)
  private List<EventFileSource> eventFileSources;

  public EventSource(@NotNull final Event event, @NotNull final List<EventFileSource> eventFileSources) {
    this.event = event;
    this.eventFileSources = eventFileSources;
  }
}

/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.model;

import java.util.List;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import lombok.Data;

/**
 * Represents a source for a given Event. Events can have multiple Sources - for example, ESPN, BBC
 * and NBC all broadcast the same Match.
 */
@Data
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class EventSource {

  @Id
  @GeneratedValue
  private Long eventSourceId;
  protected String link;

  /**
   * Return the Event this source provides.
   *
   * @return An Event
   */
  public abstract Event getEvent();

  /**
   * Returns the file resources for this Event.
   *
   * @return An Event file resource
   */
  public abstract List<EventFileSource> getEventFileSources();

}

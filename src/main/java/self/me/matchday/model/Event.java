/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;
import lombok.Data;

/**
 * A sporting Event; could be a Match (game), highlight show, trophy celebration, group selection,
 * or ... ?
 */
@Data
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Event {

  protected static final DateTimeFormatter EVENT_ID_DATE_FORMATTER =
      DateTimeFormatter.ISO_LOCAL_DATE;

  @Id @GeneratedValue private Long eventId; // for internal database reference

  @ManyToOne(cascade = {CascadeType.MERGE})
  protected Competition competition;

  @ManyToOne(cascade = CascadeType.MERGE)
  protected Season season;

  @ManyToOne(cascade = CascadeType.MERGE)
  protected Fixture fixture;

  @Column(name = "title")
  protected String title;

  @Column(name = "date")
  protected LocalDateTime date;
}

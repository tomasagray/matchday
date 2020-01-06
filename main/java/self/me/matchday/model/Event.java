/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.model;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * A sporting Event; could be a Match (game), highlight show, trophy celebration, group selection,
 * or ... ?
 */
@Data
public abstract class Event {

  protected LocalDateTime date;
  protected String title;

}

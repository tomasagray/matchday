/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.db;

import org.springframework.data.jpa.repository.JpaRepository;
import self.me.matchday.model.HighlightShow;

public interface HighlightShowRepository extends JpaRepository<HighlightShow, Long> {

}

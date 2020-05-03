/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.db;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import self.me.matchday.model.HighlightShow;

@Repository
public interface HighlightShowRepository extends JpaRepository<HighlightShow, String> {

}

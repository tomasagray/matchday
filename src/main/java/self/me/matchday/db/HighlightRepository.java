/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.db;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import self.me.matchday.model.Highlight;

@Repository
public interface HighlightRepository extends JpaRepository<Highlight, String> {

}
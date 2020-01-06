/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

/*
 *  All code written by Tomás Gray unless otherwise noted.
 *  May not be reproduced without written consent from the above.
 */
package self.me.matchday.feed.blogger.galataman;

import java.util.function.Predicate;
import java.util.regex.Pattern;
import org.jsoup.nodes.Element;
import self.me.matchday.fileserver.inclouddrive.ICDData;

/**
 * Collection class of Patterns specific to the Galataman HDF Blog.
 */
class GalatamanPattern {
  // Entry parsing patterns
  static final String START_OF_SOURCE = Pattern.compile("__*").pattern();

  static final String METADATA_ITEM_DELIMITER =
      Pattern.compile("<span style=\"color: blue;\">(\\[)?").pattern();

  static final String METADATA_KV_DELIMITER =
      Pattern.compile("(])?</span>:(<span [^>]*>)?").pattern();

  static final String LANGUAGE_DELIMITER = Pattern.compile("[\\d.* ]|/").pattern();

  static final String AV_DATA_DELIMITER = Pattern.compile("‖").pattern();

  // Predicates
  static final Predicate<Element> isSourceData =
      elem -> ("b".equals(elem.tagName())) && (elem.text().contains("Channel"));

  static final Predicate<Element> isVideoLink =
      elem ->
          ("a".equals(elem.tagName()))
              && (elem.attr("href").startsWith(ICDData.getFileUrlStub()));
}
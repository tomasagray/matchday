/*
 *  All code written by Tomás Gray unless otherwise noted.
 *  May not be reproduced without written consent from the above.
 */
package self.me.matchday;

/**
 * Contains fields and methods needed by other tests.
 * 
 * @author tomas
 */
public class MatchDayTest 
{
    // Remote sources
    // -------------------------------------------------------------------------
    public static final String REMOTE_KNOWN_GOOD_HTML 
            = "http://10.0.0.5/soccer/testing/galataman_post.html";
    public static final String REMOTE_KNOWN_GOOD_JSON
            = "http://10.0.0.5/soccer/testing/galataman.json";
    public static final String REMOTE_MISSING_DATA
            = "http://10.0.0.5/soccer/testing/galataman_damaged.json";
    public static final String EMPTY_SET
            = "http://10.0.0.5/soccer/testing/gman_beautified_no_entries.json";
    public static final String REMOTE_CONTEMPORARY_JSON
            = "http://galatamanhdf.blogspot.com/feeds/posts/default/?alt=json";
//            = "http://10.0.0.5/soccer/testing/download.json";
    public static final String NON_BLOGGER_JSON
            = "https://daringfireball.net/feeds/json";
    
    // Local sources
    // -------------------------------------------------------------------------
    public static final String LOCAL_KNOWN_GOOD_JSON
            = "src/test/resources/self/me/matchday/io/galataman.json";
    public static final String LOCAL_INVALID_JSON
            = "src/test/resources/self/me/matchday/io/gman_beautified_INVALID.json";
    public static final String MISSING_DATA_JSON
            = "src/test/resources/self/me/matchday/io/missing_data.json";
    public static final String LOCAL_KNOWN_GOOD_HTML
           = "src/test/resources/self/me/matchday/io/gman_post_beautified.html";
    // Image
    public static final String IMAGE_FILE
            = "src/test/resources/hasselhoff.jpg";
    
}

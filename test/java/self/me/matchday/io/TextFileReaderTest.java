/*
 *  All code written by Tomás Gray unless otherwise noted.
 *  May not be reproduced without written consent from the above.
 */
package self.me.matchday.io;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import self.me.matchday.util.Log;

import java.io.IOException;
import java.net.URL;

/**
 *
 * @author tomas
 */
class TextFileReaderTest
{
    private static final String LOG_TAG = "TextFileReaderTest";


    @Test
    @Tag("GENERAL")
    @DisplayName("Ensure TextFileReader throws an IOException when trying to read from invalid source.")
    void verifyHandlesBadURLTest()
    {
        // The bad URLs
        String[] badURLs = { 
            "http://ww.nothing.com", "http://&&&www.google.com/",
            "http:www.google.com/", "dddddd", "http://",
            "http://www.google.com/"
            
        };
        
        for(String url : badURLs)
        {
            // Ensure each generates an exception
            try {
                Log.d(LOG_TAG, "Testing: " + url);
                // Attempt to read non-existent data
                String nothing = TextFileReader.readRemote( new URL(url) );

            } catch(Exception e) {
                Log.d(LOG_TAG, "Caught exception type: " + e.getClass().getCanonicalName() );
                // Make sure each one throws an IO exception
                Assertions.assertTrue( e instanceof  IOException );
            }
        }
    }
}

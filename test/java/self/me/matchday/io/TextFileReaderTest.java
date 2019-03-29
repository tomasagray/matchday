/*
 *  All code written by Tomás Gray unless otherwise noted.
 *  May not be reproduced without written consent from the above.
 */
package self.me.matchday.io;

import org.junit.jupiter.api.Assertions;
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
                Log.i(LOG_TAG, "Testing: " + url);

                String nothing = TextFileReader.readRemote( new URL(url) );
            } catch(Exception e) {
                // Make sure each one throws an IO exception
                Assertions.assertTrue( e instanceof  IOException );
            }
        }
    }
    
    @Test
    @Tag("ERRORS")
    void verifyHandlesImageTest()
    {
        String imgURL = "https://images-na.ssl-images-amazon.com/images/I/51Oe1gyY85L.jpg";
        try {
            Log.i(LOG_TAG, "Testing: " + imgURL);
            // Try to read an image remotely
            String hasselhoff = TextFileReader.readRemote( new URL(imgURL) );
            // Should not execute
            Log.e(LOG_TAG, "This should not be! " + hasselhoff);
        } catch(IOException e) {
            Log.e(LOG_TAG, "Could not read test data\n" + e.getMessage());
        }
    }
}

/*
 *  All code written by Tomás Gray unless otherwise noted.
 *  May not be reproduced without written consent from the above.
 */
package self.me.matchday.io;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import self.me.matchday.MatchDayTest;
import self.me.matchday.util.Log;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author tomas
 */
class JsonStreamReaderTest
{
    private static final String LOG_TAG = "JsonStreamReaderTest";

    
    @Test
    @Tag("GENERAL")
    void verifyHandlesValidJSONTest() throws IOException
    {        
        // Good JSON - should not throw exception
        JsonObject localJson = 
                JsonStreamReader.readLocal(
                        Paths.get(MatchDayTest.LOCAL_KNOWN_GOOD_JSON)
                );
        
        JsonObject remoteJson 
                = JsonStreamReader.readRemote( 
                        new URL(MatchDayTest.REMOTE_CONTEMPORARY_JSON)
                    );
        
        // Tests
        assertFalse( localJson.entrySet().isEmpty() );
        assertFalse( remoteJson.entrySet().isEmpty() );
    }
    
    
    @ParameterizedTest
    @Tag("GENERAL")
    @ValueSource( strings = { MatchDayTest.LOCAL_INVALID_JSON })
    void verifyHandlesInvalidJSONTest(String candidate)
    {
        Log.i(LOG_TAG, "Testing: " + candidate);
        
        try {
            // Bad JSON - should throw JsonSyntaxException
            JsonObject badJson = JsonStreamReader.readLocal( Paths.get(candidate) );
            Log.d(LOG_TAG, "This should NOT be printed!!!" + badJson.getAsString());
            
            // Should not be thrown
            throw new NullPointerException();
        } catch(Exception e) {
            Log.d(LOG_TAG, "Caught exception: " + e.getMessage());
            assertTrue( e instanceof JsonSyntaxException );
        }
    }
}

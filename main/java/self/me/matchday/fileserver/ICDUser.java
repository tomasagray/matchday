package self.me.matchday.fileserver;

import org.jetbrains.annotations.NotNull;
import self.me.matchday.util.Log;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.StringJoiner;

public class ICDUser extends FSUser
{
    private static final String LOG_TAG = "ICDUser";



    // Constructors
    // ------------------------------------------------
    public ICDUser(String userName) {
        super(userName);
    }

    @NotNull
    @Override
    public byte[] getLoginDataByteArray()
    {
        // Container for data
        StringJoiner sj = new StringJoiner("&" );

        // Encode each data item and add it to the login
        // data String container
        sj.add( getURLComponent("email", this.getUserName()) );
        sj.add( getURLComponent("password", this.getPassword()) );
        sj.add( getURLComponent( "keep", this.isKeepLoggedIn()) );

        // Assemble and return String data as a byte array
        return sj.toString()
                .getBytes( StandardCharsets.UTF_8 );
    }

    private String getURLComponent(String key, Object value)
    {
        StringBuilder sb = new StringBuilder();
        String charset = StandardCharsets.UTF_8.toString();

        try {
            sb
                .append( URLEncoder.encode( key, charset) )
                .append("=")
                .append( URLEncoder.encode( value.toString(), charset ) );

        } catch (UnsupportedEncodingException e) {
            Log.e(LOG_TAG, e.getMessage());
        }

        return sb.toString();
    }
}

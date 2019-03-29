/*
 *  All code written by Tomás Gray unless otherwise noted.
 *  May not be reproduced without written consent from the above.
 */
package self.me.matchday.model;

import self.me.matchday.util.Log;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Wrapper class for MD5 hash strings.
 *
 * @author tomas
 */
public class MD5 
{
    private String hash;
    
    public MD5( String str )
    {
        try 
        {    
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest( str.getBytes() ); 

            // Convert byte array into signum representation 
            BigInteger no = new BigInteger(1, messageDigest); 

            // Convert message digest into hex value 
            StringBuilder hashText = new StringBuilder(no.toString(16));
            while (hashText.length() < 32) {
                hashText.insert(0, "0");
            }   
            
            this.hash = hashText.toString();
            
        } 
        catch( NoSuchAlgorithmException e ) {
            Log.e(
                    "MD5_Class",
                    "ERROR: NoSuchAlgorithmException thrown for MD5 of:\n" + str,
                    e
            );
        }
    }
    
    public String getHash()
    {
        return this.hash;
    }
}

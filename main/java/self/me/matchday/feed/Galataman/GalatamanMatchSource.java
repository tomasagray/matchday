/*
 *  All code written by Tomás Gray unless otherwise noted.
 *  May not be reproduced without written consent from the above.
 */
package self.me.matchday.feed.Galataman;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.model.InvalidMetadataException;
import self.me.matchday.model.MetadataTuple;
import self.me.matchday.util.Log;

import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

import static self.me.matchday.feed.Galataman.GalatamanPattern.*;

/**
 * Represents a specific source for a match (for example: 1080i, Spanish),
 * derived from the Galataman blog (http://galatamanhdf.blogspot.com/)
 *
 * @author tomas
 */
public final class GalatamanMatchSource
{
    private static final String LOG_TAG = "GManMatchSource";

    
    // Fields
    // -------------------------------------------------------------------------
    private final String metadataStr;
    private final String channel;
    private final String source;
    private final List<String> languages;
    private final List<String> videoData;
    private final List<String> audioData;
    private final String duration;
    private final String size;
    private final String release;
    private final List<URL> urls;

    // Constructor
    // -------------------------------------------------------------------------
    private GalatamanMatchSource(GalatamanMatchSourceBuilder builder)
    {
        // Initialize fields
        this.metadataStr = builder.metadataStr;
        this.channel = builder.channel;
        this.source = builder.source;
        this.duration = builder.duration;
        this.size = builder.size;
        this.release = builder.release;

        // Initialize immutable List fields
        this.languages = Collections.unmodifiableList( builder.languages );
        this.videoData = Collections.unmodifiableList( builder.videoData );
        this.audioData = Collections.unmodifiableList( builder.audioData );
        this.urls      = Collections.unmodifiableList( builder.urls );
    }
    // Getters
    // -------------------------------------------------------------------------
    @Contract(pure = true)
    public String getMetadataStr() {
        return metadataStr;
    }
    @Contract(pure = true)
    public String getChannel() {
        return channel;
    }
    @Contract(pure = true)
    public String getSource() {
        return source;
    }
    @Contract(pure = true)
    public List<String> getLanguages() {
        return languages;
    }
    @Contract(pure = true)
    public List<String> getVideoData() {
        return videoData;
    }
    @Contract(pure = true)
    public List<String> getAudioData() {
        return audioData;
    }
    @Contract(pure = true)
    public String getDuration() {
        return duration;
    }
    @Contract(pure = true)
    public String getSize() {
        return size;
    }
    @Contract(pure = true)
    public String getRelease(){
        return this.release;
    }
    @Contract(pure = true)
    public List<URL> getURLs() {
        return this.urls;
    }


    // Overridden methods
    // -------------------------------------------------------------------------
    @NotNull
    @Contract(pure = true)
    @Override
    public String toString()
    {
        return
                "\tSource:\n\t[\n"
                        + "\t\tChannel: " + this.channel + "\n"
                        + "\t\tSource: " + this.source + "\n"
                        + "\t\tLanguages: " + this.languages + "\n"
                        + "\t\tVideo: " + this.videoData + "\n"
                        + "\t\tAudio: " + this.audioData + "\n"
                        + "\t\tDuration: " + this.duration + "\n"
                        + "\t\tSize: " + this.size + "\n"
                        + "\t\tRelease: " + this.release + "\n"
                        + "\t\tURLS: " + this.urls + "\n"
                        + "\t]\n";
    }

    public static final class GalatamanMatchSourceBuilder
    {
        // Fields
        // -------------------------------------------------------------------------
        private final String metadataStr;
        private String channel;
        private String source;
        private final List<String> languages = new ArrayList<>();
        private final List<String> videoData = new ArrayList<>();
        private final List<String> audioData = new ArrayList<>();
        private String duration;
        private String size;
        private String release;
        private final List<URL> urls;

        // Constructor
        // -------------------------------------------------------------------------
        GalatamanMatchSourceBuilder(@NotNull String matchDataHTML, List<URL> urls)
        {
            // Save raw metadata
            this.metadataStr = matchDataHTML;
            this.urls = urls;

            // Cleanup HTML, removing superfluous &nbsp;
            String cleanedHTML = matchDataHTML.replace("&nbsp;", "");

            // Parse data
            parseDataItems( cleanedHTML )
                    // Parse each data item
                    .forEach( this::parseDataItem );
        }


        /**
         * Break the match data string apart into parse-able chunks, and collect
         * them into a List.
         *
         * @param data A String containing raw HTML to be parsed into metadata items
         *             concerning a particular source.
         *
         * @return A List of key/value tuples, each representing a metadata item
         *
         */
        private List<MetadataTuple> parseDataItems(@NotNull String data)
        {
            return
                Arrays.stream(
                    // Break apart stream into individual data items...
                    data.split( METADATA_ITEM_DELIMITER )
                )
                // ... eliminating any empty entries ...
                .filter((item) ->
                    !("".equals(item))
                ) // ... convert to a tuple.
                .map(
                    (String item) ->
                    {
                        try {
                            return new MetadataTuple(item, METADATA_KV_DELIMITER);
                        } catch (InvalidMetadataException e) {
                            Log.d(LOG_TAG, "Invalid metadata: " + e.getMessage() );
                            throw new GalatamanPostParseException(
                                    "Galataman data corrupted:\n" + this.metadataStr,
                                    e
                            );
                        }
                    }
                )
                .collect( Collectors.toList() );
        }

        /**
         * Examines a key/value pair to determine if they relate to a
         * relevant metadata item (e.g., Channel, Source, etc.)
         *
         * @param kv A key/value pair containing data about this source.
         */
        private void parseDataItem(@NotNull MetadataTuple kv)
        {
            // Get the key
            String key = kv.getKeyString();
            // Clean up the value
            String value = clean( kv.getValueString() );

            // Examine the key and assign the value to the correct
            // metadata field
            switch( key )
            {
                case "CHANNEL":
                    this.channel = value;
                    break;
                case "SOURCE":
                    this.source = value;
                    break;
                case "LANGUAGE":
                case "COMMENTARY":
                    this.languages.addAll( parseLanguages(value) );
                    break;
                case "VIDEO":
                    this.videoData.addAll( parseAVData(value) );
                    break;
                case "AUDIO":
                    this.audioData.addAll( parseAVData(value) );
                    break;
                case "DURATION":
                    this.duration = value;
                    break;
                case "SIZE":
                    this.size = value;
                    break;
                case "RELEASE":
                    this.release = value;
                    break;
                default:
                    throw new GalatamanPostParseException(
                            "INVALID KEY/VALUE: " + kv.toString()
                    );
            }
        }

        /**
         * Break apart a String into language names.
         *
         * @param langStr A String containing language names, separated
         *                by a delimiter configured in the GalatamanPost
         *                class.
         */
        private List<String> parseLanguages(@NotNull String langStr)
        {
            // Split string
            List<String> languages =
                    new ArrayList<>(
                            Arrays.asList( langStr.split(
                                    LANGUAGE_DELIMITER
                            ) )
                    );
            // Remove empty entries
            languages.removeIf((lang) -> "".equals( lang.trim() ));

            return languages;
        }

        /**
         * Split video data string apart.
         *
         * @param avData A String containing audio/video data items, separated
         *                  by a delimiter configured in the GalatamanPost
         *                  class.
         */
        private List<String> parseAVData(@NotNull String avData)
        {
            // Temporary container
            List<String> dataItems = new ArrayList<>();

            // Split the string into data items
            for( String dataItem : avData.split( AV_DATA_DELIMITER ) )
            {
                // Remove empty space
                String trim = dataItem.trim();
                if( !("".equals(trim)) )
                    // Add data to object
                    dataItems.add(trim);
            }

            return dataItems;
        }

        /**
         * Removes any remaining tags and special characters
         *  e.g., <br>, &nbsp;, etc.
         *
         * @param input A String in need of cleaning.
         *
         * @return The cleaned String.
         */
        @NotNull
        private String clean(@NotNull String input)
        {
            return input.replaceAll(
                        "<[^>]*>", ""
                    ).trim();
        }

        @NotNull
        @Contract(" -> new")
        public GalatamanMatchSource build()
        {
            return new GalatamanMatchSource(this);
        }

    }
}

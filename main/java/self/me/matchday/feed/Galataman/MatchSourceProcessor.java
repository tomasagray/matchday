package self.me.matchday.feed.Galataman;

import java.net.URL;
import java.util.List;

public final class MatchSourceProcessor implements IMatchSourceProcessor
{
    public GalatamanMatchSource parse(String html, List<URL> urls)
    {
        return
                new GalatamanMatchSource
                        .GalatamanMatchSourceBuilder( html, urls )
                        .build();
    }
}

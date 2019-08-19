package self.me.matchday.feed.galataman;

import java.net.URL;
import java.util.List;

public interface IMatchSourceProcessor
{
    GalatamanMatchSource parse(String html, List<URL> urls);
}

package self.me.matchday.feed.Galataman;

import java.net.URL;
import java.util.List;

public interface IMatchSourceProcessor
{
    GalatamanMatchSource parse(String html, List<URL> urls);
}

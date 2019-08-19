package self.me.matchday.feed.galataman;

import com.google.gson.JsonObject;
import self.me.matchday.feed.IBloggerPostProcessor;

public final class GalatamanPostProcessor implements IBloggerPostProcessor
{
    public GalatamanPost parse(JsonObject entry)
    {
        return
                new GalatamanPost
                        .GalatamanPostBuilder(
                                entry,
                                new MatchSourceProcessor()
                    )
                    .build();
    }
}

package self.me.matchday.plugin.datasource.forum;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import self.me.matchday.model.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class EventListParser {

    private final String linkSelector;
    private final Pattern matchPattern;
    private final DateTimeFormatter dateFormatter;

    public EventListParser(ForumPluginProperties pluginProperties) {
        this.linkSelector = pluginProperties.getLinkSelector();
        this.matchPattern = Pattern.compile(pluginProperties.getMatchPattern());
        this.dateFormatter = DateTimeFormatter.ofPattern(pluginProperties.getDateFormatter());
    }

    private static int parseYear(@NotNull String year) {
        int y = Integer.parseInt(year);
        if (y < 100) {
            y += 2_000;
        }
        return y;
    }

    public Map<URL, ? extends Event> getEventsList(@NotNull String data) {
        Document document = Jsoup.parse(data);
        Elements links = document.select(this.linkSelector);
        return links.stream().collect(Collectors.toMap(this::getLinkHref, this::parseMatchLink, (e1, e2) -> e1));
    }

    private @Nullable URL getLinkHref(@NotNull Element link) {
        try {
            return new URL(link.attr("href"));
        } catch (MalformedURLException e) {
           return null;
        }
    }

    private Match parseMatchLink(@NotNull Element link) {
        final String text = link.text();
        final Matcher matcher = this.matchPattern.matcher(text);
        if (matcher.find()) {
            String competitionName = matcher.group(1);
            String seasonData = matcher.group(2);
            String fixtureData = matcher.group(3);
            String homeTeamName = matcher.group(4);
            String awayTeamName = matcher.group(5);
            String dateData = matcher.group(6);
            LocalDateTime date = LocalDate.parse(dateData, this.dateFormatter).atStartOfDay();

            return Match.builder()
                    .competition(new Competition(competitionName))
                    .homeTeam(new Team(homeTeamName))
                    .awayTeam(new Team(awayTeamName))
                    .season(parseSeason(seasonData))
                    .fixture(new Fixture(fixtureData))
                    .date(date)
                    .build();
        }
        return new Match();
    }

    @Contract("_ -> new")
    private @NotNull Season parseSeason(@NotNull String seasonData) {
        String[] years = seasonData.split("/");
        if (years.length != 2) {
            return new Season();
        }
        int startYear = parseYear(years[0]);
        int endYear = parseYear(years[1]);
        return new Season(startYear, endYear);
    }
}

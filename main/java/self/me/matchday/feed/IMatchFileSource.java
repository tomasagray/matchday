package self.me.matchday.feed;

import java.net.URL;
import java.util.List;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public interface IMatchFileSource {
  String getChannel();
  String getSource();
  List<String> getLanguages();
  String getDuration();
  String getSize();
  Resolution getResolution();
  List<URL> getUrls();

  enum Resolution {
    R_4k("4K"),
    R_1080p("1080p"),
    R_1080i("1080i"),
    R_720p("720p"),
    R_576p("576p"),
    R_SD("SD");

    private String name;
    @Contract(pure = true)
    Resolution(@NotNull String name) {
      this.name = name;
    }

    @Contract(pure = true)
    @Override
    public String toString() {
      return this.name;
    }
  }
}

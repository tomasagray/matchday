package self.me.matchday.api.service.admin;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.util.StringUtils;

public class ConditionalStatementResourcePopulator extends ResourceDatabasePopulator {

  private static final Pattern conditionalPatten =
      Pattern.compile("/\\*!(\\d{5})([\\w\\s`@_:+=%',]+)\\*/");

  private final int versionNum;
  private String sqlScriptEncoding;

  public ConditionalStatementResourcePopulator(int versionNum) {
    super();
    this.versionNum = versionNum;
  }

  public ConditionalStatementResourcePopulator(int versionNum, Resource... resources)
      throws IOException {
    this(versionNum);
    setScripts(parseResources(resources));
  }

  @Override
  public void setSqlScriptEncoding(String sqlScriptEncoding) {
    super.setSqlScriptEncoding(sqlScriptEncoding);
    this.sqlScriptEncoding = StringUtils.hasText(sqlScriptEncoding) ? sqlScriptEncoding : null;
  }

  @Contract(pure = true)
  private Resource @NotNull [] parseResources(Resource @NotNull ... resources) throws IOException {

    final List<Resource> parsed = new ArrayList<>();
    for (Resource resource : resources) {
      Resource cleanResource = cleanResource(resource);
      parsed.add(cleanResource);
    }
    return parsed.toArray(new Resource[0]);
  }

  @Contract("_ -> new")
  private @NotNull Resource cleanResource(@NotNull Resource resource) throws IOException {

    final EncodedResource encodedResource = new EncodedResource(resource, this.sqlScriptEncoding);
    try (final BufferedReader reader = new BufferedReader(encodedResource.getReader())) {
      final String cleanedResource =
          reader
              .lines()
              .map(
                  line -> {
                    final Matcher matcher = conditionalPatten.matcher(line);
                    String cleaned = line;
                    while (matcher.find() && isLesserVersion(matcher.group(1))) {
                      cleaned = cleaned.replace(matcher.group(), matcher.group(2));
                    }
                    return cleaned;
                  })
              .collect(Collectors.joining("\n"));
      byte[] bytes = cleanedResource.getBytes(StandardCharsets.UTF_8);
      return new ByteArrayResource(bytes);
    }
  }

  private boolean isLesserVersion(String version) {
    return Integer.parseInt(version) <= this.versionNum;
  }
}

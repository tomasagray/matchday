/*
 * Copyright (c) 2022.
 *
 * This file is part of Matchday.
 *
 * Matchday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Matchday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Matchday.  If not, see <http://www.gnu.org/licenses/>.
 */

package self.me.matchday.startup;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import self.me.matchday.api.service.PatternKitTemplateService;
import self.me.matchday.model.Match;
import self.me.matchday.model.PatternKitTemplate;
import self.me.matchday.model.video.VideoFile;
import self.me.matchday.model.video.VideoFileSource;

import java.net.URI;
import java.util.Optional;

@Component
public class LoadPatternKitTemplates implements CommandLineRunner {

  private static final Logger logger = LogManager.getLogger(LoadPatternKitTemplates.class);

  private final PatternKitTemplateService templateService;

  LoadPatternKitTemplates(PatternKitTemplateService templateService) {
    this.templateService = templateService;
  }

  @Override
  public void run(String... args) {

    logger.info("Checking for existence of [Match] PatternKitTemplate...");
    final Optional<PatternKitTemplate> templateOptional =
        templateService.fetchByClassName(Match.class.getSimpleName());
    if (templateOptional.isEmpty()) {
      logger.info("No Template found for Type: [Match]; loading...");
      final PatternKitTemplate template = readMatchTemplate();
      logger.info(
          "Read Template for Match; saved to database as: {}", templateService.save(template));
    } else {
      logger.info("Template already loaded for Type: [Match]");
    }
  }

  PatternKitTemplate readMatchTemplate() {

    logger.info("Reading PatternKitTemplate(s) for: Match");

    final PatternKitTemplate template = new PatternKitTemplate(Match.class);
    template.addFields("competition", "homeTeam", "awayTeam", "season", "fixture", "date");
    template.addRelatedTemplate(readVideoFileSourceTemplate());
    template.addRelatedTemplate(readVideoFileTemplate());
    template.addRelatedTemplate(readUrlTemplate());
    return template;
  }

  PatternKitTemplate readVideoFileSourceTemplate() {
    logger.info("Reading PatternKitTemplate for: VideoFileSource");
    final PatternKitTemplate template = new PatternKitTemplate(VideoFileSource.class);
    template.addFields(
        "channel",
        "source",
        "languages",
        "videoBitrate",
        "videoCodec",
        "mediaContainer",
        "framerate",
        "audioBitrate",
        "audioCodec",
        "audioChannels",
        "approximateDuration",
        "filesize",
        "resolution");
    return template;
  }

  PatternKitTemplate readVideoFileTemplate() {
    logger.info("Reading PatternKitTemplate for: VideoFile");
    final PatternKitTemplate template = new PatternKitTemplate(VideoFile.class);
    template.addFields("title");
    return template;
  }

  PatternKitTemplate readUrlTemplate() {

    logger.info("Reading PatternKitTemplate for: URI");
    final PatternKitTemplate template = new PatternKitTemplate(URI.class);
    template.addFields("url");
    return template;
  }
}

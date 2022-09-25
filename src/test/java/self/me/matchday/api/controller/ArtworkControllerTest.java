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

package self.me.matchday.api.controller;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import self.me.matchday.TestDataCreator;
import self.me.matchday.model.Competition;
import self.me.matchday.util.ResourceFileReader;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension.class)
@DisplayName("Validation for the ArtworkController REST API endpoints")
class ArtworkControllerTest {

  private static final Logger logger = LogManager.getLogger(ArtworkControllerTest.class);
  private static final List<String> deleteList = new ArrayList<>();
  private final Competition testCompetition;
  private final MockMvc mvc;
  @LocalServerPort private int port;

  @Autowired
  ArtworkControllerTest(
      @NotNull TestDataCreator testDataCreator, CompetitionController competitionController) {
    // create test data
    testCompetition = testDataCreator.createTestCompetition("ArtworkControllerTest");
    mvc = MockMvcBuilders.standaloneSetup(competitionController).build();
  }

  @AfterAll
  static void cleanup() throws IOException {
    logger.info("Deleting images created by test...");
    for (String file : deleteList) {
      logger.info("Deleting: {}", file);
      Files.delete(Path.of(file));
    }
  }

  private static void addFileToDeleteList(String json) throws JSONException {

    logger.info("Parsing JSON: {}", json);
    final JSONObject jsonObject = new JSONObject(json);
    final JSONArray content = jsonObject.getJSONArray("content");
    logger.info("Found: {} objects in 'content'", content.length());

    for (int i = 0; i < content.length(); i++) {
      final JSONObject o = content.getJSONObject(i);
      final String file = o.getString("file");
      deleteList.add(file);
    }
  }

  @Test
  @Order(1)
  @DisplayName("Validate uploading image to Competition emblem collection")
  void addCompetitionEmblem() throws Exception {
    // given
    final byte[] uploadImage = ResourceFileReader.readBinaryData("data/TestUploadImage.png");
    logger.info("Read: {} bytes for test upload image", uploadImage.length);
    final String uri = getEmblemUri(testCompetition.getId());
    final MockMultipartFile multipartFile =
        new MockMultipartFile("image", "", "image/png", uploadImage);

    // when
    final MvcResult result =
        mvc.perform(MockMvcRequestBuilders.multipart(uri).file(multipartFile)).andReturn();

    final MockHttpServletResponse response = result.getResponse();
    final int actualStatus = response.getStatus();
    final String content = response.getContentAsString();
    logger.info("Got status: {}", actualStatus);
    logger.info("Got response: {}", content);
    // todo - cleanup images created during test
    //    addFileToDeleteList(content);

    // then
    assertThat(actualStatus).isEqualTo(200);
  }

  private String getEmblemUri(UUID id) {
    return String.format("http://localhost:%d/competitions/competition/%s/EMBLEM", port, id);
  }

  @Test
  @Order(2)
  @DisplayName("Validate retrieval of ArtworkCollection for Competition emblem")
  void fetchCompetitionEmblem() throws Exception {

    // given
    final String uri = getEmblemUri(testCompetition.getId());
    logger.info("Getting Emblem collection for Competition: {}", testCompetition);

    // when
    final MvcResult result = mvc.perform(MockMvcRequestBuilders.get(uri)).andReturn();
    final MockHttpServletResponse response = result.getResponse();
    final int actualStatus = response.getStatus();
    final String content = response.getContentAsString();
    logger.info("Got response: [{}] {}", actualStatus, content);

    // then
    assertThat(actualStatus).isEqualTo(200);
    assertThat(content).isNotNull().isNotEmpty();
  }
}

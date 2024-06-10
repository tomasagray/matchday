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

package self.me.matchday.unit.api.controller;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import self.me.matchday.TestDataCreator;
import self.me.matchday.api.controller.CompetitionController;
import self.me.matchday.model.Competition;
import self.me.matchday.util.ResourceFileReader;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension.class)
@DisplayName("Ensure Artwork can be added, retrieved and deleted via API")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ArtworkLifeCycleTest {

  private static final Logger logger = LogManager.getLogger(ArtworkLifeCycleTest.class);

  private static final List<Long> deleteList = new ArrayList<>();
  private final Competition testCompetition;
  private final MockMvc mvc;
  @LocalServerPort private int port;

  @Autowired
  ArtworkLifeCycleTest(
      @NotNull TestDataCreator testDataCreator, CompetitionController competitionController) {
    // create test data
    testCompetition = testDataCreator.createTestCompetition("ArtworkLifeCycleTest");
    mvc = MockMvcBuilders.standaloneSetup(competitionController).build();
  }

  private static void updateFilesToDeleteList(String json) throws JSONException {
    final JSONObject jsonObject = new JSONObject(json);
    final Long artworkId = jsonObject.getLong("id");
    logger.info("Adding Artwork ID: {} to delete list", artworkId);
    deleteList.add(artworkId);
  }

  private String getEmblemCollectionUri(UUID id) {
    return String.format("http://localhost:%d/competitions/competition/%s/EMBLEM", port, id);
  }

  @Contract(pure = true)
  private @NotNull String getAddEmblemUri(UUID id) {
    return getEmblemCollectionUri(id) + "/add";
  }

  private String getRemoveEmblemUri(UUID id, Long artworkId) {
    return String.format(
        "http://localhost:%d/competitions/competition/%s/EMBLEM/%d/remove", port, id, artworkId);
  }

  @Test
  @Order(1)
  @DisplayName("Validate uploading image to Competition emblem collection")
  void addCompetitionEmblem() throws Exception {
    // given
    final byte[] uploadImage = ResourceFileReader.readBinaryData("data/TestUploadImage.png");
    logger.info("Read: {} bytes for test upload image", uploadImage.length);
    final String addUri = getAddEmblemUri(testCompetition.getId());
    final MockMultipartFile multipartFile =
        new MockMultipartFile("image", "", "image/png", uploadImage);

    // when
    logger.info("Uploading Artwork to: {}", addUri);
    final MvcResult result =
        mvc.perform(MockMvcRequestBuilders.multipart(addUri).file(multipartFile)).andReturn();

    final MockHttpServletResponse response = result.getResponse();
    final int actualStatus = response.getStatus();
    final String content = response.getContentAsString();
    logger.info("Got status: {}", actualStatus);
    logger.info("Got response: {}", content);
    updateFilesToDeleteList(content);

    // then
    assertThat(actualStatus).isEqualTo(200);
  }

  @Test
  @Order(2)
  @DisplayName("Validate retrieval of ArtworkCollection for Competition emblem")
  void fetchCompetitionEmblem() throws Exception {
    // given
    final String getUri = getEmblemCollectionUri(testCompetition.getId());
    logger.info("Getting Emblem collection for Competition: {}", testCompetition);
    logger.info("Getting emblem collection from URL: {}", getUri);

    // when
    final MvcResult result = mvc.perform(MockMvcRequestBuilders.get(getUri)).andReturn();
    final MockHttpServletResponse response = result.getResponse();
    final int actualStatus = response.getStatus();
    final String content = response.getContentAsString();
    logger.info("Got emblem: [{}] {}", actualStatus, content);

    // then
    assertThat(actualStatus).isEqualTo(200);
    assertThat(content).isNotNull().isNotEmpty();
  }

  @Test
  @Order(3)
  @DisplayName("Validate deleting Artwork with file data via API")
  void deleteArtwork() throws Exception {
    assertThat(deleteList).isNotEmpty();
    for (final Long artworkId : deleteList) {
      // given
      final String removeUri = getRemoveEmblemUri(testCompetition.getId(), artworkId);
      logger.info("Removing artwork using URL: {}", removeUri);

      // when
      final MvcResult result = mvc.perform(MockMvcRequestBuilders.delete(removeUri)).andReturn();
      final MockHttpServletResponse response = result.getResponse();
      final int actualStatus = response.getStatus();
      final String content = response.getContentAsString();
      logger.info("Got response: [{}] {}", actualStatus, content);

      // then
      assertThat(actualStatus).isEqualTo(200);
      assertThat(content).isNotNull().isNotEmpty();
    }
  }
}

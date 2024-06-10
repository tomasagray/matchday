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

package self.me.matchday.integration.api.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import self.me.matchday.TestDataCreator;
import self.me.matchday.api.service.DataSourcePluginService;
import self.me.matchday.api.service.DataSourceService;
import self.me.matchday.api.service.EntityServiceRegistry;
import self.me.matchday.api.service.EventService;
import self.me.matchday.model.*;
import self.me.matchday.unit.plugin.datasource.blogger.BloggerTestEntity;
import self.me.matchday.unit.plugin.datasource.blogger.BloggerTestEntityService;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Testing for all data source refresh service")
@TestMethodOrder(OrderAnnotation.class)
class DataSourceServiceTest {

    private static final Logger logger = LogManager.getLogger(DataSourceServiceTest.class);

    private final TestDataCreator testDataCreator;
    private final DataSourceService dataSourceService;
    private final EventService eventService;
    private final EntityServiceRegistry serviceRegistry;
    private final BloggerTestEntityService testEntityService;

    @Autowired
    public DataSourceServiceTest(
            DataSourceService dataSourceService,
            DataSourcePluginService pluginService,
            TestDataCreator testDataCreator,
            EventService eventService,
            EntityServiceRegistry serviceRegistry,
            BloggerTestEntityService testEntityService) {
        this.dataSourceService = dataSourceService;
        this.testDataCreator = testDataCreator;
        this.eventService = eventService;
        this.serviceRegistry = serviceRegistry;
        this.testEntityService = testEntityService;
        // enable Blogger plugin
        pluginService.enablePlugin(UUID.fromString("64d08bc8-bd9f-11ea-b3de-0242ac130004"));
    }

    @Test
    @DisplayName("Validate that a DataSource can be added to the database")
    @Order(1)
    @SuppressWarnings("unchecked cast")
    void addDataSource() throws IOException {
        final List<DataSource<?>> preliminaryDataSources = dataSourceService.fetchAll();
        logger.info("Before adding, there are: {} DataSources", preliminaryDataSources.size());
        logger.info("Preliminary DataSources:\n{}", preliminaryDataSources);

        final PlaintextDataSource<?> testDataSource =
                (PlaintextDataSource<?>) testDataCreator.readTestLiveDataSource();
        logger.info("Read test DataSource:\n{}", testDataSource);
        final List<PatternKit<?>> testPatternKitPack = testDataSource.getPatternKits();
        assertThat(testPatternKitPack).isNotNull();

        final DataSource<Event> addedDataSource =
                (DataSource<Event>) dataSourceService.save(testDataSource);
        logger.info("Added DataSource to database:\n{}", addedDataSource);

        assertThat(addedDataSource).isNotNull();
        assertThat(addedDataSource.getBaseUri()).isEqualTo(testDataSource.getBaseUri());
        final PlaintextDataSource<Event> plaintextDataSource =
                (PlaintextDataSource<Event>) addedDataSource;
        final List<PatternKit<?>> patternKitPack = plaintextDataSource.getPatternKits();
        assertThat(patternKitPack).isNotNull();
        final List<PatternKit<? extends Event>> eventPatternKits =
                plaintextDataSource.getPatternKitsFor(Event.class);
        assertThat(eventPatternKits).isNotNull();
        assertThat(eventPatternKits.size())
                .isEqualTo(testDataSource.getPatternKitsFor(Event.class).size());
    }

    @Test
    @DisplayName("Refresh all data sources")
    @Order(2)
    @Disabled
    void refreshAllDataSources() throws IOException {
        final int expectedEventCount = 1;

        final SnapshotRequest testRequest = SnapshotRequest.builder().build();
        logger.info("Testing Data Source Service refresh with Snapshot Request:\n{}", testRequest);

        final SnapshotRequest testResult = dataSourceService.refreshAllDataSources(testRequest);
        assertThat(testResult).isEqualTo(testRequest);

        // Ensure some data was collected by request
        final List<Event> events = eventService.fetchAll();
        final int actualEventCount = events.size();
        assertThat(actualEventCount).isGreaterThanOrEqualTo(expectedEventCount);
    }

    @Test
    @DisplayName("Validate refreshing DataSource on URL")
    @Order(3)
    void testUrlRefresh() throws IOException {
        // given
        DataSource<BloggerTestEntity> dataSource = testDataCreator.readTestBloggerDataSource();
        dataSourceService.save(dataSource);
        serviceRegistry.registerService(BloggerTestEntity.class, testEntityService);
        URL testUrl = new URL("https://mdbloggertest1.blogspot.com/2023/09/post-9.html");

        // when
        logger.info("Refreshing against URL: {}", testUrl);
        dataSourceService.refreshOnUrl(testUrl);

        // then
        List<BloggerTestEntity> entities = testEntityService.fetchAll();
        logger.info("After URL refresh, entities are: {}", entities);
        assertThat(entities).isNotNull();
        assertThat(entities.size()).isNotZero();
    }
}

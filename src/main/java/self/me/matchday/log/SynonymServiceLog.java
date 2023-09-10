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

package self.me.matchday.log;

import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.api.service.SynonymService;
import self.me.matchday.model.Synonym;

@Aspect
public class SynonymServiceLog {

  private static final Logger logger = LogManager.getLogger(SynonymService.class);

  @Before("execution(* self.me.matchday.api.service.SynonymService.initialize(..))")
  public void logInitializeSynonym(@NotNull JoinPoint jp) {
    logger.debug("Initializing Synonym: {}", jp.getArgs()[0]);
  }

  @Before("execution(* self.me.matchday.api.service.SynonymService.save(..))")
  public void logSaveSynonym(@NotNull JoinPoint jp) {
    logger.info("Saving Synonym: {}", jp.getArgs()[0]);
  }

  @SuppressWarnings("unchecked cast")
  @Before("execution(* self.me.matchday.api.service.SynonymService.saveAll(..))")
  public void logSaveAllSynonyms(@NotNull JoinPoint jp) {
    final List<Synonym> synonyms = (List<Synonym>) jp.getArgs()[0];
    logger.info("Saving: {} Synonyms...", synonyms.size());
  }

  @Before("execution(* self.me.matchday.api.service.SynonymService.addProperName(..))")
  public void logAddProperName(@NotNull JoinPoint jp) {
    logger.info("Adding new ProperName: {}", jp.getArgs()[0]);
  }

  @Before("execution(* self.me.matchday.api.service.SynonymService.fetchById(..))")
  public void logFetchById(@NotNull JoinPoint jp) {
    logger.debug("Fetching Synonym with ID: {}", jp.getArgs()[0]);
  }

  @Before("execution(* self.me.matchday.api.service.SynonymService.fetchAll())")
  public void logFetchAllSynonyms() {
    logger.debug("Fetching ALL synonyms...");
  }

  @Before("execution(* self.me.matchday.api.service.SynonymService.fetchByName(..))")
  public void logFetchByName(@NotNull JoinPoint jp) {
    logger.debug("Fetching Synonym by name: {}", jp.getArgs()[0]);
  }

  @Before("execution(* self.me.matchday.api.service.SynonymService.fetchProperNameBySynonym(..))")
  public void logFetchProperName(@NotNull JoinPoint jp) {
    logger.debug("Fetching ProperName for: {}", jp.getArgs()[0]);
  }

  @Before("execution(* self.me.matchday.api.service.SynonymService.fetchSynonymsFor(..))")
  public void logFetchSynonyms(@NotNull JoinPoint jp) {
    logger.debug("Fetching all Synonyms for: {}", jp.getArgs()[0]);
  }

  @Before("execution(* self.me.matchday.api.service.SynonymService.update(..))")
  public void logUpdateSynonym(@NotNull JoinPoint jp) {
    logger.info("Updating Synonym: {}", jp.getArgs()[0]);
  }

  @SuppressWarnings("unchecked cast")
  @Before("execution(* self.me.matchday.api.service.SynonymService.updateAll(..))")
  public void logUpdateAllSynonyms(@NotNull JoinPoint jp) {
    final Iterable<Synonym> synonyms = (Iterable<Synonym>) jp.getArgs()[0];
    final long synonymCount = synonyms.spliterator().estimateSize();
    logger.info("Updating: {} Synonyms...", synonymCount);
    logger.debug("Synonyms to update: {}", synonyms);
  }

  @Before("execution(* self.me.matchday.api.service.SynonymService.delete(..))")
  public void logDeleteSynonym(@NotNull JoinPoint jp) {
    logger.info("Deleting Synonym with ID: {}", jp.getArgs()[0]);
  }

  @SuppressWarnings("unchecked cast")
  @Before("execution(* self.me.matchday.api.service.SynonymService.deleteAll(..))")
  public void logDeleteAllSynonyms(@NotNull JoinPoint jp) {
    final Iterable<Synonym> synonyms = (Iterable<Synonym>) jp.getArgs()[0];
    final long synonymCount = synonyms.spliterator().estimateSize();
    logger.info("Deleting {} Synonyms: {}", synonymCount, synonyms);
  }
}

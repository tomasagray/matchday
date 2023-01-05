/*
 * Copyright (c) 2023.
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

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import self.me.matchday.api.resource.SettingsResource;
import self.me.matchday.api.resource.SettingsResource.SettingsResourceModeller;
import self.me.matchday.api.service.SettingsService;
import self.me.matchday.model.Settings;

@RestController
@RequestMapping("/settings")
public class SettingsController {

  private final SettingsService settingsService;
  private final SettingsResourceModeller modeller;

  public SettingsController(SettingsService settingsService, SettingsResourceModeller modeller) {
    this.settingsService = settingsService;
    this.modeller = modeller;
  }

  @RequestMapping(
      value = {"", "/"},
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<SettingsResource> getSettings() {
    final Settings settings = settingsService.getSettings();
    return ResponseEntity.ok(modeller.toModel(settings));
  }

  @RequestMapping(
      value = "/update",
      method = RequestMethod.POST,
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<SettingsResource> updateSettings(@RequestBody Settings settings) {
    final Settings updated = settingsService.updateSettings(settings);
    return ResponseEntity.ok(modeller.toModel(updated));
  }
}

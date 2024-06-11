/*
 * Copyright (c) 2021.
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

package net.tomasbot.matchday.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.List;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

@ToString
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SnapshotRequest {

  private static final String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";

  @JsonFormat(pattern = DATE_PATTERN)
  @DateTimeFormat(pattern = DATE_PATTERN)
  private LocalDateTime endDate;

  @JsonFormat(pattern = DATE_PATTERN)
  @DateTimeFormat(pattern = DATE_PATTERN)
  private LocalDateTime startDate;

  private boolean fetchBodies;
  private boolean fetchImages;
  private int maxResults;
  private List<String> labels;
  private String orderBy;
  private String pageToken;
  private String status;
}

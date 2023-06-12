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

package self.me.matchday.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javax.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.Type;
import org.jetbrains.annotations.NotNull;

@Getter
@Setter
@Entity
public class PatternKitTemplate {

  @Type(type = "java.lang.Class")
  private final Class<?> type;

  private final String name;

  @ElementCollection(fetch = FetchType.EAGER)
  private final List<Field> fields = new ArrayList<>();

  @OneToMany(cascade = CascadeType.ALL)
  @LazyCollection(LazyCollectionOption.FALSE)
  private final List<PatternKitTemplate> relatedTemplates = new ArrayList<>();

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  public PatternKitTemplate() {
    this.type = null;
    this.name = null;
  }

  public PatternKitTemplate(@NotNull Class<?> type) {
    this.type = type;
    this.name = type.getSimpleName();
  }

  public void addField(@NotNull String fieldName, boolean required) {
    final boolean present =
        this.fields.stream().map(Field::getFieldName).anyMatch(name -> name.equals(fieldName));
    if (!present) {
      this.fields.add(new Field(fieldName, required));
    }
  }

  public void addFields(@NotNull String... fields) {
    Arrays.stream(fields).map(field -> new Field(field, false)).forEach(this.fields::add);
  }

  public void addRelatedTemplate(@NotNull PatternKitTemplate template) {
    this.relatedTemplates.add(template);
  }

  public void addAllTemplates(@NotNull Collection<PatternKitTemplate> templates) {
    this.relatedTemplates.addAll(templates);
  }

  @Override
  public String toString() {
    return String.format("PatternKitTemplate{id=%s, type=%s}", getId(), getType().getName());
  }

  @Data
  @AllArgsConstructor
  @Embeddable
  public static class Field {
    private final String fieldName;
    private final Boolean required;

    public Field() {
      this.fieldName = null;
      this.required = null;
    }
  }
}

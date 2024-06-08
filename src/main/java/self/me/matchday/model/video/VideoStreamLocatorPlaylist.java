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

package self.me.matchday.model.video;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.db.converter.PathConverter;

@Getter
@Setter
@Entity
public class VideoStreamLocatorPlaylist {

    @ManyToOne(cascade = {CascadeType.MERGE, CascadeType.REFRESH})
    private final VideoFileSource fileSource;

    @OneToMany(cascade = CascadeType.ALL)
    @LazyCollection(LazyCollectionOption.FALSE)
    private final List<VideoStreamLocator> streamLocators = new ArrayList<>();

    @Convert(converter = PathConverter.class)
    private final Path storageLocation;

    @EqualsAndHashCode.Exclude
    private final Instant timestamp = Instant.now();
    @Id
    @GeneratedValue
    private Long id;

    @OneToOne(cascade = CascadeType.ALL)
    private TaskListState state = TaskListState.builder().build();

    public VideoStreamLocatorPlaylist() {
        this.fileSource = null;
        this.storageLocation = null;
    }

    public VideoStreamLocatorPlaylist(@NotNull  VideoFileSource fileSource, @NotNull  Path storageLocation) {
        this.fileSource = fileSource;
        this.storageLocation = storageLocation;
    }

    public void addStreamLocator(@NotNull final VideoStreamLocator streamLocator) {
        this.streamLocators.add(streamLocator);
        this.state.addTaskState(streamLocator.getState());
    }

    public TaskListState getState() {
        // ensure state is fresh
        this.state.computeState();
        return this.state;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VideoStreamLocatorPlaylist playlist)) return false;
        return Objects.equals(getFileSource(), playlist.getFileSource())
                && Objects.equals(getStreamLocators(), playlist.getStreamLocators())
                && Objects.equals(getStorageLocation(), playlist.getStorageLocation())
                && Objects.equals(getTimestamp(), playlist.getTimestamp())
                && Objects.equals(getId(), playlist.getId())
                && Objects.equals(getState(), playlist.getState());
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                getFileSource(),
                getStreamLocators(),
                getStorageLocation(),
                getTimestamp(),
                getId(),
                getState());
    }

    @Override
    public String toString() {
        final UUID fileSrcId = fileSource != null ? fileSource.getFileSrcId() : null;
        return getClass().getSimpleName()
                + "("
                + "id = "
                + id
                + ", "
                + "fileSource = "
                + fileSrcId
                + ", "
                + "storageLocation = "
                + storageLocation
                + ", "
                + "timestamp = "
                + timestamp
                + ", "
                + "state = "
                + state
                + ")";
    }
}

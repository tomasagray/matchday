package self.me.matchday.api.service.video;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import self.me.matchday.api.service.EntityService;
import self.me.matchday.db.VideoFileSrcRepository;
import self.me.matchday.model.validation.VideoFileSourceValidator;
import self.me.matchday.model.video.VideoFileSource;

@Service
public class VideoSourceService implements EntityService<VideoFileSource, UUID> {

  private final VideoFileSrcRepository repository;
  private final VideoFileSourceValidator validator;

  public VideoSourceService(VideoFileSrcRepository repository, VideoFileSourceValidator validator) {
    this.repository = repository;
    this.validator = validator;
  }

  @Override
  public VideoFileSource initialize(@NotNull VideoFileSource fileSource) {
    return fileSource;
  }

  @Override
  public VideoFileSource save(@NotNull VideoFileSource entity) {
    validator.validate(entity);
    return repository.saveAndFlush(entity);
  }

  @Override
  public List<VideoFileSource> saveAll(@NotNull Iterable<? extends VideoFileSource> entities) {
    return StreamSupport.stream(entities.spliterator(), false)
        .map(this::save)
        .collect(Collectors.toList());
  }

  @Override
  public Optional<VideoFileSource> fetchById(@NotNull UUID id) {
    return repository.findById(id);
  }

  @Override
  public List<VideoFileSource> fetchAll() {
    return repository.findAll();
  }

  @Override
  public VideoFileSource update(@NotNull VideoFileSource entity) {
    return save(entity);
  }

  @Override
  public List<VideoFileSource> updateAll(@NotNull Iterable<? extends VideoFileSource> entities) {
    return StreamSupport.stream(entities.spliterator(), false)
        .map(this::update)
        .collect(Collectors.toList());
  }

  @Override
  public void delete(@NotNull UUID id) {
    repository.deleteById(id);
  }

  @Override
  public void deleteAll(@NotNull Iterable<? extends VideoFileSource> entities) {
    repository.deleteAll(entities);
  }
}

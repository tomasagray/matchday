package self.me.matchday.unit.plugin.datasource.blogger;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import self.me.matchday.api.service.EntityService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class BloggerTestEntityService implements EntityService<BloggerTestEntity, Long> {

    private final List<BloggerTestEntity> entities = new ArrayList<>();

    @Override
    public BloggerTestEntity initialize(@NotNull BloggerTestEntity bloggerTestEntity) {
        return bloggerTestEntity;
    }

    @Override
    public BloggerTestEntity save(@NotNull BloggerTestEntity entity) {
        entities.add(entity);
        return entity;
    }

    @Override
    public List<BloggerTestEntity> saveAll(@NotNull Iterable<? extends BloggerTestEntity> entities) {
        List<BloggerTestEntity> list = new ArrayList<>();
        for (BloggerTestEntity entity : entities) {
            save(entity);
            list.add(entity);
        }
        return list;
    }

    @Override
    public Optional<BloggerTestEntity> fetchById(@NotNull Long id) {
        return Optional.empty();
    }

    @Override
    public List<BloggerTestEntity> fetchAll() {
        return new ArrayList<>(entities);
    }

    @Override
    public BloggerTestEntity update(@NotNull BloggerTestEntity entity) {
        return entity;
    }

    @Override
    public List<BloggerTestEntity> updateAll(
            @NotNull Iterable<? extends BloggerTestEntity> entities) {
        return null;
    }

    @Override
    public void delete(@NotNull Long id) {
    }

    @Override
    public void deleteAll(@NotNull Iterable<? extends BloggerTestEntity> entities) {
    }
}

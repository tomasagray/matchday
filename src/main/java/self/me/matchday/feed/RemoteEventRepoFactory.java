package self.me.matchday.feed;

import self.me.matchday.model.RemoteEventRepository;

public abstract class RemoteEventRepoFactory {

  /**
   * Return an instance of a Remote Event Repository.
   *
   * @return The repository
   */
  protected abstract RemoteEventRepository getRepository();
}

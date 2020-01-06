/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.feed;

import java.util.stream.Stream;
import self.me.matchday.fileserver.IFSManager;

/**
 * Represents a source of sporting Events. Provides a stream of Events for the end user to watch.
 */
public interface IEventRepository {

  /**
   * Return a Stream of all Events from this repository.
   *
   * @return A Stream<> of IEventSource (Event sources)
   */
  Stream<IEventSource> getEvents();

  /**
   * Set the file server manager that will translate requests from this repository into file
   * resource URLs.
   *
   * @param ifsManager The file server manager to use for this repo
   */
  void setFileServer(IFSManager ifsManager);
}

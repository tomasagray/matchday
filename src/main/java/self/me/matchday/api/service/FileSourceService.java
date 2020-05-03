package self.me.matchday.api.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import self.me.matchday.db.EventFileSrcRepository;

@Service
public class FileSourceService {

  private static final String LOG_TAG = "FileSourceService";

  private final EventFileSrcRepository eventFileSrcRepository;

  @Autowired
  public FileSourceService(EventFileSrcRepository eventFileSrcRepository) {

    this.eventFileSrcRepository = eventFileSrcRepository;
  }
}

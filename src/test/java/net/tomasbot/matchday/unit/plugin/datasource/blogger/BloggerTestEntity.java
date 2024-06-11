package net.tomasbot.matchday.unit.plugin.datasource.blogger;

import java.time.LocalDateTime;

public final class BloggerTestEntity {

  private String title;
  private String text;
  private LocalDateTime published;

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public LocalDateTime getPublished() {
    return published;
  }

  public void setPublished(LocalDateTime published) {
    this.published = published;
  }

  @Override
  public String toString() {
    return String.format("[title=%s, text=%s, published=%s]", title, text, published);
  }
}

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

package self.me.matchday.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Class to handle logging functionality. */
public final class Log {

  private enum Level {
    ERROR,
    DEBUG,
    INFO,
    VERBOSE,
    WARN,
    ASSERT
  }

  private static class LogEntry {
    // Fields
    private final String tag;
    private final String msg;
    private final String thread;
    private final Instant timestamp;
    private final Level level;
    private final Throwable throwable;

    private LogEntry(
        String tag, String msg, Instant timestamp, Level level, @Nullable Throwable throwable) {
      this.tag = tag;
      this.msg = msg;
      this.timestamp = timestamp;
      this.level = level;
      this.throwable = throwable;
      this.thread = Thread.currentThread().getName();
    }

    @Override
    public @NotNull String toString() {
      // indent newlines
      String logMsg = null;
      if (msg != null) {
        logMsg = msg.replaceAll("\\n", "\n\t");
      }
      // Prepare timestamp
      LocalDateTime dateTime = LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault());
      final String date = dateTime.format(DateTimeFormatter.ISO_DATE);
      final String time = dateTime.format(DateTimeFormatter.ofPattern("hh:mm:ss.SSS"));
      // Holder for text output
      StringBuilder sb = new StringBuilder();
      sb.append(date)
          .append(" ")
          .append(time)
          .append("  <")
          .append(level.name())
          .append(">  --- [")
          .append(tag)
          .append("] ")
          .append(thread)
          .append(":\t")
          .append(logMsg);
      if (throwable != null) {
        sb.append("\n\tException:\n-----------------\n\t").append(throwable);
      }

      // Build the string
      return sb.toString();
    }
  }

  /**
   * For logging outright errors.
   *
   * @param tag An identifier for easier log sorting
   * @param msg The message to be logged.
   */
  public static void e(String tag, String msg) {
    LogEntry log = new LogEntry(tag, msg, Instant.now(), Level.ERROR, null);
    print_log_entry(log);
  }

  /**
   * For logging outright errors, with exceptions
   *
   * @param tag Tag for sorting log output
   * @param msg The message to be logged
   * @param exception The exception thrown
   */
  public static void e(String tag, String msg, Throwable exception) {
    LogEntry log = new LogEntry(tag, msg, Instant.now(), Level.ERROR, exception);
    print_error(log, exception);
  }

  /**
   * Debug method, for logs relevant to detecting problems within the application.
   *
   * @param tag An identifier for easier log sorting
   * @param msg The message to be logged.
   */
  public static void d(String tag, String msg) {
    LogEntry log = new LogEntry(tag, msg, Instant.now(), Level.DEBUG, null);
    print_log_entry(log);
  }

  /**
   * Debug logs, with exceptions.
   *
   * @param tag Tag for sorting log output
   * @param msg The message to be logged
   * @param exception The exception thrown.
   */
  public static void d(String tag, String msg, Throwable exception) {
    LogEntry log = new LogEntry(tag, msg, Instant.now(), Level.DEBUG, exception);
    print_error(log, exception);
  }

  /**
   * Informational logging messages.
   *
   * @param tag Tag for sorting logging output
   * @param msg The message to be logged.
   */
  public static void i(String tag, String msg) {
    LogEntry log = new LogEntry(tag, msg, Instant.now(), Level.INFO, null);
    print_log_entry(log);
  }

  /**
   * Informational logging messages, with exceptions
   *
   * @param tag Tag for sorting log output
   * @param msg The message to be logged
   * @param exception The exception thrown
   */
  public static void i(String tag, String msg, Throwable exception) {
    LogEntry log = new LogEntry(tag, msg, Instant.now(), Level.INFO, exception);
    print_log_entry(log);
  }

  /**
   * Verbose logging; includes maximum relevant data/output
   *
   * @param tag Tag for sorting log output
   * @param msg The message to be logged
   */
  public static void v(String tag, String msg) {
    LogEntry log = new LogEntry(tag, msg, Instant.now(), Level.VERBOSE, null);
    print_log_entry(log);
  }

  /**
   * Verbose logging, with exceptions; includes maximum relevant data/output
   *
   * @param tag Tag for sorting log output
   * @param msg The message to be logged
   * @param exception The exception thrown
   */
  public static void v(String tag, String msg, Throwable exception) {
    LogEntry log = new LogEntry(tag, msg, Instant.now(), Level.VERBOSE, exception);
    print_error(log, exception);
  }

  /**
   * Warning messages; more serious than informational, less serious than errors.
   *
   * @param tag Tag for sorting log output
   * @param msg The message to be logged.
   */
  public static void w(String tag, String msg) {
    LogEntry log = new LogEntry(tag, msg, Instant.now(), Level.WARN, null);
    print_log_entry(log);
  }

  /**
   * Warning messages, with exceptions
   *
   * @param tag Tag for sorting log output
   * @param msg The message to be logged
   * @param exception The exception thrown
   */
  public static void w(String tag, String msg, Throwable exception) {
    LogEntry log = new LogEntry(tag, msg, Instant.now(), Level.WARN, exception);
    print_log_entry(log);
  }

  /**
   * Write the log entry to the console.
   *
   * @param logEntry The log entry to display.
   */
  private static void print_log_entry(@NotNull LogEntry logEntry) {
    System.out.println(logEntry);
  }

  private static void print_error(
      @NotNull final LogEntry logEntry, @NotNull final Throwable throwable) {
    System.out.println(logEntry);
    throwable.printStackTrace();
  }
}

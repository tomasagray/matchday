package net.tomasbot.matchday.api.controller;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.SQLIntegrityConstraintViolationException;
import net.tomasbot.matchday.api.service.InvalidArtworkException;
import net.tomasbot.matchday.api.service.InvalidEventException;
import net.tomasbot.matchday.api.service.PluginNotFoundException;
import net.tomasbot.matchday.api.service.UnknownEntityException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class _GlobalExceptionHandler {

  private static final Logger logger = LogManager.getLogger(_GlobalExceptionHandler.class);

  private static String handleError(@NotNull Throwable e) {
    final String message = e.getMessage();
    logger.error(message, e);
    return message;
  }

  @ExceptionHandler({IOException.class, UncheckedIOException.class})
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  @ResponseBody
  public String handleIoError(@NotNull Throwable e) {
    return handleError(e);
  }

  @ExceptionHandler(FileNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  @ResponseBody
  public String handleFileNotFound(@NotNull Throwable e) {
    return "File not found: " + handleError(e);
  }

  @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ResponseBody
  public String handleSqlIntegrityError(@NotNull Throwable e) {
    return handleError(e);
  }

  @ExceptionHandler({IllegalStateException.class, IllegalArgumentException.class})
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ResponseBody
  public String handleIllegalState(@NotNull Throwable e) {
    return handleError(e);
  }

  @ExceptionHandler(RuntimeException.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  @ResponseBody
  public String handleRuntimeError(@NotNull Throwable e) {
    return handleError(e);
  }

  // Custom exceptions ==============================================================
  @ExceptionHandler({InvalidEventException.class, InvalidArtworkException.class})
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ResponseBody
  public String handleInvalidArt(@NotNull Throwable e) {
    return handleError(e);
  }

  @ExceptionHandler(PluginNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  @ResponseBody
  public String handlePluginNotFound(@NotNull Throwable e) {
    return handleError(e);
  }

  @ExceptionHandler(UnknownEntityException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  @ResponseBody
  public String handleUnknownEntity(@NotNull Throwable e) {
    return handleError(e);
  }
}

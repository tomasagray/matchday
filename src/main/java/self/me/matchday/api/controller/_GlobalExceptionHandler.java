package self.me.matchday.api.controller;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.SQLIntegrityConstraintViolationException;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import self.me.matchday.api.service.InvalidArtworkException;
import self.me.matchday.api.service.PluginNotFoundException;
import self.me.matchday.api.service.UnknownEntityException;

@RestControllerAdvice
public class _GlobalExceptionHandler {

  @ExceptionHandler({IOException.class, UncheckedIOException.class})
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  @ResponseBody
  public String handleIoError(@NotNull Throwable e) {
    return e.getMessage();
  }

  @ExceptionHandler(FileNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  @ResponseBody
  public String handleFileNotFound(@NotNull Throwable e) {
    return "File not found: " + e.getMessage();
  }

  @ExceptionHandler({IllegalStateException.class, IllegalArgumentException.class})
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ResponseBody
  public String handleIllegalState(@NotNull Throwable e) {
    return e.getMessage();
  }

  @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ResponseBody
  public String handleSqlIntegrityError(@NotNull Throwable e) {
    return e.getMessage();
  }

  // Custom exceptions ==============================================================
  @ExceptionHandler(InvalidArtworkException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ResponseBody
  public String handleInvalidArt(@NotNull InvalidArtworkException e) {
    return e.getMessage();
  }

  @ExceptionHandler(PluginNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  @ResponseBody
  public String handlePluginNotFound(@NotNull Throwable e) {
    return e.getMessage();
  }

  @ExceptionHandler(UnknownEntityException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  @ResponseBody
  public String handleUnknownEntity(@NotNull UnknownEntityException e) {
    return e.getMessage();
  }
}

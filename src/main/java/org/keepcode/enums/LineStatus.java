package org.keepcode.enums;

import org.jetbrains.annotations.NotNull;

public enum LineStatus {

  ACTIVE("Активна"),

  UN_ACTIVE("Не активна"),

  UNKNOWN("Неизвестно");

  private final String status;

  LineStatus(@NotNull String status) {
    this.status = status;
  }

  @NotNull
  public String getStatus() {
    return status;
  }
}

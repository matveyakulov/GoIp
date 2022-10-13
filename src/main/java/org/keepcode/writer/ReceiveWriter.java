package org.keepcode.writer;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedWriter;
import java.io.FileWriter;

public class ReceiveWriter {

  private static final String FILE_NAME = "goip.txt";

  public static void write(@NotNull String msg) {
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_NAME, true))) {
      writer.append(msg);
      writer.flush();
    } catch (Exception e) {
      System.out.printf("Не удалось записать в файл текст: %s из-за ошибки: %s", msg, e.getMessage());
    }
  }
}

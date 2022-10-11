package org.keepcode.writer;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedWriter;

public class FileWriter {

  private static final String FILE_NAME = "goip.txt";

  public static void write(@NotNull String msg) {
    try (BufferedWriter writer = new BufferedWriter(new java.io.FileWriter(FILE_NAME, true))) {
      writer.append(msg);
      writer.flush();
    } catch (Exception e) {
      System.out.println("Не удалось записать в файл " + msg);
    }
  }
}

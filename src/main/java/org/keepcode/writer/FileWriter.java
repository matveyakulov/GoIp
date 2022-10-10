package org.keepcode.writer;

import java.io.BufferedWriter;

public class FileWriter {

  private static final String FILE_NAME = "goip.txt";
  public static void write(String msg) {
    try (BufferedWriter writer = new BufferedWriter(new java.io.FileWriter(FILE_NAME, true))) {
      writer.append(msg);
      writer.flush();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}

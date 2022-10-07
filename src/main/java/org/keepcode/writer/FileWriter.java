package org.keepcode.writer;

import java.io.BufferedWriter;

public class FileWriter {

  private static final String fileName = "goip.txt";
  public static void write(String msg) {
    try (BufferedWriter writer = new BufferedWriter(new java.io.FileWriter(fileName, true))) {
      writer.append(msg);
      //todo только flush?
      writer.flush();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}

package org.keepcode.util;

import org.jetbrains.annotations.NotNull;
import org.keepcode.Main;

import java.io.IOException;
import java.util.Properties;

public class PropUtil {

  private static final String CONFIG_FILE_NAME = "config.properties";
  private static final Properties properties = new Properties();

  static {
    try {
      properties.load(Main.class.getClassLoader().getResourceAsStream(CONFIG_FILE_NAME));
    } catch (IOException e) {
      System.out.println("Чтение конфига завершилось с ошибкой " + e.getMessage());
      System.exit(-1);
    }
  }

  public static int getReceivePort() {
    return Integer.parseInt(getProp("goip.receive.port"));
  }

  public static int getSocketTimeout() {
    return Integer.parseInt(getProp("goip.timeout"));
  }

  @NotNull
  private static String getProp(@NotNull String prop) {
    return properties.getProperty(prop);
  }
}

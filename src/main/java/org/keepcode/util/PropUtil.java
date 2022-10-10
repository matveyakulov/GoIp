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

  @NotNull
  public static String getHost(){
      return getProp("goip.host");
  }

  @NotNull
  public static Integer getReceivePort() {
    return Integer.parseInt(getProp("goip.receive.port"));
  }

  @NotNull
  public static Integer getDefaultSendPort() {
    return Integer.valueOf(getProp("goip.send.default.port"));
  }

  @NotNull
  public static Integer getSocketTimeout() {
    return Integer.valueOf(getProp("goip.timeout"));
  }

  @NotNull
  private static String getProp(String prop){
    return properties.getProperty(prop);
  }
}

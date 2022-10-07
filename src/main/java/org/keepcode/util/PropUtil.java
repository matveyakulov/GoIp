package org.keepcode.util;

import org.keepcode.Main;

import java.io.IOException;
import java.util.Properties;

public class PropUtil {

  private static final Properties properties = new Properties();

  static {
    try {
      //todo подумай еще над обработкой
      properties.load(Main.class.getClassLoader().getResourceAsStream("config.properties"));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static String getHost(){
      return properties.getProperty("goip.host");
  }

  public static int getReceivePort() {
    return Integer.parseInt(properties.getProperty("goip.receive.port"));
  }

  public static int getDefaultSendPort() {
    return Integer.parseInt(properties.getProperty("goip.send.default.port"));
  }
}

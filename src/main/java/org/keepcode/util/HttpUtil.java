package org.keepcode.util;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class HttpUtil {

  private static final String ROOT_URI = "http://%s/default/en_US";
  private static final String LOGIN = "admin";
  private static final String PASSWORD = "admin";

  @NotNull
  public static String getInfoDeviceBody(@NotNull String ip) throws IOException {
    HttpURLConnection httpClient = getConnectionWithAuth(String.format(ROOT_URI, ip) + "/status.xml?type=base");
    if (httpClient.getResponseCode() == 200) {
      return readFromInputStream(httpClient.getInputStream());
    }
    throw new IOException(
      String.format("Пришел ответ с кодом %d и телом %s",
        httpClient.getResponseCode(),
        readFromInputStream(httpClient.getErrorStream()))
    );
  }

  @NotNull
  private static HttpURLConnection getConnectionWithAuth(@NotNull String url) throws IOException {
    HttpURLConnection httpClient = (HttpURLConnection) new URL(url).openConnection();
    httpClient.setRequestMethod("GET");
    String auth = LOGIN + ":" + PASSWORD;
    byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
    String authHeaderValue = "Basic " + new String(encodedAuth);
    httpClient.setRequestProperty("Authorization", authHeaderValue);
    return httpClient;
  }

  @NotNull
  private static String readFromInputStream(@NotNull InputStream inputStream) throws IOException {
    BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
    StringBuilder strLine = new StringBuilder();
    String strCurrentLine;
    while ((strCurrentLine = br.readLine()) != null) {
      strLine.append(strCurrentLine);
    }
    return strLine.toString();
  }
}

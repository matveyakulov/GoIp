package org.keepcode;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

  public static void main(String[] args) {
    //new MainFrame();
    Pattern AFTER_SENDID_PATTERN = Pattern.compile("\\d+ (?<answer>.+)");
    String ans = "set_gsm_num 123 124123 OK";
    Matcher matcher = AFTER_SENDID_PATTERN.matcher(ans);
    matcher.find();
    System.out.println(matcher.group("answer"));
  }
}

package org.keepcode.match;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextMatcher {

  public static String match(Pattern pattern, String text) {
    Matcher matcher = pattern.matcher(text);
    if (matcher.find()) {
      //что если групп будет несколько? Мы же вчера это обсуждали
      return matcher.group();
    } else {
      throw new RuntimeException(String.format("В тексте %s ничего не найдено по паттерну %s", text, pattern.pattern()));
    }
  }
}

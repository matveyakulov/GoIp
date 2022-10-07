package org.keepcode.swing;

import org.keepcode.domain.GsmLine;
import org.keepcode.service.GsmService;
import org.keepcode.validate.Validator;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;

import static org.keepcode.validate.Validator.validateNum;

public class MainFrame extends JFrame {

  private final Box sendUssdAnswer = Box.createHorizontalBox();
  private final Box numberInfoAnswer = Box.createHorizontalBox();
  private final Box rebootCommandAnswer = Box.createHorizontalBox();
  private final Box rebootLineCommandAnswer = Box.createHorizontalBox();

  private final Box setGsmNumAnswer = Box.createHorizontalBox();

  private Integer[] lines = new Integer[0];

  private final ComboBoxLines<String> comboBoxLinesStatus = new ComboBoxLines<>(new String[0]);

  private final ComboBoxLines<Integer> linesComboUssd = new ComboBoxLines<>(lines);

  private final ComboBoxLines<Integer> linesComboGetNumInfo = new ComboBoxLines<>(lines);

  private final ComboBoxLines<Integer> linesComboRebootLine = new ComboBoxLines<>(lines);

  private final ComboBoxLines<Integer> linesComboSetGsmNum = new ComboBoxLines<>(lines);

  private static Map<Integer, GsmLine> gsmLinesCurrent;

  private static JButton sendUssdBtn;
  private static JButton rebootGoipBtn;
  private static JButton sendNumInfoBtn;
  private static JButton rebootLineBtn;
  private static JButton sendSetNumBtn;

  public MainFrame() throws HeadlessException {
    super("Goip");
    Box mainBox = Box.createVerticalBox();
    setSize(500, 500);
    mainBox.add(createUssdCommand());
    mainBox.add(sendUssdAnswer);
    mainBox.add(createRebootCommand());
    mainBox.add(rebootCommandAnswer);
    mainBox.add(createNumberInfoCommand());
    mainBox.add(numberInfoAnswer);
    mainBox.add(createRebootLineCommand());
    mainBox.add(rebootLineCommandAnswer);
    mainBox.add(createSetGsmNumCommand());
    mainBox.add(setGsmNumAnswer);
    mainBox.add(comboBoxLinesStatus);
    setVisible(true);
    add(mainBox);
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    changeEnableBtn();
    new Thread(GsmService::listen).start();
    new Thread(() -> {
      while (true) {
        try {
          Thread.sleep(35 * 1000);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
        Map<Integer, GsmLine> gsmLinesNew = GsmService.getGsmLineMap();
        if (gsmLinesCurrent == null ||
          !gsmLinesCurrent.values().equals(gsmLinesNew.values())) {
          gsmLinesCurrent = gsmLinesNew;
          SwingUtilities.invokeLater(() -> updateCheckBoxes(gsmLinesCurrent));
        }
      }
    }).start();
  }

  private void updateCheckBoxes(Map<Integer, GsmLine> gsmLines) {
    comboBoxLinesStatus.setModel(new DefaultComboBoxModel<>(createStatusLine(gsmLines)));
    lines = gsmLines.keySet().stream().map(Integer::new).toArray(Integer[]::new);
    linesComboRebootLine.setModel(new DefaultComboBoxModel<>(lines));
    linesComboUssd.setModel(new DefaultComboBoxModel<>(lines));
    linesComboGetNumInfo.setModel(new DefaultComboBoxModel<>(lines));
    linesComboSetGsmNum.setModel(new DefaultComboBoxModel<>(lines));
    revalidate();
    if(!gsmLines.isEmpty()) {
      changeEnableBtn();
    }
  }

  private static void changeEnableBtn() {
    boolean enable = !sendUssdBtn.isEnabled();
    sendUssdBtn.setEnabled(enable);
    rebootGoipBtn.setEnabled(enable);
    sendNumInfoBtn.setEnabled(enable);
    rebootLineBtn.setEnabled(enable);
    sendSetNumBtn.setEnabled(enable);
  }

  private String[] createStatusLine(Map<Integer, GsmLine> lineStatus) {
    String[] lines = new String[lineStatus.size()];
    int i = 0;
    for (Integer s : lineStatus.keySet()) {
      lines[i++] = String.format("%d - %s", s, lineStatus.get(s).getStatus());
    }
    return lines;
  }

  private Box createUssdCommand() {
    sendUssdBtn = new JButton("Отправить");
    JTextField sendUssdValue = new CustomTextField();
    sendUssdBtn.addActionListener(e ->
      new Thread(() -> {
        if (Validator.validateUssd(sendUssdValue.getText()) && linesComboUssd.getSelectedItem() != null) {
          int lineNum = (Integer) linesComboUssd.getSelectedItem();
          String response = GsmService.sendUssd(lineNum, sendUssdValue.getText(), gsmLinesCurrent.get(lineNum).getPassword());
          SwingUtilities.invokeLater(() -> {
            sendUssdAnswer.add(new JLabel(response));
            linesComboUssd.setSelectedIndex(0);
            sendUssdValue.setText("");
            sendUssdAnswer.revalidate();
          });
        }
      }).start());
    Box innerBox = Box.createHorizontalBox();
    innerBox.add(new JLabel("Отправить ussd:"));
    innerBox.add(sendUssdValue);
    innerBox.add(new JLabel("на линию:"));
    innerBox.add(linesComboUssd);
    innerBox.add(sendUssdBtn);
    return innerBox;
  }

  private Box createRebootCommand() {
    Box box = Box.createHorizontalBox();
    rebootGoipBtn = new JButton("Рестарт goip");
    rebootGoipBtn.addActionListener(e -> {
      new Thread(() -> {
        try {
          int line = gsmLinesCurrent.keySet().stream().findFirst().orElseThrow(() -> new RuntimeException("Нет линий"));
          if (gsmLinesCurrent.get(line).getPassword() != null) {
            String answer = GsmService.reboot(line, gsmLinesCurrent.get(line).getPassword());
            SwingUtilities.invokeLater(() -> {
              rebootCommandAnswer.add(new JLabel(answer));
              rebootCommandAnswer.revalidate();
            });
          }
        } catch (Exception ex) {
          throw new RuntimeException(ex);
        }
      }).start();
    });
    box.add(rebootGoipBtn);
    return box;
  }

  private Box createNumberInfoCommand() {
    sendNumInfoBtn = new JButton("Отправить");
    sendNumInfoBtn.addActionListener(e ->
      new Thread(() -> {
        if (linesComboGetNumInfo.getSelectedItem() != null) {
          int line = (Integer) linesComboGetNumInfo.getSelectedItem();
          String response = GsmService.numberInfo(line, gsmLinesCurrent.get(line).getPassword());
          SwingUtilities.invokeLater(() -> {
            numberInfoAnswer.add(new JLabel(response));
            linesComboGetNumInfo.setSelectedIndex(0);
            numberInfoAnswer.revalidate();
          });
        }
      }).start()
    );
    Box innerBox = Box.createHorizontalBox();
    innerBox.add(new JLabel("Узнать номер на линии:"));
    innerBox.add(linesComboGetNumInfo);
    innerBox.add(sendNumInfoBtn);
    return innerBox;
  }

  private Box createRebootLineCommand() {
    rebootLineBtn = new JButton("Отправить");
    rebootLineBtn.addActionListener(e ->
      new Thread(() -> {
        if (linesComboRebootLine.getSelectedItem() != null) {
          int line = (Integer) linesComboRebootLine.getSelectedItem();
          String answer = GsmService.lineReboot(line, gsmLinesCurrent.get(line).getPassword());
          SwingUtilities.invokeLater(() -> {
            linesComboRebootLine.setSelectedIndex(0);
            rebootLineCommandAnswer.add(new JLabel(answer));
            rebootLineCommandAnswer.revalidate();
          });
        }
      }).start()
    );
    Box innerBox = Box.createHorizontalBox();
    innerBox.add(new JLabel("Перезагрузить линию:"));
    innerBox.add(linesComboRebootLine);
    innerBox.add(rebootLineBtn);
    innerBox.add(rebootLineCommandAnswer);
    return innerBox;
  }

  private Box createSetGsmNumCommand() {
    JTextField number = new CustomTextField();
    sendSetNumBtn = new JButton("Отправить");
    sendSetNumBtn.addActionListener(e ->
      new Thread(() -> {
        if (validateNum(number.getText()) && linesComboSetGsmNum.getSelectedItem() != null) {
          int line = (Integer) linesComboSetGsmNum.getSelectedItem();
          String answer = GsmService.setGsmNum(number.getText(), line, gsmLinesCurrent.get(line).getPassword());
          linesComboSetGsmNum.setSelectedIndex(0);
          SwingUtilities.invokeLater(() -> {
            number.setText("");
            setGsmNumAnswer.add(new JLabel(answer));
            setGsmNumAnswer.revalidate();
          });
        }
      }).start()
    );
    Box innerBox = Box.createHorizontalBox();
    innerBox.add(new JLabel("На линии:"));
    innerBox.add(linesComboSetGsmNum);
    innerBox.add(new JLabel("изменить номер на:"));
    innerBox.add(number);
    innerBox.add(sendSetNumBtn);
    return innerBox;
  }
}

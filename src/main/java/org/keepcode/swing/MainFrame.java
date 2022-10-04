package org.keepcode.swing;

import org.keepcode.listener.NumberInfoActionListener;
import org.keepcode.listener.RebootLineActionListener;
import org.keepcode.listener.SetNumActionListener;
import org.keepcode.listener.UssdActionListener;
import org.keepcode.service.GsmService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainFrame extends JFrame {

  private final Box sendUssdAnswer = Box.createHorizontalBox();
  private final Box numberInfoAnswer = Box.createHorizontalBox();
  private final Box rebootCommandAnswer = Box.createHorizontalBox();
  private final Box rebootLineCommandAnswer = Box.createHorizontalBox();

  private final Box setGsmNumAnswer = Box.createHorizontalBox();

  private Integer[] lines = new Integer[]{1, 3};

  private final ComboBoxLines<String> comboBoxLinesStatus = new ComboBoxLines<>(new String[0]);

  private final ComboBoxLines<Integer> linesComboUssd = new ComboBoxLines<>(lines);

  private final ComboBoxLines<Integer> linesComboGetNumInfo = new ComboBoxLines<>(lines);

  private final ComboBoxLines<Integer> linesComboRebootLine = new ComboBoxLines<>(lines);

  private final ComboBoxLines<Integer> linesComboSetGsmNum = new ComboBoxLines<>(lines);

  int port = 7777;

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
    mainBox.add(comboBoxLinesStatus);
    setVisible(true);
    add(mainBox);
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    new Thread(() -> {
      while (true) {
        Map<String, String> linesStatus = GsmService.getLinesStatus(port);
        comboBoxLinesStatus.setModel(new DefaultComboBoxModel<>(createStatusLine(linesStatus)));
        lines = linesStatus.keySet().stream().map(Integer::new).toArray(Integer[]::new);
        linesComboRebootLine.setModel(new DefaultComboBoxModel<>(lines));
        linesComboUssd.setModel(new DefaultComboBoxModel<>(lines));
        linesComboGetNumInfo.setModel(new DefaultComboBoxModel<>(lines));
        linesComboSetGsmNum.setModel(new DefaultComboBoxModel<>(lines));
        revalidate();
      }
    }).start();
  }

  private String[] createStatusLine(Map<String, String> lineStatus) {
    List<String> lines = new ArrayList<>(lineStatus.size());
    for (String s : lineStatus.keySet()) {
      lines.add(String.format("%s - %s", s, lineStatus.get(s)));
    }
    return lines.stream().toArray(String[]::new);
  }

  private Box createUssdCommand() {
    JButton sendUssdBtn = new JButton("Отправить");
    JTextField sendUssdValue = new CustomTextField();
    sendUssdBtn.addActionListener(new UssdActionListener(linesComboUssd, sendUssdValue, sendUssdAnswer));
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
    JButton button = new JButton("Рестарт goip");
    button.addActionListener(new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          String answer = GsmService.reboot();
          rebootCommandAnswer.add(new JLabel(answer));
          rebootCommandAnswer.revalidate();
        } catch (Exception ex) {
          throw new RuntimeException(ex);
        }
      }
    });
    box.add(button);
    return box;
  }

  private Box createNumberInfoCommand() {
    JButton sendUssdBtn = new JButton("Отправить");
    sendUssdBtn.addActionListener(new NumberInfoActionListener(linesComboGetNumInfo, numberInfoAnswer));
    Box innerBox = Box.createHorizontalBox();
    innerBox.add(new JLabel("Узнать номер на линии:"));
    innerBox.add(linesComboGetNumInfo);
    innerBox.add(sendUssdBtn);
    return innerBox;
  }

  private Box createRebootLineCommand() {
    JButton sendUssdBtn = new JButton("Отправить");
    sendUssdBtn.addActionListener(new RebootLineActionListener(linesComboRebootLine, rebootLineCommandAnswer));
    Box innerBox = Box.createHorizontalBox();
    innerBox.add(new JLabel("Перезагрузить линию:"));
    innerBox.add(linesComboRebootLine);
    innerBox.add(sendUssdBtn);
    innerBox.add(rebootLineCommandAnswer);
    return innerBox;
  }

  private Box createSetGsmNumCommand() {
    JTextField number = new CustomTextField();
    JButton sendUssdBtn = new JButton("Отправить");
    sendUssdBtn.addActionListener(new SetNumActionListener(number, linesComboSetGsmNum, setGsmNumAnswer));
    Box innerBox = Box.createHorizontalBox();
    innerBox.add(new JLabel("На линии:"));
    innerBox.add(linesComboSetGsmNum);
    innerBox.add(new JLabel("изменить номер на:"));
    innerBox.add(number);
    innerBox.add(sendUssdBtn);
    innerBox.add(setGsmNumAnswer);
    return innerBox;
  }
}

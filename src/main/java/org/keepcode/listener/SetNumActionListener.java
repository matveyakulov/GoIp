package org.keepcode.listener;

import org.keepcode.service.GsmService;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static org.keepcode.validate.Validator.validateNum;

public class SetNumActionListener implements ActionListener {

  private final JTextField number;
  private final JComboBox<Integer> line;

  private final Box answerBox;

  public SetNumActionListener(JTextField number, JComboBox<Integer> line, Box answerBox) {
    this.number = number;
    this.line = line;
    this.answerBox = answerBox;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (validateNum(number.getText())) {
      String answer = GsmService.setGsmNum(number.getText(), (Integer) line.getSelectedItem());
      line.setSelectedIndex(0);
      number.setText("");
      answerBox.add(new JLabel(answer));
      answerBox.revalidate();
    }
  }
}

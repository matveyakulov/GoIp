package org.keepcode.listener;

import org.keepcode.service.GsmService;
import org.keepcode.validate.Validator;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class UssdActionListener implements ActionListener {

  private final JComboBox<Integer> line;
  private final JTextField command;

  private final Box answer;

  public UssdActionListener(JComboBox<Integer> line, JTextField command, Box answer) {
    this.line = line;
    this.command = command;
    this.answer = answer;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (Validator.validateUssd(command.getText())) {
      String response = GsmService.sendUssd((Integer) line.getSelectedItem(), command.getText());
      answer.add(new JLabel(response));
      line.setSelectedIndex(0);
      command.setText("");
      answer.revalidate();
    }
  }
}

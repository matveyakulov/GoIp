package org.keepcode.listener;

import org.keepcode.service.GsmService;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class NumberInfoActionListener implements ActionListener {

    private final JComboBox<Integer> line;
    private final Box answer;

    public NumberInfoActionListener(JComboBox<Integer> line, Box answer) {
        this.line = line;
        this.answer = answer;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String response = GsmService.numberInfo((Integer) line.getSelectedItem());
        answer.add(new JLabel(response));
        line.setSelectedIndex(0);
        answer.revalidate();
    }
}

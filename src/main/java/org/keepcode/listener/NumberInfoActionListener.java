package org.keepcode.listener;

import org.keepcode.response.Response;
import org.keepcode.service.GsmService;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class NumberInfoActionListener implements ActionListener {

    private final JComboBox<String> line;
    private final Box answer;

    public NumberInfoActionListener(JComboBox<String> line, Box answer) {
        this.line = line;
        this.answer = answer;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Response response = GsmService.numberInfo((String) line.getSelectedItem());
        answer.add(new JLabel(response.getBody()));
        line.setSelectedIndex(0);
        answer.revalidate();
    }
}

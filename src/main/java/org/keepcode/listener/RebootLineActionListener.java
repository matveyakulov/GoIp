package org.keepcode.listener;

import org.keepcode.service.GsmService;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class RebootLineActionListener implements ActionListener {

    private final JComboBox<Integer> number;

    private final Box answerBox;

    public RebootLineActionListener(JComboBox<Integer> number, Box answerBox) {
        this.number = number;
        this.answerBox = answerBox;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String answer = GsmService.lineReboot((Integer) number.getSelectedItem());
        number.setSelectedIndex(0);
        answerBox.add(new JLabel(answer));
        answerBox.revalidate();
    }
}

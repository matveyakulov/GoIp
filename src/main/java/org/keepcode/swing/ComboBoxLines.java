package org.keepcode.swing;

import javax.swing.*;
import java.awt.*;

public class ComboBoxLines extends JComboBox<String> {

    public ComboBoxLines() {
        super(new String[0]);
        setMaximumSize(new Dimension(170, 50));
    }
}

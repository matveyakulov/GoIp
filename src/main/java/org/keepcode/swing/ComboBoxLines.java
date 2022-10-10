package org.keepcode.swing;

import javax.swing.*;
import java.awt.*;

public class ComboBoxLines<T> extends JComboBox<T> {

    public ComboBoxLines(T[] items) {
        super(items);
        setMaximumSize(new Dimension(200, 50));
    }
}

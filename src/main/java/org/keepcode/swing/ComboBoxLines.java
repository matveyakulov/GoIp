package org.keepcode.swing;

import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class ComboBoxLines extends JComboBox<String> {

  public ComboBoxLines() {
    super(new String[0]);
    setMaximumSize(new Dimension(170, 50));
  }

  @Nullable
  @Override
  public String getSelectedItem(){
    return (String) super.getSelectedItem();
  }
}

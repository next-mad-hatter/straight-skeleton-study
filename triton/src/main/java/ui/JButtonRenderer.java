package at.tugraz.igi.ui;

import lombok.*;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;

public class JButtonRenderer implements TableCellRenderer {

    @Getter JButton button = new JButton();

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                   int row, int column) {

        return ConfigurationTable.adjustButton(button, value, table, row);
    }
}

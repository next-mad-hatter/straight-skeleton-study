package at.tugraz.igi.ui;

import lombok.*;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;

public class JButtonRenderer implements TableCellRenderer {

    @Getter JButton button = new JButton();

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                   int row, int column) {

	    if (value.toString() == "visible") {
			val V = ((ConfigurationTable) table).getController().isVisible(row);
			System.err.println("Renderer querying visibility of " + row + " : " + V);
		}

        return ConfigurationTable.getButton(button, value.toString(), table, isSelected,
                ((ConfigurationTable) table).getController().isVisible(row));
    }
}

package at.tugraz.igi.ui;

import lombok.*;

import at.tugraz.igi.main.Controller;

import javax.swing.*;
import javax.swing.event.CellEditorListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.EventObject;

public class JButtonEditor extends AbstractCellEditor implements TableCellEditor {
    private static final long serialVersionUID = 1L;
    @Getter private JButton button;
    String action;
    int row;

    public JButtonEditor(final Controller controller) {
        super();

        button = new JButton();
        button.setOpaque(true);
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (action == "delete") {
                    controller.removeContext(row);
                } else if (action == "copy") {
                    controller.cloneContext(row);
                } else if (action == "color") {
                    controller.showColorChooser(row);
                } else if (action == "visible") {
                    controller.setVisible(row, !controller.isVisible(row));
                    /*
                    button.setIcon(controller.isVisible(row) ?
                            Controller.visible_icon :
                            Controller.not_visible_icon);
                    */
                }
            }
        });
    }

    public Object getCellEditorValue() {
        return null;
    }

    public boolean isCellEditable(EventObject anEvent) {
        return true;
    }

    public boolean shouldSelectCell(EventObject anEvent) {
        return false;
    }

    public boolean stopCellEditing() {
        return super.stopCellEditing();
    }

    public void cancelCellEditing() {
    }

    public void addCellEditorListener(CellEditorListener l) {
    }

    public void removeCellEditorListener(CellEditorListener l) {
    }

    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        action = (value == null) ? "" : value.toString();
        this.row = row;

        if (value.toString() == "visible") {
            val V = ((ConfigurationTable) table).getController().isVisible(row);
            System.err.println("Editor querying visibility of " + row + " : " + V);
        }

        return ConfigurationTable.getButton(button, value.toString(), table, isSelected,
            ((ConfigurationTable) table).getController().isVisible(row));
    }
}

package at.tugraz.igi.ui;

import lombok.*;

import at.tugraz.igi.main.Controller;
import at.tugraz.igi.ui.*;

import javax.swing.*;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.EventObject;

public class JButtonEditor extends AbstractCellEditor implements TableCellEditor {
    private static final long serialVersionUID = 1L;
    @Getter private JButton button;
    String action;
    int row;
    ConfigurationTable table;

    public JButtonEditor(ConfigurationTable table, final Controller controller) {
        super();
        this.table = table;
        button = new JButton();
        button.setOpaque(true);

        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (action == "delete") {
                    controller.removeContext(row);
                } else if (action == "copy") {
                    val cnt = table.getRowCount();
                    controller.cloneContext(row);
                    if (table.getRowCount() == cnt) return;
                    table.setValueAt(new Boolean(false), row, 1);
                    controller.refreshContext();
                    // table.getTableModel().fireTableCellUpdated(row, 1);
                } else if (action == "color") {
                    controller.showColorChooser(row);
                } else if (action == "toggle") {
                    val v = (Boolean) table.getValueAt(row, 1);
                    table.setValueAt(!v, row, 1);
                    // table.getTableModel().fireTableCellUpdated(row, 1);
                    if (!v.booleanValue())
                        button.setIcon(Controller.visible_icon);
                    else
                        button.setIcon(Controller.not_visible_icon);
                    // stopCellEditing();
                    // table.getCellEditor().stopCellEditing();
                    table.editingStopped(new ChangeEvent(table));
                    controller.refreshContext();
                }
            }
        });
    }

    public Object getCellEditorValue() {
        return table.getValueAt(row, 1);
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
        action = (value instanceof Boolean) ? "toggle" : value.toString();
        this.row = row;

        return ConfigurationTable.adjustButton(button, value, table, row);
    }
}

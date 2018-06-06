package at.tugraz.igi.ui;

import lombok.*;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EventObject;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.*;

import at.tugraz.igi.main.Controller;

public class ConfigurationTable extends JTable {

	private static final long serialVersionUID = 1L;

	@Getter Controller controller;
	@Getter TableModel tableModel;

	public ConfigurationTable(Controller controller) {

		class ForcedListSelectionModel extends DefaultListSelectionModel {

			// private static final long serialVersionUID = 42L;

			public ForcedListSelectionModel () {
				setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			}

			@Override
			public void clearSelection() {
			}

			@Override
			public void removeSelectionInterval(int index0, int index1) {
			}

		}

		// TableModel tableModel = new TableModel(controller);
		tableModel = new TableModel(controller);

		this.controller = controller;

		this.setModel(tableModel);

		this.setTableHeader(null);
		this.setShowGrid(false);
		this.getColumn(this.getColumnName(1)).setCellRenderer(new JButtonRenderer());
		this.getColumn(this.getColumnName(1)).setCellEditor(new JButtonEditor(this, controller));
		this.getColumn(this.getColumnName(2)).setCellRenderer(new JButtonRenderer());
		this.getColumn(this.getColumnName(2)).setCellEditor(new JButtonEditor(this, controller));
		this.getColumn(this.getColumnName(3)).setCellRenderer(new JButtonRenderer());
		this.getColumn(this.getColumnName(3)).setCellEditor(new JButtonEditor(this, controller));
		this.getColumn(this.getColumnName(4)).setCellRenderer(new JButtonRenderer());
		this.getColumn(this.getColumnName(4)).setCellEditor(new JButtonEditor(this, controller));

		for (int i = 0; i < 5; i++) {
			TableColumn column = this.getColumnModel().getColumn(i);
			column.setPreferredWidth(28);
		}

		this.setRowHeight(32);

		this.setRowSelectionAllowed(true);
		this.setColumnSelectionAllowed(false);
		this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		this.setSelectionModel(new ForcedListSelectionModel());

		ListSelectionModel rowSM = this.getSelectionModel();
		val me = this;
		rowSM.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting()) return;
				ListSelectionModel lsm = (ListSelectionModel)e.getSource();
				if (!lsm.isSelectionEmpty()) {
					int row = lsm.getMinSelectionIndex();
					System.err.println("Setting visibility of " + row + " to true");
					me.setValueAt(new Boolean(true), row, 1);
					// tableModel.fireTableCellUpdated(row, 1);
					System.err.println("Switching to " + row);
					controller.switchContext(row);
				}
			}
		});

		this.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

		// FIXME: How do we set background for whole selected row (buttons also)?
		/*
		this.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
			{
				final Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				c.setBackground(row % 2 == 0 ? Color.LIGHT_GRAY : Color.WHITE);
				return c;
			}
		});
		*/

	}

	public static JButton adjustButton(JButton button, Object value, JTable table, int row) {
	    val name = value.toString();
		if (value instanceof Boolean) {
			// System.err.println("Checking visibility of " + row);
			button.setIcon(((Boolean) table.getValueAt(row, 1)) ?
					Controller.visible_icon :
					Controller.not_visible_icon);
			button.setToolTipText("Change visibility");
		} else if (name == "delete") {
			button.setIcon(Controller.delete_icon);
			button.setToolTipText("Delete skeleton");
		} else if (name == "copy") {
            button.setIcon(Controller.copy_icon);
            button.setToolTipText("Copy skeleton");
		} else if (name == "color") {
			button.setIcon(Controller.color_icon);
			button.setToolTipText("Change color");
		}
		button.setBorder(BorderFactory.createEmptyBorder());
		button.setFocusable(false);
		button.setOpaque(true);

		// This won't work since the buttons are created before when
		// the row is being populated -- before it's selected iianm.
		/*
		System.err.println("Row " + row + " , selected " + table.getSelectionModel().getMinSelectionIndex());
		if (table.getSelectionModel().getMinSelectionIndex() == row) {
			button.setBackground(table.getSelectionBackground());
		} else {
			button.setBackground(table.getBackground());
		}
		// button.setBackground(new Color(Color.TRANSLUCENT));
     	*/

		return button;
	}

	public void addRow() {
		TableModel model = (TableModel) this.getModel();
		model.addRow(new Object[] { "", new Boolean(true), "color", "copy", "delete",});
	}

	public void removeRow(int rowIndex) {
		TableModel model = (TableModel) this.getModel();
		model.removeRow(rowIndex);
	}

	public void removeAllRows() {
		TableModel model = (TableModel) this.getModel();
		model.setRowCount(0);
	}
}

@SuppressWarnings("serial")
class TableModel extends DefaultTableModel {
	private String columnNames[] = { "1", "2", "3", "4", "5", };
	private Controller controller;

	public TableModel(Controller controller) {
		this.controller = controller;
	}

	public int getColumnCount() {
		return columnNames.length;
	}

	public String getColumnName(int col) {
		return columnNames[col];
	}

	public Class getColumnClass(int c) {
		return getValueAt(0, c).getClass();
	}

	public boolean isCellEditable(int row, int column) {
		return (column > 0);
		// return true;
	}

	public void setValueAt(Object obj, int row, int column) {
		super.setValueAt(obj, row, column);
		if (column == 1) {
			Boolean value = ((Boolean) obj).booleanValue();
		}
	}
}

class JLabelRenderer implements TableCellRenderer {

	JLabel label = new JLabel();
	Controller controller;
	Border padding = BorderFactory.createEmptyBorder(0, 10, 0, 10);

	public JLabelRenderer(Controller controller) {
		this.controller = controller;
	}

	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		label.setForeground(controller.getContext(row).getSkeleton(false).getColor());
		label.setBackground(controller.getContext(row).getSkeleton(false).getColor());
		label.setText(value.toString());
		label.setBorder(BorderFactory.createCompoundBorder(label.getBorder(), padding));

		return label;
	}
}

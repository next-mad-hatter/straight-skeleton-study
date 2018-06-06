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
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import at.tugraz.igi.main.Controller;

public class ConfigurationTable extends JTable {

	private static final long serialVersionUID = 1L;

	@Getter Controller controller;

	public ConfigurationTable(Controller controller) {

		class ForcedListSelectionModel extends DefaultListSelectionModel {

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

		TableModel tableModel = new TableModel(controller);

		this.controller = controller;

		this.setModel(tableModel);

		this.setTableHeader(null);
		this.setShowGrid(false);
		this.getColumn(this.getColumnName(1)).setCellRenderer(new JButtonRenderer());
		this.getColumn(this.getColumnName(1)).setCellEditor(new JButtonEditor(controller));
		this.getColumn(this.getColumnName(2)).setCellRenderer(new JButtonRenderer());
		this.getColumn(this.getColumnName(2)).setCellEditor(new JButtonEditor(controller));
		this.getColumn(this.getColumnName(3)).setCellRenderer(new JButtonRenderer());
		this.getColumn(this.getColumnName(3)).setCellEditor(new JButtonEditor(controller));
		this.getColumn(this.getColumnName(4)).setCellRenderer(new JButtonRenderer());
		this.getColumn(this.getColumnName(4)).setCellEditor(new JButtonEditor(controller));

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
		rowSM.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting()) return;
				ListSelectionModel lsm = (ListSelectionModel)e.getSource();
				if (!lsm.isSelectionEmpty()) {
					int row = lsm.getMinSelectionIndex();
					controller.switchContext(row);
				}
			}
		});

	}

	public static JButton getButton(JButton button, String name, JTable table, boolean selected, boolean cVisible) {
		if (name == "delete") {
			button.setIcon(Controller.delete_icon);
			button.setToolTipText("Delete Straight Skeleton");
		} else if (name == "copy") {
            button.setIcon(Controller.copy_icon);
            button.setToolTipText("Copy Straight Skeleton");
		} else if (name == "color") {
			button.setIcon(Controller.color_icon);
			button.setToolTipText("Change color");
		} else if (name == "visible") {
			// button.setIcon(Controller.visible_icon);
			button.setIcon(cVisible ?
					Controller.visible_icon :
					Controller.not_visible_icon);
			button.setToolTipText("Change visibility");
		}
		button.setBorder(BorderFactory.createEmptyBorder());
		button.setFocusable(false);
		if (selected) {
			// button.setBackground(table.getSelectionBackground());
			button.setBackground(Color.RED);
		} else {
			button.setBackground(table.getBackground());
		}

		return button;
	}

	public static JToggleButton getEditButton(JToggleButton button, JTable table) {
		button.setToolTipText("Edit in drawing area");
		button.setIcon(Controller.edit_icon);
		// button.setBackground(Color.WHITE);
		button.setBorder(BorderFactory.createEmptyBorder());
		button.setFocusable(false);
		return button;
	}

	public void addRow() {
		TableModel model = (TableModel) this.getModel();
		model.addRow(new Object[] { new Boolean(false), "visible", "color", "copy", "delete",});
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
		return true;
	}

	public void setValueAt(Object obj, int row, int column) {
		super.setValueAt(obj, row, column);
		/*
		if (column == 0) {
			Boolean value = ((Boolean) obj).booleanValue();
			controller.switchContext(row);
			controller.getContext().editMode = value;
			if (value) {
				for (int i = 0; i < getRowCount(); i++) {
					if (i != row) {
						// super.setValueAt(new Boolean(false), i, column);
					}
				}
			}
		}
		*/
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
		label.setText(value.toString());
		label.setBorder(BorderFactory.createCompoundBorder(label.getBorder(), padding));

		if (isSelected) {
			label.setBackground(Color.RED); // table.getSelectionBackground());
		} else {
			label.setBackground(table.getBackground());
		}

		return label;
	}
}

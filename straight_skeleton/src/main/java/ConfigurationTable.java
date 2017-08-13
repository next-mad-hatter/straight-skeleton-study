package at.tugraz.igi.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EventObject;

import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.event.CellEditorListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import at.tugraz.igi.main.Controller;

public class ConfigurationTable extends JTable {
	private static final long serialVersionUID = 1L;

	public ConfigurationTable(Controller controller) {
		TableModel tableModel = new TableModel(controller);

		this.setModel(tableModel);

		this.setTableHeader(null);
		this.setShowGrid(false);
		this.getColumn(this.getColumnName(1)).setCellRenderer(new JLabelRenderer(controller));
		this.getColumn(this.getColumnName(2)).setCellRenderer(new JButtonRenderer());
		this.getColumn(this.getColumnName(2)).setCellEditor(new JButtonEditor(controller));
		this.getColumn(this.getColumnName(3)).setCellRenderer(new JButtonRenderer());
		this.getColumn(this.getColumnName(3)).setCellEditor(new JButtonEditor(controller));
		this.getColumn(this.getColumnName(4)).setCellRenderer(new JButtonRenderer());
		this.getColumn(this.getColumnName(4)).setCellEditor(new JButtonEditor(controller));
		this.getColumn(this.getColumnName(5)).setCellRenderer(new JButtonRenderer());
		this.getColumn(this.getColumnName(5)).setCellEditor(new JButtonEditor(controller));

		TableColumn column = null;
		for (int i = 0; i < 6; i++) {
			column = this.getColumnModel().getColumn(i);
			if (i == 1) {
				column.setPreferredWidth(100);

			} else {
				column.setPreferredWidth(20);
			}
		}

	}

	public static JButton getButton(JButton button, String name, JTable table) {
		if (name == "delete") {
			button.setIcon(Controller.delete_icon);
			button.setToolTipText("Delete Straight Skeleton");
		} else if (name == "play") {
			button.setIcon(Controller.play_icon);
			button.setToolTipText("Play");
		} else if (name == "color") {
			button.setIcon(Controller.color_icon);
			button.setToolTipText("Change color");
		} else if (name == "visible") {
			button.setIcon(Controller.visible_icon);
			button.setToolTipText("Change visibility");
		}
		button.setBackground(Color.WHITE);
		button.setBorder(BorderFactory.createEmptyBorder());
		button.setFocusable(false);
		table.setRowHeight(20);
		return button;
	}

	public static JToggleButton getEditButton(JToggleButton button, JTable table) {
		button.setToolTipText("Edit in drawing area");
		button.setIcon(Controller.edit_icon);
		button.setBackground(Color.WHITE);
		button.setBorder(BorderFactory.createEmptyBorder());
		button.setFocusable(false);
		table.setRowHeight(20);
		return button;
	}

	public void addRow() {
		TableModel model = (TableModel) this.getModel();
		model.addRow(new Object[] { new Boolean(false), "Straight Skeleton ",  "visible", "delete", "play", "color",});
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

class TableModel extends DefaultTableModel {
	private String columnNames[] = { "1", "2", "3", "4", "5", "6" };
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
		if (column == 1) {
			return false;
		}
		return true;
	}

	public void setValueAt(Object obj, int row, int column) {
		super.setValueAt(obj, row, column);
		if (column == 0) {
			Boolean value = ((Boolean) obj).booleanValue();
			controller.updateSkeleton(row, value);
			if (value) {
				for (int i = 0; i < getRowCount(); i++) {
					if (i != row) {
						super.setValueAt(new Boolean(false), i, column);
					}
				}
			}
		}
	}
}

class JLabelRenderer implements TableCellRenderer {

	JLabel label = new JLabel();
	Controller controller;

	public JLabelRenderer(Controller controller) {
		this.controller = controller;
	}

	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		label.setForeground(controller.getStraightSkeletons().get(row).getColor());
		label.setText(value.toString());
		return label;
	}
}

class JButtonRenderer implements TableCellRenderer {

	JButton button = new JButton();

	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {

		return ConfigurationTable.getButton(button, value.toString(), table);
	}
}

class JButtonEditor extends AbstractCellEditor implements TableCellEditor {
	private static final long serialVersionUID = 1L;
	JButton button;
	String action;
	int row;

	public JButtonEditor(final Controller controller) {
		super();

		button = new JButton();
		button.setOpaque(true);
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (action == "delete") {
					controller.removeStraightSkeleton(row);
				} else if (action == "play") {
					controller.playSelected(row, true, true);
				} else if (action == "color") {
					controller.showColorChooser(row);
				} else if (action == "visible") {
					JButton button = (JButton) e.getSource();
					boolean visible;
					if (button.getIcon().equals(controller.visible_icon)) {
						button.setIcon(controller.not_visible_icon);
					} else {
						button.setIcon(controller.visible_icon);
					}
					controller.toggleVisibility(row, button.getIcon().equals(controller.visible_icon));
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
		return ConfigurationTable.getButton(button, value.toString(), table);
	}
}

package at.tugraz.igi.ui;

import lombok.*;

import java.awt.geom.*;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.BorderFactory;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import at.tugraz.igi.main.Controller;
import at.tugraz.igi.util.Line;
import at.tugraz.igi.util.Vector;

public class CustomTextField extends JTextField implements DocumentListener, MouseMotionListener, MouseListener {
	private Line line;
	private Line screenLine;
	private at.tugraz.igi.util.Vector vector;
	private int weight;
	private Controller controller;
	private static final long serialVersionUID = 1L;

	public CustomTextField(Line line, Line screenLine, Controller controller) {
		this.setEditable(false);
		this.getCaret().setSelectionVisible(false);
		this.getCaret().setVisible(false);

		this.setHorizontalAlignment(JTextField.CENTER);
		this.setOpaque(false);
		Font font = new Font(this.getFont().getName(), Font.BOLD, 14);
		this.setFont(font);
		this.setBorder(BorderFactory.createEmptyBorder());

		this.line = line;
		this.screenLine = screenLine;
		this.controller = controller;
		position(screenLine);

		this.getDocument().addDocumentListener(this);
		this.addMouseMotionListener(this);
		this.addMouseListener(this);

	}

	/*
	public void setScale(int size) {
		this.setFont(new Font(this.getFont().getName(), Font.BOLD, size));
	}
	*/

	public void updatePosition(boolean counterclockwise) {
		position(screenLine);
		if (counterclockwise) {
			reposition();
		}
	}

	public void position() {
	    position(screenLine);
	}

	private void position(Line line) {
	    val p2 = controller.view.transformCoordinates(
	    		line.getP2().getOriginalX(),
				line.getP2().getOriginalY(), true);
		val p1 = controller.view.transformCoordinates(
				line.getP1().getOriginalX(),
				line.getP1().getOriginalY(), true);

		int x2 = (int) (p2.getX() + p1.getX()) / 2;
		int y2 = (int) (p2.getY() + p1.getY()) / 2;
		double v_y = -(p2.getX() - p1.getX());
		double v_x = (p2.getY() - p1.getY());
		/*
		int x2 = (int) (line.getP2().getOriginalX() + line.getP1().getOriginalX()) / 2;
		int y2 = (int) (line.getP2().getOriginalY() + line.getP1().getOriginalY()) / 2;
		double v_y = -(line.getP2().getOriginalX() - line.getP1().getOriginalX());
		double v_x = (line.getP2().getOriginalY() - line.getP1().getOriginalY());
		*/

		double length = Math.sqrt(v_y * v_y + v_x * v_x);
		v_y /= length;
		v_x /= length;
		this.vector = new Vector(v_x, v_y);

		if (!this.getText().equals(line.getWeightAsString())) { // && this.getText().matches("^-?\\d+$")) {
			this.setText(line.getWeightAsString());
		}

		this.setBounds((int) (x2 - this.getPreferredSize().width / 2 + (v_x * 16)),
				(int) (y2 - this.getPreferredSize().height / 2 + (v_y * 16)), 30, this.getPreferredSize().height);
	}

	public void setLines(Line line, Line screenLine) {
		this.line = line;
		this.screenLine = screenLine;
		position(screenLine);
	}

	@Override
	public void changedUpdate(DocumentEvent e) {
	}

	@Override
	public void insertUpdate(DocumentEvent e) {
		String text = this.getText().replaceAll(" ", "");
		try {
			weight = Integer.parseInt(text);
			if (weight < 0) {
				weight = Math.abs(weight);
				final JTextField field = this;
				Runnable doRun = new Runnable() {
                    @Override
                    public void run() {
                       field.setText(Integer.toString(weight));
                    }
              };
              SwingUtilities.invokeLater(doRun);
			}
		} catch (NumberFormatException ne) {

		}
		line.setWeight(weight);
		screenLine.setWeight(weight);
		if (controller.getStraightSkeletons().size() != 0 && !controller.isMove()) {
			int row = controller.getSelectedIndex();
			// controller.playCurrent();
			controller.playSelected(row, false, false);
		}
		// this.getInputVerifier().shouldYieldFocus(this);
		// graphicPanel.setFinished(false);
		// SkeletonApplet.move = true;
		// controller.view.repaint();
	}

	@Override
	public void removeUpdate(DocumentEvent e) {
	}

	public void reposition() {
		double x = -vector.getX();
		double y = -vector.getY();
		this.setBounds((int) (this.getX() + (x * 32)), (int) (this.getY() + (y * 32)), 30,
				this.getPreferredSize().height);
		vector = new at.tugraz.igi.util.Vector(x, y);
	}

	@Override
	public void mouseDragged(MouseEvent e) {

	}

	@Override
	public void mouseMoved(MouseEvent e) {
		this.setToolTipText("Change weight of (" + screenLine.getP1() + "," + screenLine.getP2() + ")");
		this.getCaret().setSelectionVisible(true);
		this.getCaret().setVisible(true);
		this.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		this.setEditable(true);
		this.setOpaque(true);
	}

	@Override
	public void mouseClicked(MouseEvent e) {

	}

	@Override
	public void mouseEntered(MouseEvent e) {

	}

	@Override
	public void mouseExited(MouseEvent e) {
		this.setBorder(BorderFactory.createEmptyBorder());
		this.setEditable(false);
		this.getCaret().setSelectionVisible(false);
		this.getCaret().setVisible(false);
		this.setOpaque(false);

	}

	@Override
	public void mousePressed(MouseEvent e) {

	}

	@Override
	public void mouseReleased(MouseEvent e) {

	}

	public Line getScreenLine() {
		return screenLine;
	}

	public Line getLine() {
		return line;
	}

}

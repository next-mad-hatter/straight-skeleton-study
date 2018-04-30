package at.tugraz.igi.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;

import javax.swing.JSplitPane;

public class MainPane extends JSplitPane{
	private static final long serialVersionUID = 1L;
	private boolean hasProportionalLocation = false;
    private double proportionalLocation = 0.80;
    private boolean isPainted = false;
    
    public MainPane(int newOrientation,
            Component newLeftComponent,
            Component newRightComponent){
    	super(newOrientation, newLeftComponent, newRightComponent);
    }
    public void setDividerLocation(double proportionalLocation) {
        if (!isPainted) {
            hasProportionalLocation = true;
            this.proportionalLocation = proportionalLocation;
        } else {
            super.setDividerLocation(proportionalLocation);
        }
    }

    public void paint(Graphics g) {
        super.paint(g);
        if (!isPainted) {
            if (hasProportionalLocation) {
                super.setDividerLocation(proportionalLocation);
            }
            isPainted = true;
        }
    }

}

package at.tugraz.igi.main;


import javax.swing.SwingUtilities;

public class Main {

	public static void main(String[] args) {
		  SwingUtilities.invokeLater(new Runnable() {

	            public void run()
	            {
	                SkeletonApplet app = new SkeletonApplet();
	                app.setVisible(true);
	            }

	        }
	);

	}

}

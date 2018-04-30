package at.tugraz.igi.main;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.LayoutManager;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.border.TitledBorder;

import at.tugraz.igi.main.Controller.TYPES;
import at.tugraz.igi.ui.ConfigurationTable;
import at.tugraz.igi.ui.GraphicPanel;
import at.tugraz.igi.ui.MainPane;
import data.Graph;
import frontend.MainFrame;

public class SkeletonApplet extends JFrame {

	private static final long serialVersionUID = 1L;
	private GridBagConstraints constraints = new GridBagConstraints();
	private Controller controller;

	public SkeletonApplet() {
	
		controller = new Controller();
		this.setTitle("Triton");
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
		int width = gd.getDisplayMode().getWidth();
		int height = gd.getDisplayMode().getHeight();
		this.setSize(new Dimension(width, height));
		this.setExtendedState(JFrame.MAXIMIZED_BOTH); 
		
		JMenuBar menubar = new JMenuBar();
		JMenu menu = new JMenu("File");
		JMenuItem open = new JMenuItem("Open");
		JMenuItem save = new JMenuItem("Save");
		JMenuItem saveAs = new JMenuItem("Save as");
		JMenuItem saveSVG = new JMenuItem("Save as .svg");

		JMenu menu2 = new JMenu("Random Polygon");
		JMenuItem createPoly = new JMenuItem("Generate Polygon");
		// JMenuItem openPoly = new JMenuItem("Open generated Polygon");

		open.setAccelerator(KeyStroke.getKeyStroke('O', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		save.setAccelerator(KeyStroke.getKeyStroke('S', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		saveAs.setAccelerator(KeyStroke.getKeyStroke('A', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

		menu.add(open);
		menu.add(save);
		menu.add(saveAs);
		menu.add(saveSVG);

		menu2.add(createPoly);
		// menu2.add(openPoly);

		open.addActionListener(controller.createActionListener(TYPES.OPEN));

		save.addActionListener(controller.createActionListener(TYPES.SAVE));

		saveAs.addActionListener(controller.createActionListener(TYPES.SAVE_AS));
		
		saveSVG.addActionListener(controller.createActionListener(TYPES.SVG));
		
		createPoly.addActionListener(new ActionListener() {
			MainFrame gui = new MainFrame();

			@Override
			public void actionPerformed(ActionEvent e) {
				// Run a java app in a separate system process
				for (WindowListener listener : gui.getWindowListeners()) {
					gui.removeWindowListener(listener);
				}
				gui.addWindowListener(new WindowAdapter() {

					public void windowClosing(WindowEvent e) {
						MainFrame frame = (MainFrame) e.getSource();
						Graph graph = frame.getGraph();
						controller.createPolygon(graph);
						gui.setVisible(false);
					}

				});
				gui.setVisible(true);

			}
		});

		// openPoly.addActionListener(controller.createActionListener(TYPES.OPEN_POLY));

		menubar.add(menu);
		menubar.add(menu2);

		setJMenuBar(menubar);

		JPanel actionPanel = new JPanel();
		GraphicPanel screenPanel = new GraphicPanel(controller);
		JPanel confPanel = new JPanel();
		confPanel.setLayout(new BoxLayout(confPanel, BoxLayout.Y_AXIS));
		confPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
				"Available Straight Skeletons", TitledBorder.CENTER, TitledBorder.TOP));
		ConfigurationTable table = new ConfigurationTable(controller);
		confPanel.add(table);
		confPanel.setBackground(Color.WHITE);
		JScrollPane scrollPane = new JScrollPane(screenPanel);
		screenPanel.setPreferredSize(new Dimension(500, 600));

		JSplitPane splitPane = new MainPane(JSplitPane.HORIZONTAL_SPLIT, scrollPane, confPanel);
		splitPane.setOneTouchExpandable(true);
		splitPane.setResizeWeight(1.0);

		Dimension minimumSize = new Dimension(250, 50);
		confPanel.setMinimumSize(minimumSize);

		controller.setTable(table);
		controller.setView(screenPanel);

		JButton playButton = new JButton("Play");

		playButton.addActionListener(controller.createActionListener(TYPES.PLAY));

		JButton stepButton = new JButton("Step");
		stepButton.addActionListener(controller.createActionListener(TYPES.STEP));

		JButton backButton = new JButton("Back");
		backButton.addActionListener(controller.createActionListener(TYPES.BACK));

		JButton resetButton = new JButton("Reset");
		resetButton.addActionListener(controller.createActionListener(TYPES.RESET));
		
		JPanel checkPanel = new JPanel();
		checkPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
		JCheckBox randomButton = new JCheckBox("Use random weights");
		randomButton.addItemListener(controller.createItemListener());
		checkPanel.add(randomButton);
		
		actionPanel.add(playButton);
		actionPanel.add(backButton);
		actionPanel.add(stepButton);
		actionPanel.add(resetButton);

		playButton.setPreferredSize(new Dimension(80, 30));
		playButton.setIcon(Controller.play_icon);
		playButton.setBackground(Color.WHITE);
		playButton.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		playButton.setFocusable(false);
		stepButton.setPreferredSize(playButton.getPreferredSize());
		stepButton.setIcon(Controller.step_icon);
		stepButton.setBackground(Color.WHITE);
		stepButton.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		stepButton.setFocusable(false);
		backButton.setPreferredSize(playButton.getPreferredSize());
		backButton.setIcon(Controller.back_icon);
		backButton.setBackground(Color.WHITE);
		backButton.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		backButton.setFocusable(false);
		resetButton.setPreferredSize(playButton.getPreferredSize());
		resetButton.setIcon(Controller.reset_icon);
		resetButton.setBackground(Color.WHITE);
		resetButton.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		resetButton.setFocusable(false);
		actionPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
		
		JPanel bottomPanel = new JPanel();
		bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.X_AXIS));
		bottomPanel.add(checkPanel);
		bottomPanel.add(actionPanel);
		
		setLayout(new GridBagLayout());
		constraints.weightx = 1.0;
		constraints.weighty = 1.0;
		constraints.fill = GridBagConstraints.BOTH;
		addGB(splitPane, 0, 0);
		constraints.gridwidth = 1;
		constraints.weighty = 0.02;
		constraints.fill = GridBagConstraints.BOTH;
		addGB(bottomPanel, 0, 1);
		this.pack();
		repaint();
		
	}

	private void addGB(Component component, int x, int y) {
		constraints.gridx = x;
		constraints.gridy = y;
		getContentPane().add(component, constraints);
	}

	public void paint(Graphics g) {

		super.paint(g);

	}

}

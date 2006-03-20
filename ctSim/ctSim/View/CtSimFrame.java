package ctSim.View;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JSplitPane;

/** 
 * Die Haupt-View-Klasse des c't-Sim welche die ControlPanels und die
 * WorldView Klasse in einer großen JSplitPane zusammen führt.
 * 
 * @author Markus Lang (lang@repulse.de)
 * @version 2006-03-06
 */
public class CtSimFrame extends JFrame implements WindowStateListener {
	private static final long serialVersionUID = 1L;

	private JSplitPane splitPane;
	private ControlFrame controlFrame;
	
	/**
	 * Constructor: Erzeugt ein neues ctSimFrame Hauptfenster
	 * 
	 * @param controlFrame die ControlFrame Instanz (leftComponent)
	 * @param worldView  the WorldView Instanz (rightComponent)
	 */
	public CtSimFrame(ControlFrame controlFrame, WorldView worldView) {
		// super constructor aufrufen
		super();
		// gui elemente laden
		this.controlFrame = controlFrame;
		this.initGUI(worldView);
		// eventListener registrieren
		this.addWindowStateListener(this);
		// fenstergröße festlegen
		Dimension dimension = new Dimension(1024, 700);
		this.setMinimumSize(dimension);
		this.setPreferredSize(dimension);
		this.setSize(dimension);
		// fenster positionieren
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		this.setBounds(
				(screenSize.width / 2) - (dimension.width / 2),
				(screenSize.height / 2) - (dimension.height / 2),
				dimension.width, dimension.height);
		this.setVisible(true);
		this.pack();
	}
	
	/**
	 * Initialisierung der GUI Elemente
	 *  
	 * @param worldView  the WorldView Instanz (rightComponent)
	 */
	private void initGUI(WorldView worldView) {
		// layout definieren
		this.setTitle("c't-Sim");
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		BoxLayout thisLayout = new BoxLayout(getContentPane(),
				javax.swing.BoxLayout.Y_AXIS);
		this.getContentPane().setLayout(thisLayout);
		// min & max größe des controlFrame setzen
		Dimension preferedSize = this.controlFrame.getPreferredSize();
		this.controlFrame.setMinimumSize(preferedSize);
		this.controlFrame.setMaximumSize(preferedSize);
		// splitpane erstellen
		this.splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, this.controlFrame, worldView); 
		this.splitPane.setContinuousLayout(false); 
		this.splitPane.setOneTouchExpandable(true);
		
		this.getContentPane().add(this.splitPane, BorderLayout.CENTER); 
	}
	
	/**
	 * Beim Aufruf dieser Methode wird die Slider-Position neu bestimmt
	 */
	public void updateSliderPosition() {
		// min & max größe des controlFrame setzen
		Dimension preferedSize = this.controlFrame.getPreferredSize();
		this.controlFrame.setMinimumSize(preferedSize);
		this.controlFrame.setMaximumSize(preferedSize);
		this.controlFrame.revalidate();
		// splitPane Divider positionieren
		this.splitPane.setDividerLocation(
				this.controlFrame.getPreferredSize().width + this.splitPane.getInsets().left);
	}
	
	/**
	 * Implementierte Methode des WindowStateListener Interfaces
	 * Wird dazu genutzt bei Minimieren/Maximieren des Fensters die 
	 * Divider Position der SplitPlane zu aktualisieren.
	 * 
	 * @see java.awt.event.WindowStateListener#windowStateChanged(java.awt.event.WindowEvent)
	 */
	public void windowStateChanged(WindowEvent arg0) {
		this.updateSliderPosition();
	}
}

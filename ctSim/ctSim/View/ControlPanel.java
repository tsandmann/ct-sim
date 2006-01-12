package ctSim.View;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JLabel;
import java.awt.Dimension;
import javax.swing.JTextField;

import ctSim.Model.Bot;

public abstract class ControlPanel extends javax.swing.JPanel {

	private static final long serialVersionUID = 1L;

	private JTextField yPosField;

	private JTextField xPosField;

	private JTextField headField;
	
	// Zu welchem Bot gehört das Panel?
	private Bot bot;

	// In welchem Rahmen wird das Panel angezeigt?
	private ControlFrame frame;
	
	public ControlPanel(Bot bot) {
		super();
		this.bot = bot;
		Dimension dim = new Dimension(30, 25);
		xPosField = new JTextField();
		yPosField = new JTextField();
		headField = new JTextField();

		xPosField.setPreferredSize(dim);
		yPosField.setPreferredSize(dim);
		headField.setPreferredSize(dim);

	}

	protected abstract void initGUI(); 
	
	public Bot getBot() {
		return bot;
	}

	public abstract void reactToChange();
	
	public JTextField getXPosField() {
		return xPosField;
	}

	public JTextField getYPosField() {
		return yPosField;
	}

	
	/**
	 * Entfernt dieses Panel aus dem ControlFrame
	 */
	public void remove(){
		frame.getControlPanels().remove(this);
	}
	
	/**
	 * @return Returns the headField.
	 */
	public JTextField getHeadField() {
		return headField;
	}

	/**
	 * @param headField The headField to set.
	 */
	public void setHeadField(JTextField headField) {
		this.headField = headField;
	}

	/**
	 * @param posField The xPosField to set.
	 */
	public void setXPosField(JTextField posField) {
		xPosField = posField;
	}

	/**
	 * @param posField The yPosField to set.
	 */
	public void setYPosField(JTextField posField) {
		yPosField = posField;
	}

	/**
	 * @return Gibt frame zurueck.
	 */
	public ControlFrame getFrame() {
		return frame;
	}

	/**
	 * @param frame Wert fuer frame, der gesetzt werden soll.
	 */
	public void setFrame(ControlFrame frame) {
		this.frame = frame;
	}

}

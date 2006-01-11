package ctSim.View;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JTextField;

import ctSim.Model.Bot;

public abstract class ControlPanel extends javax.swing.JPanel {

	private static final long serialVersionUID = 1L;

	private JTextField yPosField;

	private JTextField xPosField;

	private JTextField headField;
	
	// Zu welchem Bot gehört das Panel?
	private Bot bot;

	public ControlPanel(Bot bot) {
		super();
		this.bot = bot;
		xPosField = new JTextField();
		yPosField = new JTextField();
		headField = new JTextField();
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

}

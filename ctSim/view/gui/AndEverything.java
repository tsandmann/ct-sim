package ctSim.view.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import ctSim.model.bots.Bot;
import ctSim.model.bots.components.Actuator;
import ctSim.model.bots.components.actuators.LcDisplay;
import ctSim.util.AuxFrameButton;

public class AndEverything extends BotBuisitor {
	private static final long serialVersionUID = - 8170321975584432026L;

	public AndEverything() {
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBorder(BorderFactory.createTitledBorder("Und der ganze Rest"));
	}

	@Buisit
	public void buildLcdViewer(LcDisplay d) {
		JTextArea t = new JTextArea(d.getModel(), null,
			d.getNumRows(), d.getNumCols());
		t.setEnabled(false);
		t.setFont(new Font("Monospaced", Font.PLAIN, 12));
		t.setDisabledTextColor(Color.BLACK);
		t.setBackground(new Color(170, 200, 90));
		t.setBorder(BorderFactory.createLoweredBevelBorder());
		t.setToolTipText(d.getName());
		t.setMaximumSize(t.getPreferredSize());
		t.setAlignmentX(Component.CENTER_ALIGNMENT);
		add(t);
		add(Box.createRigidArea(new Dimension(0, 5)));
	}

	@Buisit
	public void buildLogViewer(Actuator.Log log, Bot bot) {
		// TextArea bauen
		JTextArea t = new JTextArea(log.getModel());
		t.setEditable(false);
		t.setColumns(70);
		t.setRows(25);

		// Ausliefern
		//$$ Fenster sollte immer zur neuesten Ausgabe scrollen
		add(new AuxFrameButton(
			log.getName(),
			log.getDescription() + " von " + bot.getName(), // Fenster-Titel
			new JScrollPane(t)));
		add(Box.createRigidArea(new Dimension(0, 5)));
	}
}

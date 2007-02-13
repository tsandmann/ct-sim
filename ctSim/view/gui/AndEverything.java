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
import javax.swing.text.DefaultCaret;

import ctSim.model.bots.Bot;
import ctSim.model.bots.components.Actuators;
import ctSim.model.bots.components.RemoteCallCompnt;
import ctSim.model.bots.components.Sensors;
import ctSim.util.AuxFrameButton;

//$$ doc
//$$$ GridLayout, damit wir Reihenfolge der Dinger vorgeben koennen
public class AndEverything extends GuiBotBuisitor {
	private static final long serialVersionUID = - 8170321975584432026L;

	public AndEverything() {
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBorder(BorderFactory.createTitledBorder("Und der ganze Rest"));
	}

	public void buisitLcdViewer(Actuators.LcDisplay d) {
		JTextArea t = new JTextArea(d.getExternalModel(), null,
			d.getNumRows(), d.getNumCols());
		t.setEnabled(false);

		/*
		 * Fix fuer Bug 8 im Trac ("Kein Scrollen moeglich wenn Sim aktiv").
		 * Details hab ich nicht rausgekriegt, aber wenn man das Caret (=
		 * Cursor) abschaltet geht's. Siehe Bug:
		 * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4201999 und
		 * zugehoerigen Fix:
		 * http://java.sun.com/j2se/1.5.0/docs/guide/swing/1.5/#swingText
		 */
		setCaretPolicy(t, DefaultCaret.NEVER_UPDATE);

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

	private static void setCaretPolicy(JTextArea t, int updatePolicy) {
		DefaultCaret c = new DefaultCaret();
		c.setUpdatePolicy(updatePolicy);
		t.setCaret(c);
	}

	public void buisitLogViewer(Actuators.Log log, Bot bot) {
		// TextArea bauen
		JTextArea t = new JTextArea(log.getExternalModel());
		t.setEditable(false);
		t.setColumns(70);
		t.setRows(25);

		// Ausliefern
		//$$$ Fenster sollte immer zur neuesten Ausgabe scrollen
		//$$$ Speichern als txt
		add(new AuxFrameButton(
			log.getName(),
			log.getDescription() + " von " + bot, // Fenster-Titel
			new JScrollPane(t)));
		add(Box.createRigidArea(new Dimension(0, 5)));
	}

	public void buisitRemoteControl(Sensors.RemoteControl s, Bot bot) {
		add(new AuxFrameButton(
			s.getDescription()+" ("+s.getName()+")",
			s.getDescription()+" f\u00FCr "+bot,
			new RemoteControlViewer(s)));
		add(Box.createRigidArea(new Dimension(0, 5)));
	}

	public void buisitRemoteCallViewer(RemoteCallCompnt c, Bot bot) {
		add(new AuxFrameButton(
			c.getName(),
			c.getName()+" an "+bot,
			new RemoteCallViewer(c)));
		add(Box.createRigidArea(new Dimension(0, 5)));
	}
}

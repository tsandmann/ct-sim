package ctSim.view.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JTextArea;

import ctSim.model.bots.components.actuators.LcDisplay;

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
}

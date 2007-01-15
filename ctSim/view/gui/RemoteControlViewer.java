package ctSim.view.gui;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

import ctSim.model.bots.components.BotComponent.NumberModel;

//$$ doc
//$$ Runde Buttons waeren schoen
public class RemoteControlViewer extends JPanel {
	private static final long serialVersionUID = - 6483687307396837800L;

	private static final Color LIGHT_BLUE = new Color(120, 120, 255);
	private static final Color GR         = new Color(50,  200, 50);
	private static final Color YE         = new Color(200, 200, 0);

	// 25A1: Quadrat fuer "Stop"
	private static final String qdrt  = "\u25A1";
	// 25CF: Dicker Punkt fuer "Record"
	private static final String punkt = "\u25CF";

	private final JComponent[] buttonsAndLayout = {
		defaultColor(LIGHT_BLUE),
		grid(3, 5,
			// 03A6: "Power-Symbol" (wie Kombination von O und | )
			null,            null,            b("\u03A6", 0x118C, GR),
			b("1" , 0x1181), b("2" , 0x1182), b("3" , 0x1183),
			b("4" , 0x1184), b("5" , 0x1185), b("6" , 0x1186),
			b("7" , 0x1187), b("8" , 0x1188), b("9" , 0x1189),
			b("10", 0x1180), b("11", 0x118A), b("12", 0x11A3)
		),
		defaultColor(null),
		grid(4, 1,
			b("GR -", 0x01BA, GR), b("RE +", 0x01BD, new Color(200, 50, 50)),
			b("YE -", 0x01B1, YE), b("BL +", 0x01B0, LIGHT_BLUE)
		),
		defaultColor(Color.LIGHT_GRAY),
		grid(5, 5,
			b("I/II",0x11AB),null,          null,          null,          b("TV/VCR",0x11B8),
			null,            null,          b("||",0x11A9),null,          null,
			null,            b("<<",0x11B2),b(">", 0x11B5),b(">>",0x11B4),null,
			null,            null,          b(qdrt,0x11B6),null,          null,
			b(punkt,0x11AB), null,          null,          null,          b("CH*P/P",0x11BF)
		),
		defaultColor(LIGHT_BLUE),
		grid(3, 2,
			b("Vol+", 0x1190), b("Mute", 0x01BF), b("Ch+", 0x11A0),
			b("Vol-", 0x1191), null,              b("Ch-", 0x11A1)
		)
	};

	private final NumberModel model;
	private Color currentDefault = null;

	private JComponent defaultColor(Color c) {
		currentDefault = c;
		return null;
	}

	private JButton b(String label, final int rcCode, Color color) {
		// Bindestrich durch Streckenstrich ersetzen (ist laenger, Bindestrich
		// sieht so doof aus neben den grossen Pluszeichen)
		label = label.replaceAll("-", "\u2013");
		JButton rv = new JButton(label);
		rv.setToolTipText("Code "+rcCode+
			" (0x"+Integer.toHexString(rcCode)+")");
		rv.setForeground(color);
		rv.setBackground(Color.DARK_GRAY);
		rv.addActionListener(new ActionListener() {
			@SuppressWarnings("synthetic-access")
			public void actionPerformed(
			@SuppressWarnings("unused") ActionEvent e) {
				model.setValue(rcCode);
			}
		});
		return rv;
	}

	private JButton b(String label, final int rcCode) {
		return b(label, rcCode,
			currentDefault == null ? Color.LIGHT_GRAY : currentDefault);
	}

	private JComponent grid(int width, int height, JButton... buttons) {
		// Aufpassen: GridLayout(rows, cols), also (Y, X)
		JPanel rv = new JPanel(new GridLayout(height, width, 8, 8));
		for (JButton b : buttons)
			rv.add(b == null ? new JPanel() : b);
		currentDefault = null;
		return rv;
	}

	public RemoteControlViewer(NumberModel model) {
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 5));
		this.model = model;
		for (JComponent c : buttonsAndLayout) {
			if (c != null) {
				add(c);
				add(Box.createVerticalStrut(8));
			}
		}
	}
}

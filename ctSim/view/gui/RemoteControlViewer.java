package ctSim.view.gui;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

import ctSim.model.bots.components.Sensors.RemoteControl;
import ctSim.util.FmtLogger;

/**
 * Fernbedienungs-GUI
 */
public class RemoteControlViewer extends JPanel {
	/** UID */
	private static final long serialVersionUID = - 6483687307396837800L;

	/** Logger */
	static final FmtLogger lg = FmtLogger.getLogger(
		"ctSim.view.gui.RemoteControlViewer");

	/** blau */
	private static final Color LIGHT_BLUE = new Color(150, 150, 255);
	/** gruen */
	private static final Color GR         = new Color(50,  200, 50);
	/** gelb */
	private static final Color YE         = new Color(200, 200, 0);

	/** Fernbedienungs-Komponente */
	private final RemoteControl sensor;
	/** Defaut-Color */
	private Color currentDefault = null;

	/**
	 * Setzt die Standardfarbe
	 * @param c Farbe
	 * @return null
	 */
	private JComponent defaultColor(Color c) {
		currentDefault = c;
		return null;
	}

	/**
	 * Button bauen
	 * @param label Label
	 * @param color Farbe
	 * @return Button
	 */
	private JButton b(String label, Color color) {
		// Bindestrich durch Streckenstrich ersetzen (ist laenger, Bindestrich
		// sieht so doof aus neben den grossen Pluszeichen)
		final String key = label.replaceAll("-", "\u2013");
		final JButton rv = new JButton(key);
//		rv.setToolTipText("Code "+rcCode+
//			" (0x"+Integer.toHexString(rcCode)+")");
		rv.setForeground(color);
		rv.setBackground(Color.DARK_GRAY);
		rv.addActionListener(new ActionListener() {
			@SuppressWarnings("synthetic-access")
			public void actionPerformed(
			@SuppressWarnings("unused") ActionEvent e) {
				lg.fine("Fernbedienungsknopf '%s' gedr\u00FCckt", rv.getText());
				try {
					sensor.send(key);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});
		return rv;
	}

	/**
	 * Button bauen
	 * @param label Label
	 * @return Button
	 */
	private JButton b(String label) {
		return b(label, currentDefault == null ? Color.LIGHT_GRAY : currentDefault);
	}

	/**
	 * Baut das Panel
	 * @param width Breite
	 * @param height Hoehe
	 * @param buttons Buttons
	 * @return Panel
	 */
	private JComponent grid(int width, int height, JButton... buttons) {
		// Aufpassen: GridLayout(rows, cols), also (Y, X)
		JPanel rv = new JPanel(new GridLayout(height, width, 8, 8));
		for (JButton b : buttons)
			rv.add(b == null ? new JPanel() : b);
		currentDefault = null;
		return rv;
	}

	/** Layout der Fernbedienung */
	private JComponent[] layout = new JComponent[] {
		defaultColor(LIGHT_BLUE),
		grid(3, 5,
			null,		null,		b("\u03A6", GR),	// 03A6: "Power-Symbol" (wie Kombination von O und |)
			b( "1"),	b( "2"),	b( "3"),
			b( "4"),	b( "5"),	b( "6"),
			b( "7"),	b( "8"),	b( "9"),
			b("10"),	b("11"),	b("12")
		),
		defaultColor(null),
		grid(4, 1,
			b("GR -", GR),	b("RE +", new Color(200, 50, 50)),
			b("YE -", YE),	b("BL +", LIGHT_BLUE)
		),
		defaultColor(Color.LIGHT_GRAY),
		grid(5, 5,
			b("I/II"),	null,		null,		null,		b("TV/VCR"),
			null,		null,		b("||"),	null,		null,
			null,		b("<<"),	b(">"),		b(">>"),	null,
			null,		null,		b("\u25A1"),null,		null,			// 25A1: Quadrat fuer "Stop"
			b("\u25CF"),null,		null,		null,		b("CH*P/P")		// 25CF: Dicker Punkt fuer "Record"
		),
		defaultColor(LIGHT_BLUE),
		grid(3, 2,
			b("Vol+"),	b("Mute"),	b("Ch+"),
			b("Vol-"),	null,		b("Ch-")
		)
	};	
	
	/**
	 * @param rcSensor RC5-Komponente
	 */
	public RemoteControlViewer(RemoteControl rcSensor) {
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 5));
		this.sensor = rcSensor;
		for (JComponent c : layout) {
			if (c != null) {
				add(c);
				add(Box.createVerticalStrut(8));
			}
		}
	}
	
//	/**
//	 * Erzeugt das Fernbedienungslayout je nach Typ der Fernbedienung (einstellbar wie beim Bot-Code
//	 * ueber die Datei rc5-codes.h).
//	 * @author Timo Sandmann (mail@timosandmann.de)
//	 */
//	private class RemoteControlCodes {
//		/** Layout der Fernbedienung */
//		private JComponent[] layout;
//		
//		/**
//		 * @param type Typ der Fernbedienung (wie in rc5-codes.h beim Bot-Code)
//		 * @throws Exception Falls Typ unbekannt
//		 */
//		private RemoteControlCodes(String type) throws Exception {
//			/* Standard-Fernbedienung */
//			if (type.equals("RC_HAVE_HQ_RC_UNIVERS29_334") || type.equals("")) {
//				layout = new JComponent[] {
//						defaultColor(LIGHT_BLUE),
//						grid(3, 5,
//							// 03A6: "Power-Symbol" (wie Kombination von O und | )
//							null,            null,            b("\u03A6", 0x118C, GR),
//							b("1" , 0x1181), b("2" , 0x1182), b("3" , 0x1183),
//							b("4" , 0x1184), b("5" , 0x1185), b("6" , 0x1186),
//							b("7" , 0x1187), b("8" , 0x1188), b("9" , 0x1189),
//							b("10", 0x1180), b("11", 0x118A), b("12", 0x11A3)
//						),
//						defaultColor(null),
//						grid(4, 1,
//							b("GR -", 0x01BA, GR), b("RE +", 0x01BD, new Color(200, 50, 50)),
//							b("YE -", 0x01B1, YE), b("BL +", 0x01B0, LIGHT_BLUE)
//						),
//						defaultColor(Color.LIGHT_GRAY),
//						grid(5, 5,
//							b("I/II",0x11AB),null,          null,          null,          b("TV/VCR",0x11B8),
//							null,            null,          b("||",0x11A9),null,          null,
//							null,            b("<<",0x11B2),b(">", 0x11B5),b(">>",0x11B4),null,
//							null,            null,          b(qdrt,0x11B6),null,          null,
//							b(recrd,0x11AB), null,          null,          null,          b("CH*P/P",0x11BF)
//						),
//						defaultColor(LIGHT_BLUE),
//						grid(3, 2,
//							b("Vol+", 0x1190), b("Mute", 0x01BF), b("Ch+", 0x11A0),
//							b("Vol-", 0x1191), null,              b("Ch-", 0x11A1)
//						)
//					};
//				return;
//			}
//
//			/* RC_HAVE_HAUPPAUGE_WINTV */
//			if (type.equals("RC_HAVE_HAUPPAUGE_WINTV")) {
//				layout = new JComponent[] {
//						defaultColor(LIGHT_BLUE),
//						grid(3, 5,
//							// 03A6: "Power-Symbol" (wie Kombination von O und | )
//							null,            null,            b("\u03A6", 0x1026, GR),
//							b("1" , 0x1001), b("2" , 0x1002), b("3" , 0x1003),
//							b("4" , 0x1004), b("5" , 0x1005), b("6" , 0x1006),
//							b("7" , 0x1007), b("8" , 0x1008), b("9" , 0x1009),
//							b("10", 0x1000), null, null
//						),
//						defaultColor(Color.LIGHT_GRAY),
//						grid(5, 4,
//							b("I/II",0x1022),null,          null,          null,          b("TV/VCR",0x102e),
//							null,            null,          b("||",0x1020),null,          null,
//							null,            b("<<",0x1011),b(">", 0xffff),b(">>",0x1010),null,
//							null,            null,          b(qdrt,0x1021),null,          null
//						)
//					};
//				return;
//			}
//			
//			/* RC_HAVE_HAUPPAUGE_MediaMPV */
//			if (type.equals("RC_HAVE_HAUPPAUGE_MediaMPV")) {
//				layout = new JComponent[] {
//						defaultColor(LIGHT_BLUE),
//						grid(3, 5,
//							// 03A6: "Power-Symbol" (wie Kombination von O und | )
//							null,            null,            b("\u03A6", 0x17fd, GR),
//							b("1" , 0x17c1), b("2" , 0x17c2), b("3" , 0x17c3),
//							b("4" , 0x17c4), b("5" , 0x17c5), b("6" , 0x17c6),
//							b("7" , 0x17c7), b("8" , 0x17c8), b("9" , 0x17c9),
//							b("10", 0x17c0), null,			  null
//						),
//						defaultColor(null),
//						grid(4, 1,
//							b("GR -", 0x17ee, GR), b("RE +", 0x17cb, new Color(200, 50, 50)),
//							b("YE -", 0x17f8, YE), b("BL +", 0x17e9, LIGHT_BLUE)
//						),
//						defaultColor(Color.LIGHT_GRAY),
//						grid(5, 5,
//							b("I/II",0xffff),null,          null,          null,          b("TV/VCR",0x17cc),
//							null,            null,          b("||",0x17e0),null,          null,
//							null,            b("<<",0x17d1),b(">", 0x17f5),b(">>",0x17d0),null,
//							null,            null,          b(qdrt,0x17e1),null,          null,
//							b(recrd,0x17f7), null,          null,          null,          b("CH*P/P",0xffff)
//						),
//						defaultColor(LIGHT_BLUE),
//						grid(3, 2,
//							b("|<", 0x17e4), b("Mute", 0x07cf), b("Ch+", 0x17f2),
//							b(">|", 0x17de), null,              b("Ch-", 0x17f4)
//						)
//					};
//				return;
//			}
//			
//			/* Typ nicht gefunden */
//			throw new Exception("Unbekannte Fernbedienung \"" + type + "\"");
//		}
//		
//		/**
//		 * Gibt das Layout der Fernbedienung zurueck, so wie der Viewer es benoetigt
//		 * @return das Layout dieser Instanz
//		 */
//		private JComponent[] getLayout() {
//			return layout;
//		}
//	}
}

package ctSim.view;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class Console extends Box implements DebugWindow {
	
	private JTextArea console;
	
	Console() {
		
		super(BoxLayout.PAGE_AXIS);
		
		this.console = new JTextArea(3, 40);
		this.console.setEditable(false);
		this.console.setBorder(BorderFactory.createEtchedBorder());
		
		JScrollPane scroll = new JScrollPane(this.console, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		//this.console.setBorder(BorderFactory.create)
		
		this.add(scroll);
		
		//this.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Console"));
		this.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
		
	}
	
	public void print(String str) {
		
		this.console.append(str);
	}

	public void println(String str) {
		
		this.console.append(str);
		this.console.append("\n");
	}
}

package ctSim.view;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JEditorPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLFrameHyperlinkEvent;

public class WelcomeScreen extends Box {
	
	private static final long serialVersionUID = 5538112396754476053L;
	
	private JEditorPane editor;
	
	WelcomeScreen(String text) {
		
		super(BoxLayout.PAGE_AXIS);
		
		this.editor = new JEditorPane("text/html", text);
		this.editor.setEditable(false);
		//this.editor.setPreferredSize();
		
		this.editor.addHyperlinkListener(new HyperlinkListener() {
			 
	         public void hyperlinkUpdate(HyperlinkEvent e) {
	             if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
	                 JEditorPane pane = (JEditorPane) e.getSource();
	                 if (e instanceof HTMLFrameHyperlinkEvent) {
	                     HTMLFrameHyperlinkEvent  evt = (HTMLFrameHyperlinkEvent)e;
	                     HTMLDocument doc = (HTMLDocument)pane.getDocument();
	                     doc.processHTMLFrameHyperlinkEvent(evt);
	                 } else {
	                     try {
	                         pane.setPage(e.getURL());
	                     } catch (Throwable t) {
	                         t.printStackTrace();
	                     }
	                 }
	             }
	         }
		});
		
		this.add(this.editor);
	}
}
package ctSim.Model;

import javax.media.j3d.TransformGroup;

/**
 * Hilfsklasse, die die Hooks fuer jedem Bot verwaltet 
 * @author bbe (bbe@heise.de)
 *
 */
public class ViewBot{
	/** Kennung des Bots */
	String id;
	/** TranslationsGruppe des Bots */
	TransformGroup tg;
	/** RotationsGruppe des Bots */
	TransformGroup rg;
	
	/** Erzeugt einen neuen Bot-Eintrag
	 * 
	 * @param botId ID des Bots
	 * @param tg seine Transformgroup
	 */
	public ViewBot(String botId, TransformGroup tg,TransformGroup rg) {
		id= botId;
		this.tg = tg;
		this.rg = rg;
	}
}


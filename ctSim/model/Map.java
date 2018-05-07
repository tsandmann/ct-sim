/*
 * c't-Sim - Robotersimulator für den c't-Bot
 *
 * This program is free software; you can redistribute it
 * and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your
 * option) any later version.
 * This program is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public
 * License along with this program; if not, write to the Free
 * Software Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 * MA 02111-1307, USA.
 *
 */

package ctSim.model;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import ctSim.model.bots.Bot;

/**
 * Modell der Karte, die der Bot von der Umgebung erstellt (MAP_AVAILABLE)
 * 
 * @author Timo Sandmann (mail@timosandmann.de)
 */
public class Map {

	/** Größe der Karte [m] */
	private final float size;
	/** Auflösung der Karte [Punkte / m] */
	private final int resolution;
	/** Kantenlänge einer Section [Punkte] */
	private final int section_points;
	/** Kantenlänge eines Makroblocks [Punkte] */
	private final int macroblock_length;
	
	/** Makroblöcke der kompletten Karte */
	private Macroblock[][] macroblocks;
	
	/**
	 * Ein Makroblock stellt einen macroblock_length * macroblock_length Byte großen Teil der Map dar
	 * und enthält die Sections der Map.
	 */
	private class Macroblock {
		/** Sections dieses Makroblocks */
		private Section[][] sections;
		
		/** Erzeugt einen neuen Makroblock */
		public Macroblock() {
			int length_in_sections = macroblock_length / section_points;
			this.sections = new Section[length_in_sections][length_in_sections];
			for (int i=0; i<length_in_sections; i++) {
				for (int j=0; j<length_in_sections; j++) {
					this.sections[i][j] = new Section();
				}
			}
		}
		
		/**
		 * Liefert die Section in der das Feld (x|y) liegt
		 * 
		 * @param x	X-Koordinate relativ zum Makroblock
		 * @param y	Y-Koordinate relativ zum Makroblock
		 * @return Section des gewünschten Feldes
		 * @throws MapException	falls auf eine Section außerhalb des Makroblocks zugegriffen wird
		 */
		public Section getSection(int x, int y) throws MapException {
			int index_x = x / section_points;
			int index_y = y / section_points;
			
			if (index_x >= this.sections.length || index_y >= this.sections.length) {
				throw new MapException("Zugriff auf eine Section ausserhalb des Makroblocks " + this);
			}
			
			return this.sections[index_x][index_y];
		}
		
		/**
		 * Schreibt die Daten (aller Sections) dieses Makroblocks in einen Byte-Stream
		 * 
		 * @param stream	Stream, dem die Makroblock-Daten angehängt werden
		 * @throws IOException	falls beim Schreiben in den Stream ein Fehler auftritt
		 */
		public void toByteStream(OutputStream stream) throws IOException {
			for (Section[] row : this.sections) {
				for (Section section : row) {
					section.toByteStream(stream);
				}
			}
		}
	}
	
	/**
	 * Eine Section stellt einen section_points * section_points Byte großen Teil der Map dar,
	 * enthält die Felder der Map und liegt in einem Makroblock. 
	 */
	private class Section {
		/** Array mit den Map-Daten dieser Section */
		private byte[][] fields;
		
		/**
		 * Erstellt eine neue Section der Größe section_points * section_points
		 */
		public Section() {
			this.fields = new byte[section_points][section_points];
		}
		
		/**
		 * Gibt einen Feld-Wert zurück
		 * 
		 * @param x	X-Index des Feldes innerhalb der Section
		 * @param y	Y-Index des Feldes innerhalb der Section
		 * @return Feld (x|y) dieser Section
		 * @throws MapException	falls auf ein Feld außerhalb der Section zugegriffen wird
		 */
		public byte getField(int x, int y) throws MapException {
			if (x >= section_points || y >= section_points) {
				throw new MapException("Zugriff auf ein Feld ausserhalb der Section " + this);
			}
			return fields[x][y];
		}
		
		/**
		 * Schreibt einen Feld-Wert in die Section
		 * 
		 * @param x		X-Index des Feldes innerhalb der Section
		 * @param y		Y-Index des Feldes innerhalb der Section
		 * @param value zu schreibender Wert
		 * @throws MapException	falls auf ein Feld außerhalb der Section zugegriffen wird
		 */
		public void setField(int x, int y, byte value) throws MapException {
			if (x >= section_points || y >= section_points) {
				throw new MapException("Zugriff auf ein Feld ausserhalb der Section " + this);
			}
			fields[x][y] = value;
		}
		
		/**
		 * Schreibt die Daten dieser Section in einen Byte-Stream
		 * 
		 * @param stream	Stream, dem die Section-Daten angehängt werden
		 * @throws IOException	falls beim Schreiben in den Stream ein Fehler auftritt
		 */
		public void toByteStream(OutputStream stream) throws IOException {
			for (int y=0; y<section_points; y++) {
				for (int x=0; x<section_points; x++) {
					stream.write(fields[x][y]);
				}
			}
		}
	}
	
	/**
	 * Erstellt eine leere Map mit den folgenden Parametern:
	 * 
	 * @param size				Größe der Karte [m]
	 * @param resolution		Auflösung der Karte [Punkte / m]
	 * @param section_points	Kantenlänge einer Section [Punkte]
	 * @param macroblock_length	Kantenlänge eines Macroblocks [Punkte]
	 */
	public Map(float size, int resolution, int section_points, int macroblock_length) {
		this.size = size;
		this.resolution = resolution;
		this.section_points = section_points;
		this.macroblock_length = macroblock_length;
		
		final int length_in_macroblocks = (int)(this.size * this.resolution / this.macroblock_length);
		this.macroblocks = new Macroblock[length_in_macroblocks][length_in_macroblocks];
		// Makroblöcke werden on demand angelegt in access_field()
	}
	
	/**
	 * Greift auf ein Map-Feld lesend oder schreibend zu
	 * 
	 * @param x		Map-Koordinate X
	 * @param y		Map-Koordinate Y
	 * @param value	zu schreibender Wert (falls set == true)
	 * @param set	lesender (false) oder schreibender (true) Zugriff
	 * @return Wert des Feldes (fall set == false)
	 * @throws MapException	falls auf ein Feld außerhalb der Karte zugegriffen wird
	 */
	private byte access_field(int x, int y, byte value, boolean set) throws MapException {
		int mb_index_x = x / macroblock_length;
		int mb_index_y = y / macroblock_length;
		if (mb_index_x >= this.macroblocks.length || mb_index_y >= this.macroblocks.length) {
			throw new MapException("Zugriff auf ein Feld ausserhalb der Karte");
		}
		
		int s_index_x = x % macroblock_length;
		int s_index_y = y % macroblock_length;
		
		if (this.macroblocks[mb_index_x][mb_index_y] == null) {
			this.macroblocks[mb_index_x][mb_index_y] = new Macroblock();
		}
		
		Section section = this.macroblocks[mb_index_x][mb_index_y].getSection(s_index_x, s_index_y);
		
		int f_index_x = x % section_points;
		int f_index_y = y % section_points;
		
		if (set) {
			section.setField(f_index_x, f_index_y, value);
			return 0;
		}
		
		return section.getField(f_index_x, f_index_y);
	}
	
	/** 
	 * Wandelt eine Welt-Koordinate in eine Map-Koordinate um
	 * 
	 * @param koord	Welt-Koordinate
	 * @return Map-Koordinate
	 */
	private int world_to_map(int koord) {
		int tmp = koord + (int)(this.size * this.resolution * 4.0);
		return tmp / (1000 / this.resolution);
	}
	
	/**
	 * Trägt die Daten eines Parcours in die Karte ein. Als Urpsrung wird das Startfeld verwendet,
	 * das zum Bot der angegebenen Nr. gehört.
	 * 
	 * @param parcours	zu verwendender Parcours 
	 * @param bot		Bot-Nr., dessen Startfeld als Koordinatenursprung der Map benutzt wird
	 * @param free		Wert, mit dem freie Felder eingetragen werden (z.B. 100)
	 * @param occupied	Wert, mit dem Hindernisse eingetragen werden (z.B. -100)
	 * @throws MapException	im Fehlerfall
	 */
	public void createFromParcours(Parcours parcours, int bot, int free,
			int occupied) throws MapException {

		if (parcours.getHeightInM() > this.size
				|| parcours.getHeightInM() > this.size) {
			/* Parcours ist zu groß */
			throw new MapException("Parcours " + parcours
					+ " ist zu gross, max " + this.size + " m x " + this.size
					+ " m möglich");
		}

		if (bot >= Parcours.BOTS) {
			/* ungültige Bot-Nr. */
			throw new MapException("Bot-Nr. ist zu gross");
		}

		int simpleParcours[][] = parcours.getFlatParcoursWithHoles();

		int length_x = simpleParcours.length;
		int length_y = simpleParcours[0].length;
		int world_length_x = parcours.blockToWorld(length_x);

		int start[] = parcours.getStartPositions(bot);
		int world_start_x = parcours.blockToWorld(start[0])
				+ parcours.getBlockSizeInMM() / 2;
		int world_start_y = parcours.blockToWorld(start[1])
				+ parcours.getBlockSizeInMM() / 2;
		int world_diff_x = -(world_length_x - world_start_x);

		for (int x = 0; x < length_x; x++) {
			int world_x = parcours.blockToWorld(x) - world_diff_x;
			for (int y = 0; y < length_y; y++) {
				int world_y = parcours.blockToWorld(y) - world_start_y;
				byte value;
				switch (simpleParcours[x][y]) {
				case 0:
					/* frei */
					value = (byte) free;
					break;
				case 1:
					/* Hindernis */
					value = (byte) occupied;
					break;
				case 2:
					/* Loch */
					value = -128;
					break;
				default:
					/* Fehler */
					throw new MapException(
							"Unbekannte Daten im Simple-Parcours");
				}
				for (int i = 0; i < parcours.getBlockSizeInMM(); i += 1000 / this.resolution) {
					int map_x = world_to_map(world_length_x - (world_x + i)); // an Y-Achse spiegeln
					for (int j = 0; j < parcours.getBlockSizeInMM(); j += 1000 / this.resolution) {
						int map_y = world_to_map(world_y + j);
						access_field(map_x, map_y, value, true);
					}
				}
			}
		}
	}
	
	/**
	 * Trägt die Daten eines Parcours in die Karte ein. Als Urpsrung wird das Startfeld 
	 * des angegebenen Bots verwendet.
	 * 
	 * @param parcours	zu verwendender Parcours
	 * @param bot		Bot, dessen Startfeld als Koordinatenursprung der Map benutzt wird
	 * @param free		Wert, mit dem freie Felder eingetragen werden (z.B. 100)
	 * @param occupied	Wert, mit dem Hindernisse eingetragen werden (z.B. -100)
	 * @throws MapException	im Fehlerfall
	 */
	public void createFromParcours(Parcours parcours, Bot bot, int free,
			int occupied) throws MapException {

		int bot_nr = parcours.getStartPositionNumber(bot);
		createFromParcours(parcours, bot_nr, free, occupied);
	}
	
	/**
	 * Schreibt die komplette Map in einen Byte-Stream
	 * 
	 * @param stream	Stream, dem die Map-Daten angehängt werden
	 * @throws IOException	falls beim Schreiben in den Stream ein Fehler auftritt
	 */
	private void toByteStream(OutputStream stream) throws IOException {
		/** Nicht allokierte Makrobloecke werden als dieser null-dumm geschrieben */
		byte dummy[] = new byte[this.macroblock_length * this.macroblock_length];
		for (Macroblock[] row : this.macroblocks) {
			for (Macroblock macroblock : row) {
				if (macroblock != null) {
					macroblock.toByteStream(stream);
				} else {
					stream.write(dummy);
				}
			}
		}
	}
	
	/**
	 * Exportiert die Map in eine Datei (Bot-Format)
	 * 
	 * @param file	Datei
	 * @throws IOException	falls beim Schreiben in die Datei etwas schief ging
	 */
	public void exportToFile(File file) throws IOException {
		OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
		byte header[] = new byte[512];
		header[0] = 'M';
		header[1] = 'A';
		header[2] = 'P';
		out.write(header);
		toByteStream(out);
		out.close();
	}
	
	/**
	 * Exportiert die Map in eine auszuwählende Datei (Bot-Format)
	 * 
	 * @throws IOException	falls beim Schreiben in die Datei etwas schief ging
	 */
	public void export() throws IOException {
		JFileChooser fc = new JFileChooser();
		fc.setFileFilter(new FileFilter() {
			@Override
			public boolean accept(File f) {
				return (f.isDirectory() || f.getName().endsWith(".map"));
			}
			@Override
			public String getDescription() {
				return "Map-Dateien (*.map)";
			}
		});
		File dir = new File("maps");
		fc.setCurrentDirectory(dir);
		int userChoice = fc.showSaveDialog(null);
		if (userChoice != JFileChooser.APPROVE_OPTION) {
			// Benutzer hat abgebrochen
			return;
		}
		File file = fc.getSelectedFile();
		if (!file.exists() && !file.getName().endsWith(".map")) {
			file = new File(file.getPath() + ".map");
		}
		exportToFile(file);
	}
	
	/** Map-Exceptions */
	public class MapException extends Throwable {
		/** ID */
		private static final long serialVersionUID = 141554206659442950L;

		/** Map-Exception ohne Fehlermeldung */
		public MapException() {
			super();
		}

		/**
		 * Map-Exception mit Fehlermeldung
		 * 
		 * @param msg	Fehlermeldung
		 */
		public MapException(String msg) {
			super(msg);
		}
	}
}

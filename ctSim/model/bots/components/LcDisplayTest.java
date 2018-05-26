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

package ctSim.model.bots.components;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import ctSim.model.bots.components.Actuators.LcDisplay;

/** Unit-Test für LcDisplay */
public class LcDisplayTest extends LcDisplay {
	/** Testdaten */
	private final String twentyFourLines;

	/** Display-Test */
	public LcDisplayTest() {
		super(42, 24);
		String fortyTwoSpaces = "";
		for (int i = 0; i < 42; i++)
			fortyTwoSpaces += " ";
		String s = "";
		for (int i = 0; i < 24; i++)
			s += fortyTwoSpaces + "\n";
		twentyFourLines = s.substring(0, s.length() - 1);
	}

	/** Testcode */
	@Test
	public void ctor() {
		assertEquals(42, getNumCols());
		assertEquals(24, getNumRows());
	}

	/**
	 * Testcode
	 *
	 * @throws Exception
	 */
	@Test
	public void clearModel() throws Exception {
		clearModel(getExternalModel());
		assertEquals(twentyFourLines, getAllText(getExternalModel()));
	}

	/**
	 * Testcode
	 *
	 * @throws Exception
	 */
	@Test
	public void internalModelIsDifferentFromExternalOne()
	throws Exception {
		clearModel(getExternalModel());
		setCursor(3, 3);
		overwrite("Katastrophenfalleinsatzbehörde");
		// hier nicht updateExternalModel()
		assertEquals(twentyFourLines, getExText());
	}

	/** Testdaten */
	private String insertion = "beeblebrox";

	/**
	 * Testcode
	 *
	 * @throws Exception
	 */
	@Test
	public void setTextAtAll() throws Exception {
		// Einfügen bei 0,0 - geht das überhaupt?
		setCursor(0, 0);
		overwrite(insertion);
		updateExternalModel();
		String expected = twentyFourLines.replaceFirst("^ {" + insertion.length() + "}", insertion);
		assertEquals(expected, getExText());
	}

	/**
	 * Testcode
	 *
	 * @throws Exception
	 */
	@Test
	public void setTextColCorrect() throws Exception {
		// Einfügen bei 10,0 - kommt das in der richtigen Spalte an?
		setCursor(10, 0);
		overwrite(insertion);
		updateExternalModel();
		String expected = twentyFourLines;
		expected = expected.substring(0, 10) + insertion +
				expected.substring(10 + insertion.length(), expected.length());
		assertEquals(expected, getExText());
	}

	/**
	 * Testcode
	 *
	 * @throws Exception
	 */
	@Test
	public void setTextRowCorrect() throws Exception {
		// Einfügen bei 0,8 - kommt das in der richtigen Zeile an?
		clearModel(getExternalModel());
		setCursor(0, 8);
		overwrite(insertion);
		updateExternalModel();
		String expected = twentyFourLines.substring(0, 43*8) + insertion +
				twentyFourLines.substring(43*8 + insertion.length(), twentyFourLines.length());
		assertEquals(expected, getExText());
	}

	/**
	 * Testcode
	 *
	 * @throws Exception
	 */
	@Test
	public void setTextTruncate() throws Exception {
		// Einfügen bei 38,23 - wird das richtig abgeschnitten?
		clearModel(getExternalModel());
		setCursor(38, 23);
		overwrite(insertion);
		updateExternalModel();
		String expected = twentyFourLines.substring(0, 43*23 + 38) + insertion.substring(0, 4);
		assertEquals(expected, getExText());
	}

	/**
	 * @return Text
	 *
	 * @throws Exception
	 */
	private String getExText() throws Exception {
		return getAllText(getExternalModel());
	}
}

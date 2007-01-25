package ctSim.model.bots.components;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import ctSim.model.bots.components.Actuators.LcDisplay;

/** Unit-Test fuer LcDisplay */
public class LcDisplayTest extends LcDisplay {
	private final String twentyFourLines;

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

	@Test
	public void ctor() {
		assertEquals(42, getNumCols());
		assertEquals(24, getNumRows());
	}

	@Test
	public void clearModel() throws Exception {
		clearModel(getExternalModel());
		assertEquals(twentyFourLines, getAllText(getExternalModel()));
	}
	
	@Test
	public void internalModelIsDifferentFromExternalOne() 
	throws Exception {
		clearModel(getExternalModel());
		setCursor(3, 3);
		overwrite("Katastrophenfalleinsatzbehoerde");
		// hier nicht updateExternalModel()
		assertEquals(twentyFourLines, getExText());
	}

	private String insertion = "beeblebrox";
	
	@Test
	public void setTextAtAll() throws Exception {
		// Einfuegen bei 0,0 -- geht's ueberhaupt
		setCursor(0, 0);
		overwrite(insertion);
		updateExternalModel();
		String expected = twentyFourLines.replaceFirst(
			"^ {" + insertion.length() + "}", insertion);
		assertEquals(expected, getExText());
	}
	
	@Test
	public void setTextColCorrect() throws Exception {
		// Einfuegen bei 10,0 -- kommt das in der richtigen Spalte an
		setCursor(10, 0);
		overwrite(insertion);
		updateExternalModel();
		String expected = twentyFourLines;
		expected = expected.substring(0, 10) + insertion +
			expected.substring(10 + insertion.length(), expected.length());
		assertEquals(expected, getExText());
	}
	
	@Test
	public void setTextRowCorrect() throws Exception {
		// Einfuegen bei 0,8 -- kommt das in der richtigen Zeile an
		clearModel(getExternalModel());
		setCursor(0, 8);
		overwrite(insertion);
		updateExternalModel();
		String expected = twentyFourLines.substring(0, 43*8) + insertion +
		twentyFourLines.substring(43*8 + insertion.length(),
			twentyFourLines.length());
		assertEquals(expected, getExText());
	}
	
	@Test
	public void setTextTruncate() throws Exception {
		// Einfuegen bei 38,23 -- wird das richtig abgeschnitten
		clearModel(getExternalModel());
		setCursor(38, 23);
		overwrite(insertion);
		updateExternalModel();
		String expected = twentyFourLines.substring(0, 43*23 + 38) +
			insertion.substring(0, 4);
		assertEquals(expected, getExText());
	}
	
	private String getExText() throws Exception {
		return getAllText(getExternalModel());
	}
}
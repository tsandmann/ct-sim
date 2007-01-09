package ctSim.model.bots.components.actuators;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

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
	public void testCtor() {
		assertEquals(42, getNumCols());
		assertEquals(24, getNumRows());
	}

	@Test
	public void testClear() throws Exception {
		clearModel();
		assertEquals(twentyFourLines, getAllText());
	}

	private String getAllText() throws Exception {
		return getModel().getText(0, getModel().getLength());
	}

	@Test
	public void testSetText() throws Exception {
		String insertion = "beeblebrox";
		String expected = twentyFourLines;

		// Einfuegen bei 0,0 -- geht's ueberhaupt
		expected = expected.replaceFirst("^ {" + insertion.length() + "}",
			insertion);
		setCursor(0, 0);
		overwrite(insertion);
		assertEquals(expected, getAllText());

		// Einfuegen bei 10,0 -- kommt das in der richtigen Spalte an
		expected = expected.substring(0, 10) + insertion +
			expected.substring(10 + insertion.length(), expected.length());
		setCursor(10, 0);
		overwrite(insertion);
		assertEquals(expected, getAllText());

		// Einfuegen bei 0,8 -- kommt das in der richtigen Zeile an
		clearModel();
		expected = twentyFourLines.substring(0, 43*8) + insertion +
			twentyFourLines.substring(43*8 + insertion.length(),
			twentyFourLines.length());
		setCursor(0, 8);
		overwrite(insertion);
		assertEquals(expected, getAllText());

		// Einfuegen bei 38,23 -- wird das richtig abgeschnitten
		clearModel();
		expected = twentyFourLines.substring(0, 43*23 + 38) +
			insertion.substring(0, 4);
		setCursor(38, 23);
		overwrite(insertion);
		assertEquals(expected, getAllText());
	}
}

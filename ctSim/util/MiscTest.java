package ctSim.util;

import org.junit.Test;
import static ctSim.util.Misc.beginsWith;
import static org.junit.Assert.*;

public class MiscTest {
	@Test
	public void beginWith() {
		// einer auf der Blacklist
		assertTrue(beginsWith("uuu", "u"));
		assertTrue(beginsWith("u", "u"));
		assertTrue(beginsWith("uuu", ""));
		assertTrue(beginsWith("", ""));
		assertTrue(beginsWith("uuu", "uuu"));

		assertFalse(beginsWith("uuu", "uuuu"));
		assertFalse(beginsWith("uuu", "e"));
		assertFalse(beginsWith("ttt", "T"));
		assertFalse(beginsWith("uuu", (String[])null));
		assertFalse(beginsWith("", "u"));

		// mehrere auf der Blacklist
		assertTrue(beginsWith("222", "2", "7"));
		assertTrue(beginsWith("222", "7", "2"));
		assertTrue(beginsWith("222", null, "7", "22", ""));

		assertFalse(beginsWith("222", "9", "7"));
		assertFalse(beginsWith("222", "9", "7", null, "3"));
		assertFalse(beginsWith("222", "9", "7", null, "2222"));
	}
}

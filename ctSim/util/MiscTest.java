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

package ctSim.util;

import static ctSim.util.Misc.intersperse;
import static ctSim.util.Misc.join;
import static ctSim.util.Misc.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

/** Test-Klasse */
public class MiscTest {
	/** Tests */
	@Test
	public void beginWith() {
		// einer auf der Whitelist
		assertTrue(startsWith("uuu", "u"));
		assertTrue(startsWith("u", "u"));
		assertTrue(startsWith("uuu", ""));
		assertTrue(startsWith("", ""));
		assertTrue(startsWith("uuu", "uuu"));

		assertFalse(startsWith("uuu", "uuuu"));
		assertFalse(startsWith("uuu", "e"));
		assertFalse(startsWith("ttt", "T"));
		assertFalse(startsWith("uuu", (String[])null));
		assertFalse(startsWith("", "u"));

		// mehrere auf der Whitelist
		assertTrue(startsWith("222", "2", "7"));
		assertTrue(startsWith("222", "7", "2"));
		assertTrue(startsWith("222", null, "7", "22", ""));

		assertFalse(startsWith("222", "9", "7"));
		assertFalse(startsWith("222", "9", "7", null, "3"));
		assertFalse(startsWith("222", "9", "7", null, "2222"));
	}

	/** Tests */
	@Test(expected=NullPointerException.class)
	public void intersperseWithNullSep() {
		intersperse(null, "a");
	}

	/** Tests */
	@Test
	public void testIntersperse() {
		String[] str;

		str = intersperse("u", "a");
		assertEquals(1, str.length);
		assertTrue(Arrays.equals(str, new String[] {"a"}));

		str = intersperse("m", null, "b");
		assertEquals(3, str.length);
		assertTrue(Arrays.equals(str, new String[] {null, "m", "b"}));

		str = intersperse("u", "a", "b", "c");
		assertEquals(5, str.length);
		assertTrue(Arrays.equals(str, new String[] {"a", "u", "b", "u", "c"}));

		str = intersperse("u", "a", "b", "c", "d", "e", "f");
		assertEquals(11, str.length);
		assertTrue(Arrays.equals(str, new String[] {"a", "u", "b", "u", "c",
			"u", "d", "u", "e", "u", "f"}));

		str = intersperse("f", null, null);
		assertEquals(3, str.length);
		assertTrue(Arrays.equals(str, new String[] {null, "f", null}));

		Integer[] intg = intersperse(42, 1, 2);
		assertEquals(3, intg.length);
		assertTrue(Arrays.equals(intg, new Integer[] {1, 42, 2}));
	}

	/** Tests */
	@Test
	public void testJoin() {
		assertEquals("a", join("a"));
		assertEquals("abc", join("a", "b", "c"));
		assertEquals("lkjjkl", join("lkj", "jkl"));
		assertEquals("null", join((String)null));
		assertEquals("anullu", join("a", null, "u"));
	}
}

package ctSim.util;

import java.lang.reflect.Array;

//$$ doc Misc
public class Misc {
	public static boolean startsWith(
			String fullString, String... whitelistOfBeginnings) {
		if (whitelistOfBeginnings == null)
			return false;
		for (String shorter : whitelistOfBeginnings) {
			if (shorter == null)
				continue;
			if (fullString.length() < shorter.length())
				continue;
			if (fullString.substring(0, shorter.length()).equals(shorter))
				return true;
		}
		return false;
	}

	// throws NullPtr wenn separator == null
	@SuppressWarnings("unchecked")
    public static <T> T[] intersperse(T separator, T... stuff) {
		T[] rv = (T[])Array.newInstance(separator.getClass(),
			stuff.length * 2 - 1);
		for (int i = 0; i < stuff.length; i++) {
			rv[i * 2] = stuff[i];
			if (i + 1 < stuff.length)
				rv[i * 2 + 1] = separator;
		}
		return rv;
	}

	public static String join(String... strings) {
		StringBuilder b = new StringBuilder();
		for (String s : strings)
			b.append(s);
		return b.toString();
	}
}

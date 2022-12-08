package dt.jdictionary;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Utils 
{
	// Blind copy and paste.
	public static boolean hasChinese(String string)
	{
		return string.codePoints().anyMatch(codepoint -> Character.UnicodeScript.of(codepoint) == Character.UnicodeScript.HAN);
	}

	/**
	 * Java doesn't handle 4 byte encoded Chinese characters very well.
	 * Char at returns bogus, string length "inflates" among other problems.
	 * Attempt to detect these 4 byte characters and make a "true" char by char decomposition of the string.
	 * @param string
	 * @return list of strings where each entry is an actual char in the string and not what Java thinks the actual char is.
	 */
	public static List<String> trueChars(String string)
	{
		final int UNKNOWN_JCHARS = 2; // majority of the time this seems to be right
		String accumulator = "";

		final List<String> result = new ArrayList<>();
		for(int i=0; i<string.length(); i++)
		{
			final char singleChar = string.charAt(i);
			final String single = Character.toString(singleChar);
			if(Character.UnicodeScript.of(string.charAt(i)) != Character.UnicodeScript.UNKNOWN)
			{
				result.add(single);
			}
			else
			{
				accumulator = accumulator + single;
			}

			if(accumulator.length() == UNKNOWN_JCHARS)
			{
				result.add(accumulator);
				accumulator = "";
			}
		}

		if(accumulator.length() > 0)
		{
			result.add(accumulator);
		}
		return result;
	}

	public static String printBytes(byte[] bytes)
	{
		String result = "[";
		for(byte b : bytes)
		{
			result = result + String.format("%02X", b) + " ";
		}
		return result.substring(0, result.length()-1) + "]";
	}

	public static void logTimestamp(String message)
	{
		System.out.println(Instant.now() + " " + message);
	}
}

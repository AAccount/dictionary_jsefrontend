package dt.jdictionary;

import java.time.Instant;

public class Utils 
{
	// Blind copy and paste.
	public static boolean hasChinese(String string)
	{
		return string.codePoints().anyMatch(codepoint -> Character.UnicodeScript.of(codepoint) == Character.UnicodeScript.HAN);
	}

	public static void logTimestamp(String message)
	{
		System.out.println(Instant.now() + " " + message);
	}
}

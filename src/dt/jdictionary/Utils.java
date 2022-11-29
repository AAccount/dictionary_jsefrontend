package dt.jdictionary;

public class Utils 
{
	// Blind copy and paste.
	public static boolean hasChinese(String string)
	{
		return string.codePoints().anyMatch(codepoint -> Character.UnicodeScript.of(codepoint) == Character.UnicodeScript.HAN);
	}
}

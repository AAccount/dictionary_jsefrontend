package dt.jdictionary.util;

import java.time.Instant;

public class Debug
{

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

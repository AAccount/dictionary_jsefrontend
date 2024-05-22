package dt.jdictionary;

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import dt.jdictionary.ui.UiConstants;

public class Utils 
{
	// Blind copy and paste.
	public static boolean hasChinese(String string)
	{
		return string.codePoints().anyMatch(codepoint -> Character.UnicodeScript.of(codepoint) == Character.UnicodeScript.HAN);
	}

	public static boolean allChinese(String string)
	{
		return string.codePoints().allMatch(codepoint -> Character.UnicodeScript.of(codepoint) == Character.UnicodeScript.HAN);
	}

	public static String normalizePinyin(String pinyin)
	{
		return Normalizer.normalize(pinyin.toLowerCase().strip(), Form.NFD).replaceAll("\\p{M}", "");
	}

	public static <T> List<List<T>> subdivideList(List<T> original, int subSize)
	{
		final List<List<T>> result = new ArrayList<>();
		int position = 0;
		while(position < original.size())
		{
			final int end = (position + subSize) > original.size() ? original.size() : position + subSize;
			result.add(original.subList(position, end));
			position = end;
		}
		return result;
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
	
	public static double stdev(Collection<Double> numbers)
	{
		final double mean = average(numbers);
		final double sd = numbers.stream().collect(Collectors.summingDouble(val -> Math.pow(val - mean, 2)));
		return Math.sqrt(sd / numbers.size());
	}
	
	public static double average(Collection<Double> numbers)
	{
		return numbers.stream().mapToDouble(i -> i).average().orElse(0);
	}
	
	public static String autoSwapChinese(String zh)
	{
		if(!UiConstants.flagMap.get(UiConstants.FLAG_AUTOSWAP))
		{
			return zh;
		}
		
		final Map<String, String> autoSwaps = Map.of(
				"着", "著",
				"爲", "為",
				"僞", "偽",
				"泄", "洩"
				);
		
		String result = zh;
		for(final String source : autoSwaps.keySet())
		{
			result = result.replace(source, autoSwaps.get(source));
		}
		
		if(!result.equals(zh))
		{
			Utils.logTimestamp("Swapped " + zh + " for " + result);
		}
		return result;
	}
}

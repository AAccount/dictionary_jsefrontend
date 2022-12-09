package dt.jdictionary.cedict;

import java.util.Map;

class PinyinParser 
{
	private static final Map<Character, Map<Integer, Character>> TONEMAP = Map.of(
		'a', Map.of(1, 'ā', 2, 'á', 3,'ǎ', 4,'à', 5,'a'),
		'e', Map.of(1, 'ē', 2, 'é', 3,'ě', 4,'è', 5,'e'),
		'i', Map.of(1, 'ī', 2, 'í', 3,'ǐ', 4,'ì', 5,'i'),
		'o', Map.of(1, 'ō', 2, 'ó', 3,'ǒ', 4,'ò', 5,'o'),
		'u', Map.of(1, 'ū', 2, 'ú', 3,'ǔ', 4,'ù', 5,'u'),
		'v', Map.of(1, 'ǖ', 2, 'ǘ', 3,'ǚ', 4,'ǜ', 5,'ǚ')
	);

	public static String parse(String raw)
	{
		final int start = raw.indexOf("[");
		final int end = raw.indexOf("]");
		final int NOT_FOUND = -1;
		if(start == NOT_FOUND || end == NOT_FOUND || start >= end)
		{
			return raw;
		}

		final String recreated = PinyinParser.recreate(raw.substring(start+1, end));
		return raw.substring(0, start) + " " + recreated + " " + raw.substring(end+1);
	}

	private static String recreate(String raw)
	{
		final String nocommas = raw.replaceAll(",", "");
		final String[] words = nocommas.split(" ");
		String result = "";
		for(final String word : words)
		{
			if(word.length() < 1)
			{
				continue;
			}
			final char lastChar = word.charAt(word.length()-1);
			if(word.equals("r5"))
			{
				result = result + "er";
			}
			else if(Character.isDigit(lastChar) && !hasOnlyNumbers(word))
			{
				result = result + " " + recreateWord(word);
			}
			else
			{
				result = result + " " + word;
			}
		}
		return result;	
	}

	private static boolean hasOnlyNumbers(String string)
	{
		return string.matches("[0-9]+");
	}

	private static String recreateWord(String rawWord)
	{
		final String letters = rawWord.substring(0, rawWord.length() - 1);
		final int tone = Integer.parseInt(Character.toString(rawWord.charAt(rawWord.length() - 1)));

		final int NOT_FOUND = -1;
		int iuIndex = NOT_FOUND;

		for(int index=0; index<letters.length(); index++)
		{
			char wordChar = letters.charAt(index);
			if(wordChar == 'a' || wordChar == 'e' || wordChar == 'o')
			{
				return applyToneMap(letters, index, tone);
			}

			if(wordChar == 'i' || wordChar == 'u')
			{
				iuIndex = index;
			}
		}

		if(iuIndex == NOT_FOUND)
		{
			return rawWord;
		}
		return applyToneMap(letters, iuIndex, tone);
	}

	private static String applyToneMap(String letters, int index, int tone)
	{
		char[] underlyingChars = letters.toCharArray();
		final char actualToneChar = letters.contains("u:") ? 'v' : underlyingChars[index];
		underlyingChars[index] = TONEMAP.get(actualToneChar).get(tone);
		return new String(underlyingChars).replace(":", ""); // get rid of the : in u: if it's there
	}
}

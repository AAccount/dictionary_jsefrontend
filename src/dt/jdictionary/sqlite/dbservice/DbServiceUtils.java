package dt.jdictionary.sqlite.dbservice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dt.jdictionary.SimpleLookup;
import dt.jdictionary.sqlite.raw.RawDictionaryRow;

public class DbServiceUtils 
{
	public static final int MIN_SUBSTRING_LENGTH = 2;

	public static List<SimpleLookup> convertRawToSimple(List<RawDictionaryRow> rawResults)
	{
		final List<SimpleLookup> result = new ArrayList<>();
		final Map<String, SimpleLookup> mapper = new HashMap<>();

		for(final RawDictionaryRow rawResult : rawResults)
		{
			final String key = rawResult.getZh() + ":" + rawResult.getPinyin();
			if(!mapper.keySet().contains(key))
			{
				final SimpleLookup simpleLookup = new SimpleLookup(rawResult.getZh(), rawResult.getPinyin(), new ArrayList<>(), rawResult.getRank());
				result.add(simpleLookup);
				mapper.put(key, simpleLookup);
			}
			mapper.get(key).getDefinitions().add(rawResult.getSingleDefinition());
		}

		return result;
	}

	public static List<String> generateSubstrings(String saying)
	{
		// To generate all possible substrings, you will get the original string itself. Don't return that entry.
		final List<String> results = generateSubstringsReal(saying);
		return results.stream().filter(substring -> !substring.equals(saying)).toList();
	}

	private static List<String> generateSubstringsReal(String saying)
	{
		if(saying.length() < DbServiceUtils.MIN_SUBSTRING_LENGTH)
		{
			return List.of();
		}

		final List<String> result = new ArrayList<>();
		for(int i = DbServiceUtils.MIN_SUBSTRING_LENGTH; i <= saying.length(); i++)
		{
			result.add(saying.substring(0, i));
		}
		result.addAll(generateSubstringsReal(saying.substring(1)));
		return result;
	}
}

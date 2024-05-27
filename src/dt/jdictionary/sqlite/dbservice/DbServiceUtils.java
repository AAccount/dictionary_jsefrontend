package dt.jdictionary.sqlite.dbservice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dt.jdictionary.SimpleLookup;
import dt.jdictionary.sqlite.raw.RawDictionaryRow;

public class DbServiceUtils 
{

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
}

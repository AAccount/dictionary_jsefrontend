package dt.jdictionary.sqlite.dbservice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dt.jdictionary.FullLookup;
import dt.jdictionary.SimpleLookup;
import dt.jdictionary.sqlite.dbservice.alternative.AlternateSearch;

public class DbCache 
{
	private final Map<String, Map<String, List<SimpleLookup>>> simpleLookupByProducer = new HashMap<>();
	private final Map<String, FullLookup> fullLookupCache = new HashMap<>();

	public List<SimpleLookup> getSimpleLookup(AlternateSearch altSearch, String zh)
	{
		final String producer = altSearch.getAltSearchType();
		if(!simpleLookupByProducer.containsKey(producer))
		{
			return null;
		}

		final Map<String, List<SimpleLookup>> producerCache = simpleLookupByProducer.get(producer);
		if(!producerCache.containsKey(zh))
		{
			return null;
		}

		return producerCache.get(zh);
	}

	public void setSimpleLookup(AlternateSearch altSearch, String zh, List<SimpleLookup> results)
	{
		final String producer = altSearch.getAltSearchType();
		if(!simpleLookupByProducer.containsKey(producer))
		{
			simpleLookupByProducer.put(producer, new HashMap<>());
		}

		simpleLookupByProducer.get(producer).put(zh, results);
	}

	public FullLookup getFullLookup(String zh)
	{
		if(!fullLookupCache.containsKey(zh))
		{
			return null;
		}
		return fullLookupCache.get(zh);
	}

	public void setFullLookup(String zh, FullLookup result)
	{
		fullLookupCache.put(zh, result);
	}
}

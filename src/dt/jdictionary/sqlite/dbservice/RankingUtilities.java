package dt.jdictionary.sqlite.dbservice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dt.jdictionary.Utils;
import dt.jdictionary.cedict.UnrankedLookup;

public class RankingUtilities
{
	public static Map<Character, Double> rankSingleChars(List<UnrankedLookup> dictionary)
	{
		final Map<Character, Double> rawFreq = countFreq(dictionary);
		final double average = Utils.average(rawFreq.values());
		final double stdev = Utils.stdev(rawFreq.values());
		final double maxRank = average + stdev*3;
		
		// Don't let extreme, high use characters "overpower" any compound word ranking they show up in. 
		for(final char zhchar : rawFreq.keySet())
		{
			if(rawFreq.getOrDefault(zhchar, 0.0) > maxRank)
			{
				rawFreq.put(zhchar, maxRank);
			}
		}
		return rawFreq;
	}
	
	private static Map<Character, Double> countFreq(List<UnrankedLookup> dictionary)
	{
		final Map<Character, Double> freqCountMap = new HashMap<>();
		for(final UnrankedLookup entry : dictionary)
		{
			final String chinese = entry.getZh();
			final char[] chars = chinese.toCharArray();
			for(final char singlechar : chars)
			{
				if(!freqCountMap.containsKey(singlechar))
				{
					freqCountMap.put(singlechar, 0.0);
				}
				final double currentCount = freqCountMap.get(singlechar);
				freqCountMap.put(singlechar, currentCount + 1.0);
			}
		}
		return freqCountMap;
	}
	
	public static double rank(UnrankedLookup unranked, Map<Character, Double> normalizedMap)
	{
		final Set<String> blacklist = Set.of(
				"erhua variant of",
				"species of China", "species of china",
				"County in","county in",
				"Township in", "township in",
				"City in", "city in",
				"Autonomous City", "autonomous city", "Autonomous city", "autonomous City",
				"Autonomous Prefecture", "autonomous prefecture", "Autonomous prefecture", "autonomous Prefecture");
		
		if(unranked.getDefinitions().size() == 1 && blacklist.stream().anyMatch(banned -> unranked.getDefinitions().get(0).indexOf(banned) > -1))
		{
			return -1;
		}
		
		final String zh = unranked.getZh();
		double total = 0.0;
		for(final char zhchar : zh.toCharArray())
		{
			total = total + normalizedMap.getOrDefault(zhchar, 0.0);
		}
		return total / (double)zh.length();
	}
}

package dt.jdictionary.sqlite.dbservice.alternative;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dt.jdictionary.SimpleLookup;
import dt.jdictionary.Utils;
import dt.jdictionary.sqlite.dbservice.DbServiceUtils;
import dt.jdictionary.sqlite.raw.DbRepo;

public class TypoSearch implements AlternateSearch
{
	@Override
	public List<SimpleLookup> trySearch(String compoundWord, DbRepo db)
	{
		final List<String> trueChars = Utils.trueChars(compoundWord);
		final List<List<String>> normalizedPinyins = findPinyinForZh(trueChars, db);
		if(compoundWord.length() != normalizedPinyins.size())
		{
			return List.of();
		}

		final List<String> permutations = pinyinPermutations(normalizedPinyins);
		final List<SimpleLookup> candidates = DbServiceUtils.convertRawToSimple(db.findByNormalizedPinyin(permutations));

		final Map<Integer, List<SimpleLookup>> candidatesRanked = new HashMap<>();
		for(final SimpleLookup candidate : candidates)
		{
			final int similarity = pinyinLookupSimilarity(candidate, trueChars);
			if(similarity == 0 || similarity == compoundWord.length())
			{
				continue;
			}
			if(!candidatesRanked.keySet().contains(similarity))
			{
				candidatesRanked.put(similarity, new ArrayList<>());
			}
			candidatesRanked.get(similarity).add(candidate);
		}

		final List<Integer> rankings = new ArrayList<>(candidatesRanked.keySet());
		rankings.sort(Comparator.reverseOrder());

		final List<SimpleLookup> result = new ArrayList<>();
		rankings.stream().forEach(rank -> result.addAll(candidatesRanked.get(rank)));
		return result;
	}

	private int pinyinLookupSimilarity(SimpleLookup candidate, List<String> targetChars)
	{
		int similarity = 0;
		final List<String> candidateTrueChars = Utils.trueChars(candidate.getZh());
		final Set<String> candidateSet = new HashSet<>();
		candidateTrueChars.stream().forEach(candidateChar -> candidateSet.add(candidateChar));
		for(final String targetChar : targetChars)
		{
			if(candidateSet.contains(targetChar))
			{
				similarity++;
			}
		}
		return similarity;
	}

	private List<List<String>> findPinyinForZh(List<String> chars, DbRepo db)
	{
		final HashMap<String, Set<String>> pinyinMap = new HashMap<>();
		final List<SimpleLookup> dictionaryEntries = DbServiceUtils.convertRawToSimple(db.lookupChinese(chars));
		for(final SimpleLookup entry : dictionaryEntries)
		{
			if(!pinyinMap.containsKey(entry.getZh()))
			{
				pinyinMap.put(entry.getZh(), new HashSet<>());
			}
			pinyinMap.get(entry.getZh()).add(Utils.normalizePinyin(entry.getPinyin()));
		}
		
		final List<List<String>> result = new ArrayList<>();
		for(final String singleChar : chars)
		{
			result.add(new ArrayList<String>(pinyinMap.get(singleChar)));
		}
		return result;
	}

	private List<String> pinyinPermutations(List<List<String>> individualPinyins)
	{
		if(individualPinyins.size() == 0)
		{
			return List.of();
		}
		else if(individualPinyins.size() == 1)
		{
			return individualPinyins.get(0);
		}
		else if(individualPinyins.size() == 2)
		{
			final List<String> result = new ArrayList<>();
			for(final String first : individualPinyins.get(0))
			{
				for(final String second : individualPinyins.get(1))
				{
					result.add(first + " " + second);
				}
			}
			return result;
		}
		else
		{
			final List<String> subresult = pinyinPermutations(individualPinyins.subList(1, individualPinyins.size()));
			return pinyinPermutations(List.of(individualPinyins.get(0), subresult));
		}
	}
}

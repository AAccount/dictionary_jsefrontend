package dt.jdictionary.sqlite;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dt.jdictionary.FullLookup;
import dt.jdictionary.SimpleLookup;
import dt.jdictionary.Utils;
import dt.jdictionary.cedict.CedictDump;
import dt.jdictionary.cedict.MeasureWords;
import dt.jdictionary.cedict.ZhPinyin;
import dt.jdictionary.sqlite.DbRepo.RelatedChar;

public class DbService 
{
	public static final int MIN_4CHARS_SUBSTRING = 2;
	private DbRepo db;

	public DbService()
	{
		db = new DbRepo();
	}

	public FullLookup lookupChinese(String zh)
	{
		final List<RawDictionaryRow> rawResults = db.lookupChinese(zh);
		final Map<String, List<String>> resultsByPinyin = new HashMap<>();
		for(final RawDictionaryRow rawResult : rawResults)
		{
			final String pinyin = rawResult.getPinyin();
			if(!resultsByPinyin.keySet().contains(pinyin))
			{
				resultsByPinyin.put(pinyin, new ArrayList<>());
			}
			resultsByPinyin.get(pinyin).add(rawResult.getSingleDefinition());
		}

		final String simplified = db.lookupSimplified(zh);
		final List<String> measureWords = db.lookupMeasureWords(zh);
		return new FullLookup(zh, resultsByPinyin, simplified, measureWords);
	}

	public List<SimpleLookup> lookupSameFront(String zh)
	{
		final String firstChar = Character.toString(zh.charAt(0));
		final List<RawDictionaryRow> rawResults = db.lookupRelatedWord(firstChar, RelatedChar.SAME_FRONT);
		return convertRawToSimple(rawResults);
	}

	public List<SimpleLookup> lookupSameBack(String zh)
	{
		final String lastChar = Character.toString(zh.charAt(zh.length()-1));
		final List<RawDictionaryRow> rawResults = db.lookupRelatedWord(lastChar, RelatedChar.SAME_BACK);
		return convertRawToSimple(rawResults);
	}

	public List<SimpleLookup> lookupEnglish(String en)
	{
		final List<RawDictionaryRow> rawResults = db.lookupEnglish(en);
		return convertRawToSimple(rawResults);
	}

	private List<SimpleLookup> convertRawToSimple(List<RawDictionaryRow> rawResults)
	{
		final List<SimpleLookup> result = new ArrayList<>();
		final Map<String, SimpleLookup> mapper = new HashMap<>();

		for(final RawDictionaryRow rawResult : rawResults)
		{
			final String key = rawResult.getZh() + ":" + rawResult.getPinyin();
			if(!mapper.keySet().contains(key))
			{
				final SimpleLookup simpleLookup = new SimpleLookup(rawResult.getZh(), rawResult.getPinyin(), new ArrayList<>());
				result.add(simpleLookup);
				mapper.put(key, simpleLookup);
			}
			mapper.get(key).getDefinitions().add(rawResult.getSingleDefinition());
		}

		return result;
	}

	public List<SimpleLookup> try4CharLookup(String compoundWord)
	{
		final List<String> possibleMatches = db.tryFourChars(compoundWord);
		if(possibleMatches.size() == 0)
		{
			return List.of();
		}

		final List<RawDictionaryRow> raws = new ArrayList<>();
		for(final String possibleMatch : possibleMatches)
		{
			raws.addAll(db.lookupChinese(possibleMatch));
		}

		return convertRawToSimple(raws);
	}

	public List<SimpleLookup> tryTypoMatch(String compoundWord)
	{
		List<String> trueChars = Utils.trueChars(compoundWord);
		List<List<String>> normalizedPinyins = trueChars.stream().map(trueChar -> findPinyinForZh(trueChar)).toList();
		if(compoundWord.length() != normalizedPinyins.size())
		{
			return List.of();
		}

		final List<String> permutations = pinyinPermutations(normalizedPinyins);
		final List<SimpleLookup> candidates = new ArrayList<>();
		permutations.stream().forEach(permutation -> candidates.addAll(convertRawToSimple(db.findByNormalizedPinyin(permutation))));

		final Map<Integer, List<SimpleLookup>> candidatesRanked = new HashMap<>();
		for(final SimpleLookup candidate : candidates)
		{
			final int similarity = pinyinLookupSimilarity(candidate, trueChars);
			if(similarity == 0)
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

	private List<String> findPinyinForZh(String singleChar)
	{
		final List<SimpleLookup> directResults = convertRawToSimple(db.lookupChinese(singleChar));
		if(directResults.size() > 0)
		{
			final Set<String> pinyinsUnique = new HashSet<>();
			directResults.stream().forEach(result -> pinyinsUnique.add(Utils.normalizePinyin(result.getPinyin())));
			return new ArrayList<>(pinyinsUnique);
		}

		return db.findSimplifiedNormalizedPinyins(singleChar);
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

	public void saveCedictDump(CedictDump dump)
	{
		if(dump.getDictionary().size() == 0)
		{
			System.out.println("Empty dump. Don't wipe!");
			return;
		}

		final int dictionarySize = dump.getDictionary().size();
		final int uptoDictTrxes = DbRepo.INIT_TRX_COUNT + dictionarySize + DbRepo.DICT_EN_TRX;
		final int totalTrxes = uptoDictTrxes + DbRepo.POST_DICT_TRX;
		DbEvent.sendProgressEvent(0, totalTrxes);
		db.wipe();
		DbEvent.sendProgressEvent(1, totalTrxes);
		db.init();
		DbEvent.sendProgressEvent(2, totalTrxes);

		db.fillDictionary(dump.getDictionary());
		fillMeasureWords(dump);
		DbEvent.sendProgressEvent(uptoDictTrxes + 1, totalTrxes);
		fillSimplified(dump);
		DbEvent.sendProgressEvent(uptoDictTrxes + 2, totalTrxes);
		fill4Chars(dump);
		DbEvent.sendProgressEvent(uptoDictTrxes + 3, totalTrxes);

		// Funny things happen when you try to import more than once on the same connection.
		db.close();
		db = new DbRepo();
	}

	private void fill4Chars(CedictDump dump)
	{
		final List<SimpleLookup> fourCharEntries = dump.getDictionary().stream()
			.filter(simplelookup -> List.of(3,4,5).indexOf(simplelookup.getZh().length()) != -1 && Utils.allChinese(simplelookup.getZh())).toList();

		final Set<Raw4CharRow> result = new HashSet<>();
		for(final SimpleLookup simpleLookup : fourCharEntries)
		{
			final List<String> substrings = generate4CharSubstrings(simpleLookup.getZh());
			for(final String substring : substrings)
			{
				result.add(new Raw4CharRow(substring, simpleLookup.getZh()));
			}
		}
		db.fill4Chars(new ArrayList<>(result));
	}

	private List<String> generate4CharSubstrings(String saying)
	{
		if(saying.length() <= MIN_4CHARS_SUBSTRING)
		{
			return List.of();
		}

		final List<String> result = new ArrayList<>();
		for(int i = MIN_4CHARS_SUBSTRING; i < saying.length(); i++)
		{
			result.add(saying.substring(0, i));
		}
		result.addAll(generate4CharSubstrings(saying.substring(1)));
		return result;
	}

	private void fillMeasureWords(CedictDump dump)
	{
		final Set<RawMeasureWordRow> mwTracker = new HashSet<>();
		for(final MeasureWords measureListing : dump.getMeasureWords())
		{
			for(final ZhPinyin measure : measureListing.getMeasures())
			{
				mwTracker.add(new RawMeasureWordRow(measureListing.getZh(), measure.getZh(), measure.getPinyin()));
			}
		}
		db.fillMeasureWords(new ArrayList<>(mwTracker));
	}

	private void fillSimplified(CedictDump dump)
	{
		final List<RawSimplifiedRow> simplifieds = new ArrayList<>();
		for(final String original : dump.getSimplifiedChars().keySet())
		{
			simplifieds.add(new RawSimplifiedRow(original, dump.getSimplifiedChars().get(original)));
		}
		db.fillSimplified(simplifieds);
	}
}

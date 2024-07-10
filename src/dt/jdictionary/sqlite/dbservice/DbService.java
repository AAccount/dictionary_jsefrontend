package dt.jdictionary.sqlite.dbservice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import dt.jdictionary.ExhaustiveChineseLookup;
import dt.jdictionary.SimpleLookup;
import dt.jdictionary.cedict.CedictDump;
import dt.jdictionary.sqlite.dbservice.alternative.AlternateSearch;
import dt.jdictionary.sqlite.dbservice.alternative.DeinterlaceSearch;
import dt.jdictionary.sqlite.dbservice.alternative.SubstringOfSearch;
import dt.jdictionary.sqlite.dbservice.alternative.SameBackSearch;
import dt.jdictionary.sqlite.dbservice.alternative.SameFrontSearch;
import dt.jdictionary.sqlite.dbservice.alternative.SubstringSearch;
import dt.jdictionary.sqlite.dbservice.alternative.TypoSearch;
import dt.jdictionary.sqlite.raw.DbRepo;
import dt.jdictionary.sqlite.raw.RawDictionaryRow;
import dt.jdictionary.ui.UiConstants;
import dt.jdictionary.util.ChineseText;
import dt.jdictionary.util.Debug;
import dt.jdictionary.util.GenerateCombinations;

public class DbService 
{
	private final DbRepo db = new DbRepo();

	public ExhaustiveChineseLookup lookupChinese(String chinese, boolean newSearch)
	{
		Debug.logTimestamp("definition start");
		final CompletableFuture<ChineseDefinitionLookup> directResults = CompletableFuture.supplyAsync(() -> {return this.lookupChineseDefinition(chinese);});
		
		final List<AlternateSearch> alts = List.of(
			new SameFrontSearch(chinese, db), 
			new SameBackSearch(chinese, db), 
			new SubstringSearch(chinese, db), 
			new SubstringOfSearch(chinese, db), 
			new DeinterlaceSearch(chinese, db), 
			new TypoSearch(chinese, db)
		);
		
		Debug.logTimestamp("start exhaustive Chinese search");
		final Map<String, CompletableFuture<List<SimpleLookup>>> supplementaryFutures = new LinkedHashMap<>(); 
		alts.forEach(alt -> supplementaryFutures.put(alt.LOOKUP_NAME(), CompletableFuture.supplyAsync(() -> {return alt.trySearch();})));
		
		final Map<String, List<SimpleLookup>> supplementaries = new LinkedHashMap<>(); // linked hash map for predictable iteration order
		supplementaryFutures.keySet().forEach(altName -> supplementaries.put(altName, rerankAlternates(altName, supplementaryFutures.get(altName).join())));
		Debug.logTimestamp("finish exhaustive Chinese search");
		
		final ExhaustiveChineseLookup result =  new ExhaustiveChineseLookup(directResults.join(), supplementaries);
		if(newSearch)
		{
			saveChineseSeachHits(result);
		}
		return result;
	}
	
	private List<SimpleLookup> rerankAlternates(String alternate, List<SimpleLookup> results)
	{
		if(alternate.equals(SubstringSearch.LOOKUP_NAME))
		{
			return results;
		}
		
		final List<String> candidates = results.stream().map(SimpleLookup::getZh).toList();
		final Map<String, Integer> pastHits = PastHitUtils.countHits(db.lookupPastHits(candidates));
		return DbServiceUtils.rerank(results, pastHits);
	}
	
	private ChineseDefinitionLookup lookupChineseDefinition(String zh)
	{
		final List<RawDictionaryRow> rawResults = db.lookupChinese(List.of(zh));
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
		final ChineseDefinitionLookup result = new ChineseDefinitionLookup(zh, resultsByPinyin, simplified, measureWords);
		return result;
	}
	
	private void saveChineseSeachHits(ExhaustiveChineseLookup exhaustiveLookup)
	{
		if(!UiConstants.flagMap.get(UiConstants.FLAG_SAVE_HITS))
		{
			return;
		}
		
		final List<String> hits = new ArrayList<>();
		if(!exhaustiveLookup.getDefinition().getResults().isEmpty())
		{
			hits.add(exhaustiveLookup.getDefinition().getZh());
		}
		
		if(exhaustiveLookup.getSupplementaries().containsKey(SubstringSearch.LOOKUP_NAME))
		{
			final List<String> substringHits = exhaustiveLookup.getSupplementaries().get(SubstringSearch.LOOKUP_NAME).stream()
				.filter(substringEntry -> ChineseText.trueChars(substringEntry.getZh()).size() > 1)
				.map(SimpleLookup::getZh).toList();
			hits.addAll(substringHits);
		}
		db.saveHits(hits);
	}
	
	public Map<String, List<SimpleLookup>> lookupEnglish(String en)
	{
		Debug.logTimestamp("english start");

		final Map<String, CompletableFuture<List<SimpleLookup>>> wordFutures= new HashMap<>();
		final String[] individualWords = en.split(" ");
		for(final String individualWord : individualWords)
		{
			wordFutures.put(individualWord, CompletableFuture.supplyAsync(() -> {return this.lookupSingleEnglishWord(individualWord);}));
		}
		
		final Map<String, List<SimpleLookup>> result= new HashMap<>();
		for(final String word : wordFutures.keySet())
		{
			final List<SimpleLookup> singleResult = wordFutures.get(word).join();
			result.put(word, singleResult);
		}
		Debug.logTimestamp("english end");

		return findUseableCombinations(result);
	}
	
	private Map<String, List<SimpleLookup>> findUseableCombinations(Map<String, List<SimpleLookup>> individualDefinitions)
	{
		final List<List<String>> combinations = GenerateCombinations.generateCombinations(List.copyOf(individualDefinitions.keySet()));
		final Map<String, List<SimpleLookup>> result = new HashMap<String, List<SimpleLookup>>();
		for(final List<String> combination : combinations)
		{
			final List<SimpleLookup> combinedLookup = getQualifyingEntries(individualDefinitions, combination);
			if(!combinedLookup.isEmpty())
			{
				result.put(combination.toString(), combinedLookup);
			}
		}
		return result;
	}
	
	private List<SimpleLookup> getQualifyingEntries(Map<String, List<SimpleLookup>> individualDefinitions, List<String> combination)
	{
		if(combination.size() == 1)
		{
			return individualDefinitions.get(combination.get(0));
		}
		
		final List<SimpleLookup> result = new ArrayList<>(individualDefinitions.get(combination.get(0)));
		for(final String word : combination.subList(1, combination.size()))
		{
			final List<SimpleLookup> wordEntries = individualDefinitions.get(word);
			result.retainAll(wordEntries);
		}
		return result;
	}
	
	private List<SimpleLookup> lookupSingleEnglishWord(String singleWord)
	{
		final List<SimpleLookup> rawResults =  DbServiceUtils.convertRawToSimple(db.lookupEnglish(singleWord));
		final List<String> candidates = rawResults.stream().map(SimpleLookup::getZh).toList();
		final Map<String, Integer> pastHits = PastHitUtils.countHits(db.lookupPastHits(candidates));
		return DbServiceUtils.rerank(rawResults, pastHits);
	}

	public void saveCedictDump(CedictDump dump)
	{
		new SaveCedict().save(dump, db);
	}
	
	public void savePastHits(List<String> words, boolean verifyInDictionary)
	{
		final List<String> useable = verifyInDictionary ? checkChineseInDictionary(words) : words;
		db.saveHits(useable);
	}
	
	private List<String> checkChineseInDictionary(List<String> words)
	{
		final List<RawDictionaryRow> rawDictionaryRows = db.lookupChinese(words);
		final Set<String> inDictionary = rawDictionaryRows.stream().map(RawDictionaryRow::getZh).collect(Collectors.toCollection(HashSet::new));
		return words.stream().filter(word -> inDictionary.contains(word)).toList();
	}
}

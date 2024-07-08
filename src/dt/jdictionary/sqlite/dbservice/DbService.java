package dt.jdictionary.sqlite.dbservice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import dt.jdictionary.ExhaustiveChineseLookup;
import dt.jdictionary.SimpleLookup;
import dt.jdictionary.cedict.CedictDump;
import dt.jdictionary.events.EventUtils;
import dt.jdictionary.sqlite.dbservice.alternative.AlternateSearch;
import dt.jdictionary.sqlite.dbservice.alternative.DeinterlaceSearch;
import dt.jdictionary.sqlite.dbservice.alternative.SubstringOfSearch;
import dt.jdictionary.sqlite.dbservice.alternative.SameBackSearch;
import dt.jdictionary.sqlite.dbservice.alternative.SameFrontSearch;
import dt.jdictionary.sqlite.dbservice.alternative.SubstringSearch;
import dt.jdictionary.sqlite.dbservice.alternative.TypoSearch;
import dt.jdictionary.sqlite.raw.DbRepo;
import dt.jdictionary.sqlite.raw.RawDictionaryRow;
import dt.jdictionary.util.Debug;

public class DbService 
{
	private DbRepo db = new DbRepo(true);

	public ExhaustiveChineseLookup lookupChinese(String chinese)
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
		supplementaryFutures.keySet().forEach(altName -> supplementaries.put(altName, supplementaryFutures.get(altName).join()));
		Debug.logTimestamp("finish exhaustive Chinese search");
		
		return new ExhaustiveChineseLookup(directResults.join(), supplementaries);
	}
	
	private ChineseDefinitionLookup lookupChineseDefinition(String zh)
	{
		checkDbRo();
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
	
	public Map<String, List<SimpleLookup>> lookupEnglish(String en)
	{
		checkDbRo();
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

		return result;
	}
	
	private List<SimpleLookup> lookupSingleEnglishWord(String singleWord)
	{
		checkDbRo();
		return DbServiceUtils.convertRawToSimple(db.lookupEnglish(singleWord));
	}

	public void saveCedictDump(CedictDump dump)
	{
		db = new DbRepo(false);

		new SaveCedict().save(dump, db);

		db.close();
		db = new DbRepo(true);
	}

	private void checkDbRo()
	{
		if(!db.isReadonly())
		{
			EventUtils.sendError(new Exception("DB is in rw mode."));
		}
	}
}

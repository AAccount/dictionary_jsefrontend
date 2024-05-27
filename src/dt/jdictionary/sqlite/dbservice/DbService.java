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
	private DbRepo db = new DbRepo(this, true);

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
	
	public List<SimpleLookup> lookupEnglish(String en)
	{
		checkDbRo();
		final List<RawDictionaryRow> rawResults = db.lookupEnglish(en);
		return DbServiceUtils.convertRawToSimple(rawResults);
	}

	public void saveCedictDump(CedictDump dump)
	{
		db = new DbRepo(this, false);

		new SaveCedict().save(dump, db);

		db.close();
		db = new DbRepo(this, true);
	}

	private void checkDbRo()
	{
		if(!db.isReadonly())
		{
			EventUtils.sendError(new Exception("DB is in rw mode."));
		}
	}
}

package dt.jdictionary.sqlite.dbservice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dt.jdictionary.ExhaustiveChineseLookup;
import dt.jdictionary.SimpleLookup;
import dt.jdictionary.Utils;
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

public class DbService 
{
	private DbRepo db = new DbRepo(this, true);

	public ExhaustiveChineseLookup lookupChinese(String chinese)
	{
		Utils.logTimestamp("definition start");
		final ChineseDefinitionLookup directResults = this.lookupChineseDefinition(chinese);
		Utils.logTimestamp("definition end");
		final Map<String, List<SimpleLookup>> supplementaries = new LinkedHashMap<>(); // linked hash map for predictable iteration order
		Utils.logTimestamp("same front");
		supplementaries.put("Same Front", this.lookupSameFront(chinese));
		Utils.logTimestamp("same back");
		supplementaries.put("Same Back", this.lookupSameBack(chinese));
		Utils.logTimestamp("substring");
		supplementaries.put("Substring", this.trySubstringMatch(chinese));
		Utils.logTimestamp("substring of");
		supplementaries.put("Substring Of", this.trySubstringOfLookup(chinese));
		Utils.logTimestamp("deinterlace");
		supplementaries.put("Deinterlace", this.tryDeinterlace(chinese));
		Utils.logTimestamp("typo");
		supplementaries.put("Typo", this.tryTypoMatch(chinese));
		Utils.logTimestamp("finished lookups");
		return new ExhaustiveChineseLookup(directResults, supplementaries);
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

	private List<SimpleLookup> lookupSameFront(String zh)
	{
		return tryAlternateSearch(new SameFrontSearch(), zh);
	}

	private List<SimpleLookup> lookupSameBack(String zh)
	{
		return tryAlternateSearch(new SameBackSearch(), zh);
	}

	private List<SimpleLookup> tryDeinterlace(String zh)
	{
		return tryAlternateSearch(new DeinterlaceSearch(), zh);
	}

	private List<SimpleLookup> trySubstringOfLookup(String zh)
	{
		return tryAlternateSearch(new SubstringOfSearch(), zh);
	}

	private List<SimpleLookup> tryTypoMatch(String zh)
	{
		return tryAlternateSearch(new TypoSearch(), zh);
	}

	private List<SimpleLookup> trySubstringMatch(String zh)
	{
		return tryAlternateSearch(new SubstringSearch(), zh);
	}

	private List<SimpleLookup> tryAlternateSearch(AlternateSearch alternateSearch, String zh)
	{
		checkDbRo();
		return alternateSearch.trySearch(zh, db);
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

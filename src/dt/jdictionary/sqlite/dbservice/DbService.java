package dt.jdictionary.sqlite.dbservice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dt.jdictionary.FullLookup;
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

public class DbService 
{
	private DbRepo db = new DbRepo(this, true);;

	public FullLookup lookupChinese(String zh)
	{
		checkDbRo();
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
		final FullLookup result = new FullLookup(zh, resultsByPinyin, simplified, measureWords);
		return result;
	}

	public List<SimpleLookup> lookupSameFront(String zh)
	{
		return tryAlternateSearch(new SameFrontSearch(), zh);
	}

	public List<SimpleLookup> lookupSameBack(String zh)
	{
		return tryAlternateSearch(new SameBackSearch(), zh);
	}

	public List<SimpleLookup> lookupEnglish(String en)
	{
		checkDbRo();
		final List<RawDictionaryRow> rawResults = db.lookupEnglish(en);
		return DbServiceUtils.convertRawToSimple(rawResults);
	}

	public List<SimpleLookup> tryDeinterlace(String zh)
	{
		return tryAlternateSearch(new DeinterlaceSearch(), zh);
	}

	public List<SimpleLookup> trySubstringOfLookup(String zh)
	{
		return tryAlternateSearch(new SubstringOfSearch(), zh);
	}

	public List<SimpleLookup> tryTypoMatch(String zh)
	{
		return tryAlternateSearch(new TypoSearch(), zh);
	}

	public List<SimpleLookup> trySubstringMatch(String zh)
	{
		return tryAlternateSearch(new SubstringSearch(), zh);
	}

	private List<SimpleLookup> tryAlternateSearch(AlternateSearch alternateSearch, String zh)
	{
		checkDbRo();
		return alternateSearch.trySearch(zh, db);
	}

	public void saveCedictDump(CedictDump dump)
	{
		db.close();
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

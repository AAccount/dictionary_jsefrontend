package dt.jdictionary.sqlite.dbservice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dt.jdictionary.FullLookup;
import dt.jdictionary.SimpleLookup;
import dt.jdictionary.cedict.CedictDump;
import dt.jdictionary.events.EventUtils;
import dt.jdictionary.sqlite.dbservice.alternative.DeinterlaceSearch;
import dt.jdictionary.sqlite.dbservice.alternative.FourCharSearch;
import dt.jdictionary.sqlite.dbservice.alternative.SubstringSearch;
import dt.jdictionary.sqlite.dbservice.alternative.TypoSearch;
import dt.jdictionary.sqlite.raw.DbRepo;
import dt.jdictionary.sqlite.raw.RawDictionaryRow;
import dt.jdictionary.sqlite.raw.DbRepo.RelatedChar;

public class DbService 
{
	private DbRepo db;
	private boolean isReadonly = true;

	public DbService()
	{
		db = new DbRepo(this, isReadonly);
	}

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
		return new FullLookup(zh, resultsByPinyin, simplified, measureWords);
	}

	public List<SimpleLookup> lookupSameFront(String zh)
	{
		checkDbRo();
		final String firstChar = Character.toString(zh.charAt(0));
		final List<RawDictionaryRow> rawResults = db.lookupRelatedWord(firstChar, RelatedChar.SAME_FRONT);
		return DbServiceUtils.convertRawToSimple(rawResults);
	}

	public List<SimpleLookup> lookupSameBack(String zh)
	{
		checkDbRo();
		final String lastChar = Character.toString(zh.charAt(zh.length()-1));
		final List<RawDictionaryRow> rawResults = db.lookupRelatedWord(lastChar, RelatedChar.SAME_BACK);
		return DbServiceUtils.convertRawToSimple(rawResults);
	}

	public List<SimpleLookup> lookupEnglish(String en)
	{
		checkDbRo();
		final List<RawDictionaryRow> rawResults = db.lookupEnglish(en);
		return DbServiceUtils.convertRawToSimple(rawResults);
	}

	public List<SimpleLookup> tryDeinterlace(String zh)
	{
		return new DeinterlaceSearch().deinterlace(zh, db);
	}

	public List<SimpleLookup> try4CharLookup(String zh)
	{
		return new FourCharSearch().tryLookup(zh, db);
	}

	public List<SimpleLookup> tryTypoMatch(String zh)
	{
		return new TypoSearch().tryTypo(zh, db);
	}

	public List<SimpleLookup> trySubstringMatch(String zh)
	{
		return new SubstringSearch().trySubstring(zh, db);
	}

	public void saveCedictDump(CedictDump dump)
	{
		db.close();
		isReadonly = false;
		db = new DbRepo(this, isReadonly);

		new SaveCedict().save(dump, db);

		db.close();
		isReadonly = true;
		db = new DbRepo(this, isReadonly);
	}

	private void checkDbRo()
	{
		if(!isReadonly)
		{
			EventUtils.sendError(new Exception("DB is in rw mode."));
		}
	}
}

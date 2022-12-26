package dt.jdictionary.sqlite.dbservice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dt.jdictionary.FullLookup;
import dt.jdictionary.SimpleLookup;
import dt.jdictionary.cedict.CedictDump;
import dt.jdictionary.sqlite.dbservice.alternative.DeinterlaceSearch;
import dt.jdictionary.sqlite.dbservice.alternative.FourCharSearch;
import dt.jdictionary.sqlite.dbservice.alternative.TypoSearch;
import dt.jdictionary.sqlite.raw.DbRepo;
import dt.jdictionary.sqlite.raw.RawDictionaryRow;
import dt.jdictionary.sqlite.raw.DbRepo.RelatedChar;

public class DbService 
{
	private final String DB_USER = "DbService";

	public FullLookup lookupChinese(String zh)
	{
		final DbRepo db = new DbRepo(DB_USER + " " + this.hashCode());
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
		db.close();
		return new FullLookup(zh, resultsByPinyin, simplified, measureWords);
	}

	public List<SimpleLookup> lookupSameFront(String zh)
	{
		final DbRepo db = new DbRepo(DB_USER + " " + this.hashCode());
		final String firstChar = Character.toString(zh.charAt(0));
		final List<RawDictionaryRow> rawResults = db.lookupRelatedWord(firstChar, RelatedChar.SAME_FRONT);
		db.close();
		return DbServiceUtils.convertRawToSimple(rawResults);
	}

	public List<SimpleLookup> lookupSameBack(String zh)
	{
		final DbRepo db = new DbRepo(DB_USER + " " + this.hashCode());
		final String lastChar = Character.toString(zh.charAt(zh.length()-1));
		final List<RawDictionaryRow> rawResults = db.lookupRelatedWord(lastChar, RelatedChar.SAME_BACK);
		db.close();
		return DbServiceUtils.convertRawToSimple(rawResults);
	}

	public List<SimpleLookup> lookupEnglish(String en)
	{
		final DbRepo db = new DbRepo(DB_USER + " " + this.hashCode());
		final List<RawDictionaryRow> rawResults = db.lookupEnglish(en);
		db.close();
		return DbServiceUtils.convertRawToSimple(rawResults);
	}

	public List<SimpleLookup> tryDeinterlace(String zh)
	{
		return new DeinterlaceSearch().deinterlace(zh);
	}

	public List<SimpleLookup> try4CharLookup(String zh)
	{
		return new FourCharSearch().tryLookup(zh);
	}

	public List<SimpleLookup> tryTypoMatch(String zh)
	{
		return new TypoSearch().tryTypo(zh);
	}

	public void saveCedictDump(CedictDump dump)
	{
		new SaveCedict().save(dump);
	}
}

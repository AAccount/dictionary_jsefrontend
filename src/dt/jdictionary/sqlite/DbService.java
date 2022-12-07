package dt.jdictionary.sqlite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dt.jdictionary.FullLookup;
import dt.jdictionary.SimpleLookup;
import dt.jdictionary.cedict.CedictDump;
import dt.jdictionary.cedict.MeasureWords;
import dt.jdictionary.cedict.ZhPinyin;
import dt.jdictionary.sqlite.DbRepo.RelatedChar;

public class DbService 
{
	private final DbRepo db;

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

	public void saveCedictDump(CedictDump dump)
	{
		if(dump.getDefinitions().size() == 0)
		{
			System.out.println("Empty dump. Don't wipe!");
			return;
		}

		db.wipe();
		db.init();

		// There are legit duplicate values in the cedict file.
		final Set<RawDictionaryRow> defTracker = new HashSet<>();
		for(final SimpleLookup simpleLookup : dump.getDefinitions())
		{
			for(final String definition : simpleLookup.getDefinitions())
			{
				defTracker.add(new RawDictionaryRow(simpleLookup.getZh(), simpleLookup.getPinyin(), definition));
			}
		}
		final List<RawDictionaryRow> dedupDefs = new ArrayList<>();
		dedupDefs.addAll(defTracker);
		db.fillDictionary(dedupDefs);

		final Set<RawMeasureWordRow> mwTracker = new HashSet<>();
		for(final MeasureWords measureListing : dump.getMeasureWords())
		{
			for(final ZhPinyin measure : measureListing.getMeasures())
			{
				mwTracker.add(new RawMeasureWordRow(measureListing.getZh(), measure.getZh(), measure.getPinyin()));
			}
		}
		final List<RawMeasureWordRow> dedupMeasures = new ArrayList<>();
		dedupMeasures.addAll(mwTracker);
		db.fillMeasureWords(dedupMeasures);

		final List<RawSimplifiedRow> simplifieds = new ArrayList<>();
		for(final String original : dump.getSimplifiedChars().keySet())
		{
			simplifieds.add(new RawSimplifiedRow(original, dump.getSimplifiedChars().get(original)));
		}
		db.fillSimplified(simplifieds);
	}
}

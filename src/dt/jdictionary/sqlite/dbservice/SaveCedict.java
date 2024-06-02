package dt.jdictionary.sqlite.dbservice;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dt.jdictionary.SimpleLookup;
import dt.jdictionary.UnrankedLookup;
import dt.jdictionary.cedict.CedictDump;
import dt.jdictionary.cedict.MeasureWords;
import dt.jdictionary.cedict.ZhPinyin;
import dt.jdictionary.events.EventUtils;
import dt.jdictionary.sqlite.DbEvent;
import dt.jdictionary.sqlite.raw.DbRepo;
import dt.jdictionary.sqlite.raw.RawSubstringRow;
import dt.jdictionary.util.GenerateSubstrings;
import dt.jdictionary.util.ChineseText;
import dt.jdictionary.sqlite.raw.RawMeasureWordRow;
import dt.jdictionary.sqlite.raw.RawSimplifiedRow;

public class SaveCedict 
{
	public void save(CedictDump dump, DbRepo db)
	{
		if(dump.getDictionary().size() == 0)
		{
			EventUtils.sendWarning("Empty dump. Don't wipe!");
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

		final Map<Character, Double> freqCountMap = RankingUtilities.rankSingleChars(dump.getDictionary());
		final List<SimpleLookup> dictionaryRankedList = dump.getDictionary().stream()
			.map(unranked -> new SimpleLookup(unranked, RankingUtilities.rank(unranked, freqCountMap))).toList();
		db.fillDictionary(dictionaryRankedList);
		fillMeasureWords(dump, db);
		DbEvent.sendProgressEvent(uptoDictTrxes + 1, totalTrxes);
		fillSimplified(dump, db);
		DbEvent.sendProgressEvent(uptoDictTrxes + 2, totalTrxes);
		fillSubstrings(dump, db);
		DbEvent.sendProgressEvent(uptoDictTrxes + 3, totalTrxes);
	}

	private void fillSubstrings(CedictDump dump, DbRepo db)
	{
		final List<UnrankedLookup> substringEntries = dump.getDictionary().stream()
			.filter(unrankedlookup -> unrankedlookup.getZh().length() > 1 && ChineseText.allChinese(unrankedlookup.getZh())).toList();

		final Set<RawSubstringRow> result = new HashSet<>();
		for(final UnrankedLookup simpleLookup : substringEntries)
		{
			final List<String> substrings = GenerateSubstrings.generateSubstrings(simpleLookup.getZh());
			for(final String substring : substrings)
			{
				result.add(new RawSubstringRow(substring, simpleLookup.getZh()));
			}
		}
		db.fillSubstrings(new ArrayList<>(result));
	}

	private void fillMeasureWords(CedictDump dump, DbRepo db)
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

	private void fillSimplified(CedictDump dump, DbRepo db)
	{
		final List<RawSimplifiedRow> simplifieds = new ArrayList<>();
		for(final String original : dump.getSimplifiedChars().keySet())
		{
			simplifieds.add(new RawSimplifiedRow(original, dump.getSimplifiedChars().get(original)));
		}
		db.fillSimplified(simplifieds);
	}
}

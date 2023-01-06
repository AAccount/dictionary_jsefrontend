package dt.jdictionary.sqlite.dbservice;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dt.jdictionary.SimpleLookup;
import dt.jdictionary.Utils;
import dt.jdictionary.cedict.CedictDump;
import dt.jdictionary.cedict.MeasureWords;
import dt.jdictionary.cedict.ZhPinyin;
import dt.jdictionary.events.EventUtils;
import dt.jdictionary.sqlite.DbEvent;
import dt.jdictionary.sqlite.raw.DbRepo;
import dt.jdictionary.sqlite.raw.RawSubstringRow;
import dt.jdictionary.sqlite.raw.RawMeasureWordRow;
import dt.jdictionary.sqlite.raw.RawVariantRow;

public class SaveCedict 
{
	public void save(CedictDump dump)
	{
		if(dump.getDictionary().size() == 0)
		{
			EventUtils.sendWarning("Empty dump. Don't wipe!");
			return;
		}

		final DbRepo db = new DbRepo(this);
		final int dictionarySize = dump.getDictionary().size();
		final int uptoDictTrxes = DbRepo.INIT_TRX_COUNT + dictionarySize + DbRepo.DICT_EN_TRX;
		final int totalTrxes = uptoDictTrxes + DbRepo.POST_DICT_TRX;
		DbEvent.sendProgressEvent(0, totalTrxes);
		db.wipe();
		DbEvent.sendProgressEvent(1, totalTrxes);
		db.init();
		DbEvent.sendProgressEvent(2, totalTrxes);

		db.fillDictionary(dump.getDictionary());
		fillMeasureWords(dump, db);
		DbEvent.sendProgressEvent(uptoDictTrxes + 1, totalTrxes);
		fillSimplified(dump, db);
		DbEvent.sendProgressEvent(uptoDictTrxes + 2, totalTrxes);
		fill4Chars(dump, db);
		DbEvent.sendProgressEvent(uptoDictTrxes + 3, totalTrxes);
		db.close();
	}

	private void fill4Chars(CedictDump dump, DbRepo db)
	{
		final List<SimpleLookup> fourCharEntries = dump.getDictionary().stream()
			.filter(simplelookup -> List.of(3,4,5).indexOf(simplelookup.getZh().length()) != -1 && Utils.allChinese(simplelookup.getZh())).toList();

		final Set<RawSubstringRow> result = new HashSet<>();
		for(final SimpleLookup simpleLookup : fourCharEntries)
		{
			final List<String> substrings = DbServiceUtils.generateSubstrings(simpleLookup.getZh());
			for(final String substring : substrings)
			{
				result.add(new RawSubstringRow(substring, simpleLookup.getZh()));
			}
		}
		db.fill4Chars(new ArrayList<>(result));
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
		final List<RawVariantRow> simplifieds = new ArrayList<>();
		for(final String original : dump.getSimplifiedChars().keySet())
		{
			simplifieds.add(new RawVariantRow(original, dump.getSimplifiedChars().get(original), DbRepo.VARIANT_SIMPLIFIED));
		}
		db.fillSimplified(simplifieds);
	}
}

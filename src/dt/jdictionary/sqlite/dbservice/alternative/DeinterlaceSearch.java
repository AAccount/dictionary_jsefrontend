package dt.jdictionary.sqlite.dbservice.alternative;

import java.util.ArrayList;
import java.util.List;

import dt.jdictionary.SimpleLookup;
import dt.jdictionary.Utils;
import dt.jdictionary.sqlite.dbservice.DbServiceUtils;
import dt.jdictionary.sqlite.raw.DbRepo;

public class DeinterlaceSearch implements AlternateSearch
{	
	/**
	 * Attempt to "deinterlace" an entry: chars 123 --> lookup 13; chars 1234 --> lookup 13 and 24
	 */
	@Override
	public List<SimpleLookup> trySearch(String zh, DbRepo db)
	{
		final int MIN_DEINTERLACE = 3;
		final int MAX_DEINTERLACE = 4;
		if(zh.length() < MIN_DEINTERLACE || zh.length() > MAX_DEINTERLACE)
		{
			return List.of();
		}

		final List<String> trueChars = Utils.trueChars(zh);
		final List<String> candidates = new ArrayList<String>();
		candidates.add(trueChars.get(0) + trueChars.get(2));
		if(trueChars.size() == MAX_DEINTERLACE)
		{
			candidates.add(trueChars.get(1) + trueChars.get(3));
		}
		return DbServiceUtils.convertRawToSimple(db.lookupChinese(candidates));
	}
}

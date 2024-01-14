package dt.jdictionary.sqlite.dbservice.alternative;

import java.util.ArrayList;
import java.util.List;

import dt.jdictionary.SimpleLookup;
import dt.jdictionary.sqlite.dbservice.DbServiceUtils;
import dt.jdictionary.sqlite.raw.DbRepo;
import dt.jdictionary.sqlite.raw.RawDictionaryRow;

public class FourCharSearch implements AlternateSearch
{
 	public List<SimpleLookup> trySearch(String compoundWord, DbRepo db)
	{
		if(compoundWord.length() < DbServiceUtils.MIN_SUBSTRING_LENGTH)
		{
			return List.of();
		}

		final List<String> possibleMatches = db.trySubstring(compoundWord);
		if(possibleMatches.size() == 0)
		{
			return List.of();
		}

		final List<RawDictionaryRow> raws = new ArrayList<>();
		for(final String possibleMatch : possibleMatches)
		{
			raws.addAll(db.lookupChinese(possibleMatch));
		}
		return DbServiceUtils.convertRawToSimple(raws);
	}

	@Override
	public String getAltSearchType() 
	{
		return this.getClass().getName();
	}
}

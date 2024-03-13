package dt.jdictionary.sqlite.dbservice.alternative;

import java.util.ArrayList;
import java.util.List;

import dt.jdictionary.SimpleLookup;
import dt.jdictionary.sqlite.dbservice.DbServiceUtils;
import dt.jdictionary.sqlite.raw.DbRepo;
import dt.jdictionary.sqlite.raw.RawDictionaryRow;

public class SubstringOfSearch implements AlternateSearch
{
	@Override
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
		return DbServiceUtils.convertRawToSimple(db.lookupChinese(possibleMatches));
	}
}

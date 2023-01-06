package dt.jdictionary.sqlite.dbservice.alternative;

import java.util.ArrayList;
import java.util.List;

import dt.jdictionary.SimpleLookup;
import dt.jdictionary.sqlite.dbservice.DbServiceUtils;
import dt.jdictionary.sqlite.raw.DbRepo;
import dt.jdictionary.sqlite.raw.RawDictionaryRow;

public class FourCharSearch 
{
 	public List<SimpleLookup> tryLookup(String compoundWord)
	{
		if(compoundWord.length() < DbServiceUtils.MIN_SUBSTRING_LENGTH)
		{
			return List.of();
		}

		final DbRepo db = new DbRepo(this);
		final List<String> possibleMatches = db.trySubstring(compoundWord);
		if(possibleMatches.size() == 0)
		{
			db.close();
			return List.of();
		}

		final List<RawDictionaryRow> raws = new ArrayList<>();
		for(final String possibleMatch : possibleMatches)
		{
			raws.addAll(db.lookupChinese(possibleMatch));
		}
		db.close();
		return DbServiceUtils.convertRawToSimple(raws);
	}
}

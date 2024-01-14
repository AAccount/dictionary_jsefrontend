package dt.jdictionary.sqlite.dbservice.alternative;

import java.util.ArrayList;
import java.util.List;

import dt.jdictionary.SimpleLookup;
import dt.jdictionary.sqlite.dbservice.DbServiceUtils;
import dt.jdictionary.sqlite.raw.DbRepo;
import dt.jdictionary.sqlite.raw.RawDictionaryRow;

public class SubstringSearch implements AlternateSearch
{
	@Override
	public List<SimpleLookup> trySearch(String zh, DbRepo db)
	{
		if(zh.length() < DbServiceUtils.MIN_SUBSTRING_LENGTH)
		{
			return List.of();
		}

		final List<String> allSubstrings = DbServiceUtils.generateSubstrings(zh);
		final List<SimpleLookup> result = new ArrayList<>();
		for(final String substring : allSubstrings)
		{
			final List<RawDictionaryRow> substringResults = db.lookupChinese(substring);
			result.addAll(DbServiceUtils.convertRawToSimple(substringResults));
		}
		return result;
	}
}

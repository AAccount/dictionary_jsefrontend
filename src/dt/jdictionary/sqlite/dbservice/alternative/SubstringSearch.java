package dt.jdictionary.sqlite.dbservice.alternative;

import java.util.ArrayList;
import java.util.List;

import dt.jdictionary.SimpleLookup;
import dt.jdictionary.sqlite.dbservice.DbCache;
import dt.jdictionary.sqlite.dbservice.DbServiceUtils;
import dt.jdictionary.sqlite.raw.DbRepo;
import dt.jdictionary.sqlite.raw.RawDictionaryRow;

public class SubstringSearch implements AlternateSearch
{
	private final DbCache cache;
	public SubstringSearch(DbCache cache)
	{
		this.cache = cache;
	}

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
			final List<SimpleLookup> cached = cache.getSimpleLookup(this, substring);
			if(cached != null)
			{
				result.addAll(cached);
			}
			else
			{
				final List<RawDictionaryRow> substringResults = db.lookupChinese(substring);
				final List<SimpleLookup> converted = DbServiceUtils.convertRawToSimple(substringResults);
				cache.setSimpleLookup(this, substring, converted);
				result.addAll(converted);
			}
		}
		return result;
	}
}

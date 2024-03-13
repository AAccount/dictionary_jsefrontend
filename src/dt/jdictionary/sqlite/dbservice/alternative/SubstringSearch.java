package dt.jdictionary.sqlite.dbservice.alternative;

import java.util.List;

import dt.jdictionary.SimpleLookup;
import dt.jdictionary.sqlite.dbservice.DbServiceUtils;
import dt.jdictionary.sqlite.raw.DbRepo;

public class SubstringSearch implements AlternateSearch
{
	@Override
	public List<SimpleLookup> trySearch(String zh, DbRepo db)
	{
		if(zh.length() <= DbServiceUtils.MIN_SUBSTRING_LENGTH)
		{
			return List.of();
		}

		final List<String> allSubstrings = DbServiceUtils.generateSubstrings(zh);
		return DbServiceUtils.convertRawToSimple(db.lookupChinese(allSubstrings));
	}
}

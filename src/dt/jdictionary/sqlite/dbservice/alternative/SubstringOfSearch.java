package dt.jdictionary.sqlite.dbservice.alternative;

import java.util.List;

import dt.jdictionary.SimpleLookup;
import dt.jdictionary.sqlite.dbservice.DbServiceUtils;
import dt.jdictionary.sqlite.raw.DbRepo;

public class SubstringOfSearch implements AlternateSearch
{
	private final String zh;
	private final DbRepo db;
	
	public SubstringOfSearch(String zh, DbRepo db)
	{
		this.zh = zh;
		this.db = db;
	}

	@Override
 	public List<SimpleLookup> trySearch()
	{
		final List<String> possibleMatches = this.db.trySubstring(this.zh);
		if(possibleMatches.size() == 0)
		{
			return List.of();
		}
		return DbServiceUtils.convertRawToSimple(this.db.lookupChinese(possibleMatches));
	}

	@Override
	public String LOOKUP_NAME()
	{
		return "Substring Of";
	}
}

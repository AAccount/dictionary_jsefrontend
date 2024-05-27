package dt.jdictionary.sqlite.dbservice.alternative;

import java.util.List;

import dt.jdictionary.SimpleLookup;
import dt.jdictionary.sqlite.dbservice.DbServiceUtils;
import dt.jdictionary.sqlite.raw.DbRepo;

public class SubstringSearch implements AlternateSearch
{
	private final String zh;
	private final DbRepo db;
	
	public SubstringSearch(String zh, DbRepo db)
	{
		this.zh = zh;
		this.db = db;
	}

	@Override
	public List<SimpleLookup> trySearch()
	{
		final List<String> allSubstrings = DbServiceUtils.generateSubstrings(this.zh);
		return DbServiceUtils.convertRawToSimple(this.db.lookupChinese(allSubstrings));
	}

	@Override
	public String LOOKUP_NAME()
	{
		return "Substring";
	}
}

package dt.jdictionary.sqlite.dbservice.alternative;

import java.util.List;

import dt.jdictionary.SimpleLookup;
import dt.jdictionary.sqlite.dbservice.DbServiceUtils;
import dt.jdictionary.sqlite.raw.DbRepo;
import dt.jdictionary.ui.UiConstants;
import dt.jdictionary.util.ChineseText;
import dt.jdictionary.util.GenerateSubstrings;

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
		final List<String> allSubstrings = GenerateSubstrings.generateSubstrings(this.zh);
		final List<SimpleLookup> allResults = DbServiceUtils.convertRawToSimple(this.db.lookupChinese(allSubstrings));
		if(UiConstants.flagMap.get(UiConstants.FLAG_ALWAYS_SINGLE_SUBSTRING))
		{
			return allResults;
		}
		
		final List<SimpleLookup> nonSingle = allResults.stream().filter(result -> ChineseText.trueChars(result.getZh()).size() > 1).toList();
		return nonSingle.isEmpty() ? allResults : nonSingle;
	}

	@Override
	public String LOOKUP_NAME()
	{
		return "Substring";
	}
}

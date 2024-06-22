package dt.jdictionary.sqlite.dbservice.alternative;

import java.util.List;

import dt.jdictionary.SimpleLookup;
import dt.jdictionary.sqlite.dbservice.DbServiceUtils;
import dt.jdictionary.sqlite.raw.DbRepo;
import dt.jdictionary.ui.UiConstants;
import dt.jdictionary.util.ChineseText;

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
		
		final int minimumLength = UiConstants.flagMap.get(UiConstants.FLAG_SUBSTRING_OF_2CHAR) ? 1 : 3;
		return DbServiceUtils.convertRawToSimple(this.db.lookupChinese(possibleMatches)).stream()
				.filter(result -> ChineseText.trueChars(result.getZh()).size() >= minimumLength)
				.toList();
	}

	@Override
	public String LOOKUP_NAME()
	{
		return "Substring Of";
	}
}

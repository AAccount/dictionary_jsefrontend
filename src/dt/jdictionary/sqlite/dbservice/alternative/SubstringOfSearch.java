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
	private final String first;
	private final String last;
	
	public SubstringOfSearch(String zh, DbRepo db)
	{
		this.zh = zh;
		this.db = db;
		
		final List<String> chars = ChineseText.trueChars(zh);
		this.first = chars.get(0);
		this.last = chars.get(chars.size() - 1);
	}

	@Override
 	public List<SimpleLookup> trySearch()
	{
		final List<String> possibleMatches = this.db.trySubstring(this.zh);
		if(possibleMatches.size() == 0)
		{
			return List.of();
		}
		
		return DbServiceUtils.convertRawToSimple(this.db.lookupChinese(possibleMatches)).stream()
				.filter(this::nonStartEndDuplicate)
				.toList();
	}
	
	private boolean nonStartEndDuplicate(SimpleLookup result)
	{
		if(UiConstants.flagMap.get(UiConstants.FLAG_SUBSTRING_OF_ALL))
		{
			return true;
		}
		
		final List<String> resultChars = ChineseText.trueChars(result.getZh());
		final String resultFirst = resultChars.get(0);
		final String resultLast = resultChars.get(resultChars.size()-1);
		return !resultFirst.equals(this.first) && !resultLast.equals(this.last);
	}

	@Override
	public String LOOKUP_NAME()
	{
		return "Substring Of";
	}
}

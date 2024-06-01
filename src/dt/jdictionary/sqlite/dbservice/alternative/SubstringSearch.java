package dt.jdictionary.sqlite.dbservice.alternative;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	private final Map<String, Double> frontToBackRanking;
	
	public SubstringSearch(String zh, DbRepo db)
	{
		this.zh = zh;
		this.db = db;
		this.frontToBackRanking = this.generateFrontToBackRanking(zh);
	}

	@Override
	public List<SimpleLookup> trySearch()
	{
		final List<String> allSubstrings = GenerateSubstrings.generateSubstrings(this.zh);
		final List<SimpleLookup> allResults = DbServiceUtils
				.convertRawToSimple(this.db.lookupChinese(allSubstrings))
				.stream().map(simpleLookup -> new SimpleLookup(simpleLookup, this.rankBasedOnOriginalFrontToBack(simpleLookup.getZh())))
				.toList();
		
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
	
	private Map<String, Double> generateFrontToBackRanking(String input)
	{
		final Map<String, Double> result = new HashMap<>();
		final List<String> trueChars = ChineseText.trueChars(input);
		for(int i=0; i<trueChars.size(); i++)
		{
			final String singleChar = trueChars.get(i);
			result.put(singleChar, 1.0*(trueChars.size() - i));
		}
		return result;
	}
	
	private double rankBasedOnOriginalFrontToBack(String resultZh)
	{
		return ChineseText.trueChars(resultZh).stream().reduce(0.0, (acc, singleChar) -> acc + this.frontToBackRanking.get(singleChar), Double::sum);
	}
}

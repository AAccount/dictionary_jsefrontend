package dt.jdictionary.sqlite.dbservice.alternative;

import java.util.List;

import dt.jdictionary.SimpleLookup;
import dt.jdictionary.sqlite.raw.DbRepo.RelatedChar;
import dt.jdictionary.sqlite.dbservice.DbServiceUtils;
import dt.jdictionary.sqlite.raw.DbRepo;
import dt.jdictionary.sqlite.raw.RawDictionaryRow;

public class SameFrontSearch implements AlternateSearch
{
	private final String zh;
	private final DbRepo db;
	
	public SameFrontSearch(String zh, DbRepo db)
	{
		this.zh = zh;
		this.db = db;
	}

	@Override
	public List<SimpleLookup> trySearch()
	{
		final String firstChar = Character.toString(this.zh.charAt(0));
		final List<RawDictionaryRow> rawResults = this.db.lookupRelatedWord(firstChar, RelatedChar.SAME_FRONT);
		return DbServiceUtils.convertRawToSimple(rawResults);
	}

	@Override
	public String LOOKUP_NAME()
	{
		return "Same Front";
	}
}

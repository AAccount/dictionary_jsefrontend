package dt.jdictionary.sqlite.dbservice.alternative;

import java.util.List;

import dt.jdictionary.SimpleLookup;
import dt.jdictionary.sqlite.raw.DbRepo.RelatedChar;
import dt.jdictionary.sqlite.dbservice.DbServiceUtils;
import dt.jdictionary.sqlite.raw.DbRepo;
import dt.jdictionary.sqlite.raw.RawDictionaryRow;

public class SameFrontSearch implements AlternateSearch
{
	@Override
	public List<SimpleLookup> trySearch(String zh, DbRepo db)
	{
		final String firstChar = Character.toString(zh.charAt(0));
		final List<RawDictionaryRow> rawResults = db.lookupRelatedWord(firstChar, RelatedChar.SAME_FRONT);
		return DbServiceUtils.convertRawToSimple(rawResults);
	}
}

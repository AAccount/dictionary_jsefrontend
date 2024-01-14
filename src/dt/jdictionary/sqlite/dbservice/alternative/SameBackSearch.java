package dt.jdictionary.sqlite.dbservice.alternative;

import java.util.List;

import dt.jdictionary.SimpleLookup;
import dt.jdictionary.sqlite.raw.DbRepo.RelatedChar;
import dt.jdictionary.sqlite.dbservice.DbServiceUtils;
import dt.jdictionary.sqlite.raw.DbRepo;
import dt.jdictionary.sqlite.raw.RawDictionaryRow;

public class SameBackSearch implements AlternateSearch
{
	@Override
	public List<SimpleLookup> trySearch(String zh, DbRepo db)
	{
		final String lastChar = Character.toString(zh.charAt(zh.length()-1));
		final List<RawDictionaryRow> rawResults = db.lookupRelatedWord(lastChar, RelatedChar.SAME_BACK);
		return DbServiceUtils.convertRawToSimple(rawResults);
	}
}

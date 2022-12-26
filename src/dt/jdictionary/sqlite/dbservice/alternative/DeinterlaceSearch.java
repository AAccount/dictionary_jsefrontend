package dt.jdictionary.sqlite.dbservice.alternative;

import java.util.List;

import dt.jdictionary.SimpleLookup;
import dt.jdictionary.Utils;
import dt.jdictionary.sqlite.dbservice.DbServiceUtils;
import dt.jdictionary.sqlite.raw.DbRepo;

public class DeinterlaceSearch 
{
	private final String DB_USER = "DeinterlaceSearch";
	
	/**
	 * Attempt to "deinterlace" an entry: chars 123 --> lookup 13; chars 1234 --> lookup 13 and 24
	 */
	public List<SimpleLookup> deinterlace(String zh)
	{
		final int MIN_DEINTERLACE = 3;
		final int MAX_DEINTERLACE = 4;
		if(zh.length() < MIN_DEINTERLACE || zh.length() > MAX_DEINTERLACE)
		{
			return List.of();
		}

		final DbRepo db = new DbRepo(DB_USER + " " + this.hashCode());
		final List<String> trueChars = Utils.trueChars(zh);
		final List<SimpleLookup> oneThree = DbServiceUtils.convertRawToSimple(db.lookupChinese(trueChars.get(0) + trueChars.get(2)));
		if(zh.length() == MIN_DEINTERLACE)
		{
			db.close();
			return oneThree;
		}
		
		final List<SimpleLookup> twoFour = DbServiceUtils.convertRawToSimple(db.lookupChinese(trueChars.get(1) + trueChars.get(3)));
		oneThree.addAll(twoFour);
		db.close();
		return oneThree;
	}
}

package dt.jdictionary.sqlite.dbservice.alternative;

import java.util.List;

import dt.jdictionary.SimpleLookup;
import dt.jdictionary.sqlite.raw.DbRepo;

public interface AlternateSearch 
{
	public abstract List<SimpleLookup> trySearch(String zh, DbRepo db);
	public abstract String getAltSearchType();
}

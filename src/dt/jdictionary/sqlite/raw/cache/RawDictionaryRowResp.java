package dt.jdictionary.sqlite.raw.cache;

import java.util.List;

import dt.jdictionary.sqlite.raw.RawDictionaryRow;

public class RawDictionaryRowResp 
{
	private final boolean hasResult;
	private final List<RawDictionaryRow> rawDictionaryRow;

	public RawDictionaryRowResp(boolean hasResult, List<RawDictionaryRow> rawDictionaryRow) 
	{
		this.hasResult = hasResult;
		this.rawDictionaryRow = rawDictionaryRow;
	}

	public boolean foundResult() 
	{
		return hasResult;
	}

	public List<RawDictionaryRow> getRawDictionaryRow() 
	{
		return rawDictionaryRow;
	}
}

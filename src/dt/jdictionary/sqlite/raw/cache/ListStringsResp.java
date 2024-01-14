package dt.jdictionary.sqlite.raw.cache;

import java.util.List;

public class ListStringsResp 
{
	private final boolean hasResult;
	private final List<String> result;

	public ListStringsResp(boolean hasResult, List<String> result) 
	{
		this.hasResult = hasResult;
		this.result = result;
	}

	public boolean foundResult() 
	{
		return hasResult;
	}

	public List<String> getResult() 
	{
		return result;
	}
}

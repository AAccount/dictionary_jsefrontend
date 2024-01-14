package dt.jdictionary.sqlite.raw.cache;

public class StringResp 
{
	private final boolean hasResult;
	private final String result;	

	public StringResp(boolean hasResult, String result) 
	{
		this.hasResult = hasResult;
		this.result = result;
	}

	public boolean foundResult() 
	{
		return hasResult;
	}

	public String getResult() 
	{
		return result;
	}
}

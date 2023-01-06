package dt.jdictionary.sqlite.raw;

public class RawSubstringRow 
{
	final String substring;
	final String fullString;

	public RawSubstringRow(String substring, String fourChar) 
	{
		this.substring = substring;
		this.fullString = fourChar;
	}

	public String getSubstring() 
	{
		return substring;
	}

	public String getFullString() 
	{
		return fullString;
	}

	@Override
	public String toString() 
	{
		return "Raw4CharRow [substring=" + substring + ", fourChar=" + fullString + "]";
	}

	@Override
	public int hashCode() 
	{
		return toString().hashCode();
	}

	@Override
	public boolean equals(Object obj) 
	{
		if(obj == null || !obj.getClass().equals(this.getClass()))
		{
			return false;
		}

		final RawSubstringRow casted = (RawSubstringRow)obj;
		return
			casted.substring.equals(this.substring) &&
			casted.fullString.equals(this.fullString);
	}
}

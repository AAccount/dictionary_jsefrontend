package dt.jdictionary.sqlite.raw;

public class Raw4CharRow 
{
	final String substring;
	final String fourChar;

	public Raw4CharRow(String substring, String fourChar) 
	{
		this.substring = substring;
		this.fourChar = fourChar;
	}

	public String getSubstring() 
	{
		return substring;
	}

	public String getFourChar() 
	{
		return fourChar;
	}

	@Override
	public String toString() 
	{
		return "Raw4CharRow [substring=" + substring + ", fourChar=" + fourChar + "]";
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

		final Raw4CharRow casted = (Raw4CharRow)obj;
		return
			casted.substring.equals(this.substring) &&
			casted.fourChar.equals(this.fourChar);
	}
}

package dt.jdictionary.sqlite;

import java.util.List;

import dt.jdictionary.Utils;

class RawDictionaryRow 
{
	private final String zh;
	private final String pinyin;
	private final String singleDefinition;
	private final String firstChar;
	private final String lastChar;

	public RawDictionaryRow(String zh, String rawPinyin, String singleDefinition) 
	{
		this.zh = zh;
		this.pinyin = rawPinyin;
		this.singleDefinition = singleDefinition;

		final List<String> trueChars = Utils.trueChars(zh);
		this.firstChar = trueChars.size() > 1 ? trueChars.get(0) : null;
		this.lastChar = trueChars.size() > 1 ? trueChars.get(trueChars.size()-1) : null;
	}

	public RawDictionaryRow(String zh, String rawPinyin, String singleDefinition, String firstChar, String lastChar) 
	{
		this.zh = zh;
		this.pinyin = rawPinyin;
		this.singleDefinition = singleDefinition;
		this.firstChar = firstChar;
		this.lastChar = lastChar;
	}

	public String getZh() 
	{
		return zh;
	}

	public String getPinyin() 
	{
		return pinyin;
	}

	public String getSingleDefinition() 
	{
		return singleDefinition;
	}
	
	public String getFirstChar() 
	{
		return firstChar;
	}

	public String getLastChar() 
	{
		return lastChar;
	}

	@Override
	public int hashCode()
	{
		return toString().hashCode();
	}

	@Override
	public String toString() 
	{
		return "RawDictionaryRow [zh=" + zh + ", pinyin=" + pinyin + ", singleDefinition=" + singleDefinition + "]";
	}

	@Override
	public boolean equals(Object obj)
	{
		if(obj == null || !obj.getClass().equals(this.getClass()))
		{
			return false;
		}
		
		final RawDictionaryRow casted = (RawDictionaryRow)obj;
		return 
			casted.zh.equals(this.zh) && 
			casted.pinyin.equals(this.pinyin) && 
			casted.singleDefinition.equals(this.singleDefinition);
	}
}

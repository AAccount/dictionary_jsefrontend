package dt.jdictionary.sqlite;

import dt.jdictionary.cedict.PinyinParser;

class RawDictionaryRow 
{
	private final String zh;
	private final String pinyin;
	private final String singleDefinition;

	public RawDictionaryRow(String zh, String rawPinyin, String singleDefinition) 
	{
		this.zh = zh;
		this.pinyin = PinyinParser.recreate(rawPinyin);
		this.singleDefinition = PinyinParser.recreateEmbeddedPinyin(singleDefinition);
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

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
}

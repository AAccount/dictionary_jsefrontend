package dt.jdictionary.sqlite;

public class RawDbRow 
{
	private final String zh;
	private final String pinyin;
	private final String singleDefinition;

	public RawDbRow(String zh, String rawPinyin, String singleDefinition) 
	{
		this.zh = zh;
		this.pinyin = Pinyin.recreate(rawPinyin);
		this.singleDefinition = singleDefinition;
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

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
		this.singleDefinition = recreateEmbeddedPinyin(singleDefinition);
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

	private String recreateEmbeddedPinyin(String raw)
	{
		final int start = raw.indexOf("[");
		final int end = raw.indexOf("]");
		final int NOT_FOUND = -1;
		if(start == NOT_FOUND || end == NOT_FOUND || start >= end)
		{
			return raw;
		}

		final String recreated = Pinyin.recreate(raw.substring(start+1, end));
		return raw.substring(0, start) + " " + recreated + " " + raw.substring(end+1);
	}
}

package dt.jdictionary.cedict;

public class ZhPinyin 
{
	private final String zh;
	private final String pinyin;

	public ZhPinyin(String zh, String pinyin) 
	{
		this.zh = zh;
		this.pinyin = pinyin;
	}

	public String getZh() 
	{
		return zh;
	}

	public String getPinyin() 
	{
		return pinyin;
	}
}

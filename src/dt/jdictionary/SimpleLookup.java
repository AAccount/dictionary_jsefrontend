package dt.jdictionary;
import java.util.List;

public class SimpleLookup 
{
	private final String zh;
	private final String pinyin;
	private final List<String> definitions;

	public SimpleLookup(String zh, String pinyin, List<String> definitions) 
	{
		this.zh = zh;
		this.pinyin = pinyin;
		this.definitions = definitions;
	}

	public String getZh() 
	{
		return zh;
	}

	public String getPinyin() 
	{
		return pinyin;
	}

	public List<String> getDefinitions() 
	{
		return definitions;
	}
}

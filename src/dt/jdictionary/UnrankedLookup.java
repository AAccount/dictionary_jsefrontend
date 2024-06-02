package dt.jdictionary;

import java.util.List;

public class UnrankedLookup
{
	private final String zh;
	private final String pinyin;
	private final List<String> definitions;

	public UnrankedLookup(String zh, String pinyin, List<String> definitions) 
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

	@Override
	public String toString() 
	{
		return "UnrankedLookup [zh=" + zh + ", pinyin=" + pinyin + ", definitions=" + definitions + "]";
	}

	// Definitions are based on the zh and pinyin. No need to compare those.
	@Override
	public int hashCode() 
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((zh == null) ? 0 : zh.hashCode());
		result = prime * result + ((pinyin == null) ? 0 : pinyin.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) 
	{
		if(obj == null || !obj.getClass().equals(this.getClass()))
		{
			return false;
		}

		final UnrankedLookup casted = (UnrankedLookup)obj;
		return
			casted.zh.equals(this.zh) &&
			casted.pinyin.equals(this.pinyin);
	}
}

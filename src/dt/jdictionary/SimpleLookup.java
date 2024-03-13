package dt.jdictionary;
import java.util.List;

import dt.jdictionary.cedict.UnrankedLookup;

public class SimpleLookup implements Comparable<SimpleLookup>
{
	private final String zh;
	private final String pinyin;
	private final double rank;
	private final List<String> definitions;

	public SimpleLookup(String zh, String pinyin, List<String> definitions, double rank) 
	{
		this.zh = zh;
		this.pinyin = pinyin;
		this.definitions = definitions;
		this.rank = rank;
	}
	
	public SimpleLookup(UnrankedLookup unrankedLookup, double rank)
	{
		this.zh = unrankedLookup.getZh();
		this.pinyin = unrankedLookup.getPinyin();
		this.definitions = unrankedLookup.getDefinitions();
		this.rank = rank;
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

	public double getRank()
	{
		return rank;
	}

	@Override
	public String toString()
	{
		return "SimpleLookup [zh=" + zh + ", pinyin=" + pinyin + ", rank=" + rank + ", definitions=" + definitions + "]";
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

		final SimpleLookup casted = (SimpleLookup)obj;
		return
			casted.zh.equals(this.zh) &&
			casted.pinyin.equals(this.pinyin);
	}


	@Override
	public int compareTo(SimpleLookup other)
	{
		final double difference = this.rank - other.rank;
		return difference == 0 ? 0 : difference > 0 ? 1 : -1;
	}
}

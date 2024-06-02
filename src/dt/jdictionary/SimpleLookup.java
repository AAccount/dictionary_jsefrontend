package dt.jdictionary;
import java.util.List;

public class SimpleLookup extends UnrankedLookup implements Comparable<SimpleLookup>
{
	private final double rank;

	public SimpleLookup(String zh, String pinyin, List<String> definitions, double rank) 
	{
		super(zh, pinyin, definitions);
		this.rank = rank;
	}
	
	public SimpleLookup(UnrankedLookup unrankedLookup, double rank)
	{
		super(unrankedLookup.getZh(), unrankedLookup.getPinyin(), unrankedLookup.getDefinitions());
		this.rank = rank;
	}
	
	public double getRank()
	{
		return rank;
	}

	@Override
	public String toString()
	{
		return "SimpleLookup [zh=" + super.getZh() + ", pinyin=" + super.getPinyin() + ", rank=" + rank + ", definitions=" + super.getDefinitions() + "]";
	}

	// Definitions are based on the zh and pinyin. No need to compare those.
	@Override
	public int hashCode() 
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((super.getZh() == null) ? 0 : super.getZh().hashCode());
		result = prime * result + ((super.getPinyin() == null) ? 0 : super.getPinyin().hashCode());
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
			casted.getZh().equals(this.getZh()) &&
			casted.getPinyin().equals(this.getPinyin());
	}


	@Override
	public int compareTo(SimpleLookup other)
	{
		final double difference = this.rank - other.rank;
		return difference == 0 ? 0 : difference > 0 ? 1 : -1;
	}
}

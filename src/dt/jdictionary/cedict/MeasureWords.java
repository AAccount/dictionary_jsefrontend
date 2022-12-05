package dt.jdictionary.cedict;

import java.util.List;

public class MeasureWords
{
	private final String zh;
	private final List<ZhPinyin> measures;
	
	public MeasureWords(String zh, List<ZhPinyin> measures) 
	{
		this.zh = zh;
		this.measures = measures;
	}

	public String getZh() 
	{
		return zh;
	}

	public List<ZhPinyin> getMeasures() 
	{
		return measures;
	}	
}
package dt.jdictionary.cedict;

import java.util.List;

class RawCedictLine 
{
	private final String original;
	private final String simplified;
	private final String pinyin;
	private final List<String> rawDefinitions;

	public RawCedictLine(String original, String simplified, String rawPinyin, List<String> rawDefinitions) 
	{
		this.original = original;
		this.simplified = simplified;
		this.pinyin = PinyinParser.recreateEmbeddedPinyin(rawPinyin).strip();
		this.rawDefinitions = rawDefinitions;
	}

	public String getOriginal() 
	{
		return original;
	}

	public String getSimplified() 
	{
		return simplified;
	}

	public String getPinyin()
	{
		return pinyin;
	}

	public List<String> getRawDefinitions() 
	{
		return rawDefinitions;
	}	

	
}

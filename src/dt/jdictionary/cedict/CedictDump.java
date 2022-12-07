package dt.jdictionary.cedict;

import java.util.List;
import java.util.Map;

import dt.jdictionary.SimpleLookup;

public class CedictDump 
{
	private final List<SimpleLookup> dictionary;
	private final List<MeasureWords> measureWords;
	private final Map<String, String> simplifiedChars;

	public CedictDump(
		List<SimpleLookup> definitions, 
		List<MeasureWords> measureWords, 
		Map<String, String> simplifiedChars) 
	{
		this.dictionary = definitions;
		this.measureWords = measureWords;
		this.simplifiedChars = simplifiedChars;
	}

	public List<SimpleLookup> getDictionary() 
	{
		return dictionary;
	}

	public List<MeasureWords> getMeasureWords() 
	{
		return measureWords;
	}

	public Map<String, String> getSimplifiedChars() 
	{
		return simplifiedChars;
	}
}

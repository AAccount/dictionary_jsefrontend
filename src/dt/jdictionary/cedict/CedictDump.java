package dt.jdictionary.cedict;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dt.jdictionary.UnrankedLookup;

public class CedictDump 
{
	private final List<UnrankedLookup> dictionary;
	private final List<MeasureWords> measureWords;
	private final Map<String, String> simplifiedChars;

	public CedictDump() 
	{
		this.dictionary = new ArrayList<>();
		this.measureWords = new ArrayList<>();
		this.simplifiedChars = new HashMap<>();
	}

	public List<UnrankedLookup> getDictionary() 
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

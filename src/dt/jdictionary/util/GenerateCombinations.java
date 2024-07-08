package dt.jdictionary.util;

import java.util.ArrayList;
import java.util.List;

public class GenerateCombinations
{

	public static <T> List<List<T>> generateCombinations(List<T> elements)
	{
		final int MINIMUM_USEABLE_STRING = 1;
		if(elements.size() < MINIMUM_USEABLE_STRING)
		{
			return List.of();
		}
	
		final List<List<T>> result = new ArrayList<>();
		result.add(List.of(elements.get(0)));
		for(int i = 1; i < elements.size(); i++)
		{
			result.add(List.of(elements.get(0), elements.get(i)));
		}
		result.addAll(generateCombinations(elements.subList(1, elements.size())));
		return result;
	}

}

package dt.jdictionary.util;

import java.util.ArrayList;
import java.util.List;

public class ListUtils
{

	public static <T> List<List<T>> subdivideList(List<T> original, int subSize)
	{
		final List<List<T>> result = new ArrayList<>();
		int position = 0;
		while(position < original.size())
		{
			final int end = (position + subSize) > original.size() ? original.size() : position + subSize;
			result.add(original.subList(position, end));
			position = end;
		}
		return result;
	}

}

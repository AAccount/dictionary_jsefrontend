package dt.jdictionary.ui;

import java.util.ArrayList;
import java.util.List;

public class HistoryManager<T>
{
	private final List<T> entries;
	private int index;

	public HistoryManager()
	{
		entries = new ArrayList<>();
		index = -1; // actually start with "before" anything happens
	}

	public T setIndex(int requested)
	{
		if(requested < 0 || requested > entries.size()-1)
		{
			return entries.get(index);
		}

		index = requested;
		return entries.get(index);
	}

	public boolean canGoBack()
	{
		return index > 0;
	}

	public T goBack()
	{
		if(canGoBack())
		{
			index--;
		}
		return entries.get(index);
	}

	public boolean canGoFwd()
	{
		return index < entries.size()-1;
	}

	public T goFwd()
	{
		if(canGoFwd())
		{
			index++;
		}
		return entries.get(index);
	}

	// This is what makes this a history manager vs a generic pagination manager.
	private void clearAfterIndex()
	{
		if(canGoFwd())
		{
			entries.subList(index+1, entries.size()).clear();
		}
	}

	public void addSingleEntry(T entry)
	{
		clearAfterIndex();
		entries.add(entry);
		index++;
	}

	public void addAllEntries(List<T> newEntries)
	{
		clearAfterIndex();
		entries.addAll(newEntries);
		index = entries.size() - 1; // move the index to the end
	}

	public int getIndex()
	{
		return index;
	} 

	public int getSize()
	{
		return entries.size();
	}
}

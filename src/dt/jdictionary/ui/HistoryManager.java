package dt.jdictionary.ui;

import java.util.ArrayList;
import java.util.List;

public class HistoryManager<T>
{
	private final List<T> entries = new ArrayList<>();
	private int index = -1; // actually start with "before" anything happens
	private final int maxSize;

	public HistoryManager()
	{
		this.maxSize = 0;
	}

	public HistoryManager(int size)
	{
		this.maxSize = size;
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
		if(entries.size() == 0 || !entries.get(entries.size()-1).equals(entry))
		{
			clearAfterIndex();
			entries.add(entry);
			index++;
			trimEntries();
		}
	}

	public void addAllEntries(List<T> newEntries)
	{
		clearAfterIndex();
		entries.addAll(newEntries);
		index = entries.size() - 1; // move the index to the end
		trimEntries();
	}

	private void trimEntries()
	{
		if(this.maxSize < 1 || entries.size() <= this.maxSize)
		{
			return;
		}

		entries.subList(0, entries.size() - maxSize).clear();
		index = entries.size() - 1;
	}

	public int getIndex()
	{
		return index;
	} 

	public int getSize()
	{
		return entries.size();
	}

	public List<T> getCompleteHistoryReadonly()
	{
		return entries.stream().toList();
	}
}

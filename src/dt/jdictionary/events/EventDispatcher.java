package dt.jdictionary.events;

import java.util.HashSet;
import java.util.Set;

public class EventDispatcher 
{
	private static EventDispatcher itself;

	private Set<EventListener> listeners;
	private Object lock;

	private EventDispatcher()
	{
		listeners = new HashSet<>();
		lock = new Object();
	}
	
	public static EventDispatcher get()
	{
		if(itself == null)
		{
			itself = new EventDispatcher();
		}
		return itself;
	}

	public void register(EventListener listener)
	{
		synchronized(lock)
		{
			listeners.add(listener);
		}
	}

	public void deregister(EventListener listener)
	{
		synchronized(lock)
		{
			listeners.remove(listener);
		}
	}

	public void push(Event event)
	{
		synchronized(lock)
		{
			for(final EventListener listener : listeners)
			{
				listener.onEvent(event);
			}
		}
	}
}

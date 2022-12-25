package dt.jdictionary.events;

import java.util.Map;

public class Event 
{
	private final EventType type;
	private final Map<String, Object> data;

	public Event(EventType type, Map<String, Object> data) 
	{
		this.type = type;
		this.data = data;
	}

	public EventType getType() 
	{
		return type;
	}

	public Map<String, Object> getData() 
	{
		return data;
	}

	@Override
	public String toString() 
	{
		return "Event [type=" + type + ", data=" + data + "]";
	}

	@Override
	public int hashCode() 
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		result = prime * result + ((data == null) ? 0 : data.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) 
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;

		Event other = (Event) obj;
		if (type != other.type)
			return false;
		if (data == null) 
		{
			if (other.data != null)
				return false;
		} 
		else if (!data.equals(other.data))
			return false;
		return true;
	}
}

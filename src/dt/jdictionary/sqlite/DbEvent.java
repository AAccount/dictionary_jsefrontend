package dt.jdictionary.sqlite;

import java.util.Map;

import dt.jdictionary.events.Event;
import dt.jdictionary.events.EventDispatcher;
import dt.jdictionary.events.EventType;

public class DbEvent 
{
	public static final String EVENT_TRX_TOTAL = "total db transactions";
	public static final String EVENT_TRX_SOFAR = "db writes transactions so far";

	public static void sendProgressEvent(int trxed, int total)
	{
		final Map<String, Object> data = Map.of(
			DbEvent.EVENT_TRX_SOFAR, trxed,
			DbEvent.EVENT_TRX_TOTAL, total
		);
		final Event progress = new Event(EventType.DB_SAVE, data);
		EventDispatcher.get().push(progress);
	}
}

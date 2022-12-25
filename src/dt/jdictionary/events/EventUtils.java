package dt.jdictionary.events;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EventUtils 
{
	public static final String EVENT_ERR_CLASS = "exception class name";
	public static final String EVENT_ERR_MSG = "exception get localized message";
	public static final String EVENT_STACK_TRACE = "exception relevant stack trace";

	public static void sendError(Exception e)
	{
		e.printStackTrace();
		
		final StackTraceElement[] stackTrace = e.getStackTrace();
		final List<StackTraceElement> stackList =  Arrays.asList(stackTrace);
		final String relevantEntries = stackList.stream()
			.filter(element -> element.getClassName().contains("dt.jdictionary"))
			.map(element -> element.toString())
			.collect(Collectors.joining("\n"));
		
		final Map<String, Object> data = Map.of(
			EVENT_ERR_CLASS, e.getClass().getName(),
			EVENT_ERR_MSG, e.getLocalizedMessage(),
			EVENT_STACK_TRACE, relevantEntries
		);
		final Event err = new Event(EventType.JAVA_EXCEPTION, data);
		EventDispatcher.get().push(err);
	}
}

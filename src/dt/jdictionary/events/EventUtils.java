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
	public static final String EVENT_WARN_MSG = "warning message";
	
	public static final String EVENT_TOTAL_BYTES = "total bytes";
	public static final String EVENT_PROCESSED_BYTES = "processed bytes";

	public static void sendError(Exception e)
	{
		e.printStackTrace();
		final Map<String, Object> data = Map.of(
			EVENT_ERR_CLASS, e.getClass().getName(),
			EVENT_ERR_MSG, e.getLocalizedMessage(),
			EVENT_STACK_TRACE, printRelevantEntries(Arrays.asList(e.getStackTrace()))
		);
		final Event err = new Event(EventType.JAVA_EXCEPTION, data);
		EventDispatcher.get().push(err);
	}

	public static void sendWarning(String message)
	{
		final List<StackTraceElement> stackTrace = Arrays.asList(Thread.currentThread().getStackTrace());
		final Map<String, Object> data = Map.of(
			EVENT_WARN_MSG, message,
			EVENT_STACK_TRACE, printRelevantEntries(stackTrace)
		);
		System.out.println(message + "\n" + data.get(EVENT_STACK_TRACE));
		final Event err = new Event(EventType.SELF_WARNING, data);
		EventDispatcher.get().push(err);
	}

	private static String printRelevantEntries(List<StackTraceElement> stack)
	{
		return stack.stream()
			.filter(element -> element.getClassName().contains("dt.jdictionary"))
			.map(element -> element.toString())
			.collect(Collectors.joining("\n"));
	}
	
	public static void sendBytesProcessed(long bytesProcessed, long bytesTotal)
	{
		final Map<String, Object> data = Map.of(
			EventUtils.EVENT_PROCESSED_BYTES, bytesProcessed,
			EventUtils.EVENT_TOTAL_BYTES, bytesTotal
		);
		final Event progress = new Event(EventType.FILE_PARSE, data);
		EventDispatcher.get().push(progress);
	}
}

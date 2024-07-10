package dt.jdictionary.sqlite.load;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dt.jdictionary.events.Event;
import dt.jdictionary.events.EventDispatcher;
import dt.jdictionary.events.EventType;
import dt.jdictionary.events.EventUtils;
import dt.jdictionary.events.FileParseEventKey;
import dt.jdictionary.util.ChineseText;

public class WordList
{	
	public List<String> parse(File file)
	{
		final Set<String> hashSet = new HashSet<>();
		long bytesProcessed = 0;
		try
		{
			final BufferedReader fileReader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
			String line = fileReader.readLine();
			while (line != null)
			{
				final String cleaned = line.strip();
				if(cleaned.length() > 1 && ChineseText.allChinese(cleaned)) // Past hits is intended to help prioritize compound words.
				{
					hashSet.add(cleaned);
				}
				bytesProcessed = bytesProcessed + line.length();
				sendProgressEvent(bytesProcessed, file.length());
				line = fileReader.readLine();
			}
			fileReader.close();
		}
		catch(IOException e)
		{
			EventUtils.sendError(e);
		}
		return new ArrayList<String>(hashSet);
	}
	
	private void sendProgressEvent(long bytesProcessed, long bytesTotal)
	{
		final Map<String, Object> data = Map.of(
			FileParseEventKey.EVENT_PROCESSED_BYTES, bytesProcessed,
			FileParseEventKey.EVENT_TOTAL_BYTES, bytesTotal
		);
		final Event progress = new Event(EventType.FILE_PARSE, data);
		EventDispatcher.get().push(progress);
	}
}

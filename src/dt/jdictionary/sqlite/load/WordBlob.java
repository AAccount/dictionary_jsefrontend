package dt.jdictionary.sqlite.load;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import dt.jdictionary.events.EventUtils;
import dt.jdictionary.util.ChineseText;

public class WordBlob
{
	public List<String> parse(File file)
	{
		final List<String> sentences = new ArrayList<>();
		long bytesProcessed = 0;
		try
		{
			final BufferedReader fileReader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
			String line = fileReader.readLine();
			while (line != null)
			{
				final String cleaned = line.strip();
				if(cleaned.length() < 1)
				{
					line = fileReader.readLine();
				}
				
				final String[] lineSentences = line.split("\\.\\?\\,。？，");
				for(final String sentence : lineSentences)
				{
					sentences.add(ChineseText.stripNonChinese(sentence));
				}
				
				bytesProcessed = bytesProcessed + line.length();
				EventUtils.sendBytesProcessed(bytesProcessed, file.length());
				line = fileReader.readLine();
			}
			fileReader.close();
		}
		catch(IOException e)
		{
			EventUtils.sendError(e);
		}
		return sentences;
	}
}

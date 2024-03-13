package dt.jdictionary.cedict;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import dt.jdictionary.Utils;
import dt.jdictionary.events.Event;
import dt.jdictionary.events.EventDispatcher;
import dt.jdictionary.events.EventType;
import dt.jdictionary.events.EventUtils;

public class CedictParser 
{
	public static final String EVENT_TOTAL_BYTES = "total bytes";
	public static final String EVENT_PROCESSED_BYTES = "processed bytes";

	private final String MEASURE_WORD_INDICATOR = "CL:";
	private final String OG_SIMPLIFIED_SPLIT = "|";

	public CedictDump parse(File cedictFile)
	{
		long processedBytes = 0;
		sendProgressEvent(processedBytes, cedictFile.length());

		final CedictDump result = new CedictDump();
		try 
		{
			final BufferedReader cedictReader =  new BufferedReader(new InputStreamReader(new FileInputStream(cedictFile), StandardCharsets.UTF_8));
			String line = cedictReader.readLine();
			
			while (line != null)
			{
				final RawCedictLine parsedLine = parseLine(line);
				if(parsedLine == null)
				{
					line = cedictReader.readLine();
					processedBytes = processedBytes + line.getBytes().length;
					sendProgressEvent(processedBytes, cedictFile.length());
					continue;
				}

				final List<String> definitions = parseDefinitions(parsedLine);
				result.getDictionary().add(new UnrankedLookup(parsedLine.getOriginal(), parsedLine.getPinyin(), definitions));
				final Map<String, String> simplifiedChars = getSimplifiedChars(parsedLine);
				result.getSimplifiedChars().putAll(simplifiedChars);
				final List<ZhPinyin> measureWords = procDefsMeasureWords(parsedLine);
				if(measureWords.size() > 0)
				{
					result.getMeasureWords().add(new MeasureWords(parsedLine.getOriginal(), measureWords));
				}

				processedBytes = processedBytes + line.getBytes().length;
				sendProgressEvent(processedBytes, cedictFile.length());
				line = cedictReader.readLine();
			}
			cedictReader.close();
			sendProgressEvent(cedictFile.length(), cedictFile.length());
		} 
		catch (FileNotFoundException e) 
		{
			EventUtils.sendError(e);
		} 
		catch (IOException e) 
		{
			EventUtils.sendError(e);
		}
		return result;
	}

	private void sendProgressEvent(long bytesProcessed, long bytesTotal)
	{
		final Map<String, Object> data = Map.of(
			EVENT_PROCESSED_BYTES, bytesProcessed,
			EVENT_TOTAL_BYTES, bytesTotal
		);
		final Event progress = new Event(EventType.CEDICT_PARSE, data);
		EventDispatcher.get().push(progress);
	}

	private List<String> parseDefinitions(RawCedictLine line)
	{
		final List<String> rawDefinitions = line.getRawDefinitions().stream().map(rawDef -> new String(rawDef)).toList();
		final List<String> pinyin = procDefsEmbeddedPinyin(rawDefinitions);
		final List<String> pinyinNoSimplified = procDefsRmSimplified(pinyin);
		final List<String> pinyinNoSimplifiedNoMW = procDefsRmMeasureWords(pinyinNoSimplified);
		return dedupDefinitions(pinyinNoSimplifiedNoMW);
	}

	private Map<String, String> getSimplifiedChars(RawCedictLine line)
	{
		final Map<String, String> selfSimplifiedChars = catalogSimplified(line.getOriginal(), line.getSimplified());
		final Map<String, String> defsSimplifiedChars = procDefsEmbeddedSimplified(line.getRawDefinitions());
		selfSimplifiedChars.putAll(defsSimplifiedChars);
return selfSimplifiedChars;
	}

	private RawCedictLine parseLine(String line)
	{
		if(line.charAt(0) == '#')
		{
			return null;
		}

		final int pinyinStart = line.indexOf("[");
		final int pinyinEnd = line.indexOf("]");
		final int NOT_FOUND = -1;

		if(pinyinStart == NOT_FOUND || pinyinEnd == NOT_FOUND || pinyinStart >= pinyinEnd)
		{
			EventUtils.sendWarning("Couldn't parse line: " + line);
			return null;
		}

		final String zhPortion = line.substring(0, pinyinStart).strip();
		final String[] zhParts = zhPortion.split(" ");
		if(zhParts.length != 2)
		{
			EventUtils.sendWarning("Couldn't parse the simplified and traditional portion of: '" + zhPortion + "' from '" + line + "'");
			return null;
		}
		final String zhTraditional = zhParts[0].strip();
		final String zhSimplified = zhParts[1].strip();
		final String pinyinPortion = line.substring(pinyinStart, pinyinEnd+1).strip();
		final String definitionPortion = line.substring(pinyinEnd+1).strip();
		final List<String> definitions = processRawDefinitions(definitionPortion);

		return new RawCedictLine(zhTraditional, zhSimplified, pinyinPortion, definitions);
	}

	private List<String> processRawDefinitions(String definitionPortion)
	{
		final String sanitized = sanitizeRawDefinitions(definitionPortion);
		final List<String> definitions = Arrays.asList(sanitized.split("/"));
		return definitions.stream().filter(def -> def.length() > 0).toList();
	}

	private final String sanitizeRawDefinitions(String rawDefinitions)
	{
		final String IMPROPER_MEASURE_WORD_MARKER = "(CL:";
		if(!rawDefinitions.contains(IMPROPER_MEASURE_WORD_MARKER))
		{
			return rawDefinitions;
		}

		/**
		 * On VERY rare occasions cedict breaks its own rules marking measure words as (CL:XX)
		 * instead of marking them as /separate/definitions/.
		 */
		final String definitionCleaned = rawDefinitions.replace(IMPROPER_MEASURE_WORD_MARKER, "/CL:");
		/**
		 * Have to use the og definition portion, only a 1 char difference
		 * but it's the 1 char needed to spot the improperly formatted measure word ending ")"
		 */
		final int endBracket = rawDefinitions.indexOf(")", rawDefinitions.indexOf(IMPROPER_MEASURE_WORD_MARKER));
		final char[] underlyingChars = definitionCleaned.toCharArray();
		underlyingChars[endBracket] = ' ';
		return String.valueOf(underlyingChars);
	}

	private Map<String, String> catalogSimplified(String original, String simplified)
	{
		Map<String, String> result = new HashMap<>();
		final String originalCleaned = makeStringChineseOnly(original);
		final String simplifiedCleaned = makeStringChineseOnly(simplified);

		final List<String> ogchars = Utils.trueChars(originalCleaned);
		final List<String> simplifiedchars = Utils.trueChars(simplifiedCleaned);
		final boolean sameLength = ogchars.size() == simplifiedchars.size();

		if(!sameLength)
		{
			EventUtils.sendWarning(originalCleaned + " and " + simplified + " are not the same perceived length. Ignoring.");
			return result;
		}

		for(int i=0; i<ogchars.size(); i++)
		{
			final String ogchar = ogchars.get(i);
			final String simplifiedchar = simplifiedchars.get(i);
			if(!ogchar.equals(simplifiedchar))
			{ 
				result.put(ogchar, simplifiedchar);
			}
		}
		return result;
	}

	private String makeStringChineseOnly(String string)
	{
		Set<Character.UnicodeScript> chineseEncoded = Set.of(Character.UnicodeScript.UNKNOWN, Character.UnicodeScript.HAN);
		List<Character> stringAsCharObjs = new ArrayList<>();
		for(char single : string.toCharArray())
		{
			stringAsCharObjs.add(single);
		}
		
		List<Character> remainingChinese =  stringAsCharObjs
			.stream().filter(jchar -> chineseEncoded.contains(Character.UnicodeScript.of(jchar))).toList();
		
		return remainingChinese.size() == 0 ? "" : remainingChinese.stream()
			.map(jchar -> jchar.toString())
			.reduce((acc, singlestring) -> acc + singlestring)
			.get();
	}

	private List<String> procDefsEmbeddedPinyin(List<String> rawDefinitions)
	{
		return rawDefinitions.stream()
			.map(raw -> raw.contains(MEASURE_WORD_INDICATOR) ? raw : PinyinParser.parse(raw))
			.toList();
	}

	private Map<String, String> procDefsEmbeddedSimplified(List<String> rawDefinitions)
	{
		final Map<String, String> result = new HashMap<>();
		for(final String rawDef : rawDefinitions)
		{
			if(!rawDef.contains(OG_SIMPLIFIED_SPLIT) || rawDef.contains(MEASURE_WORD_INDICATOR))
			{
				// Measure words will have their own dictionary entries. Don't mess with measure definitions here.
				continue;
			}

			parseEmbeddedSimplifed(rawDef.split(" ")).stream()
				.filter(tuple -> tuple.getSimplified() != null)
				.forEach(tuple -> result.putAll(catalogSimplified(tuple.getOriginal(), tuple.getSimplified())));
		}
		return result;
	}
	
	private List<String> procDefsRmSimplified(List<String> rawDefinitions)
	{
		final List<String> result = new ArrayList<>();
		for(final String rawDef : rawDefinitions)
		{
			if(!rawDef.contains(OG_SIMPLIFIED_SPLIT) || rawDef.contains(MEASURE_WORD_INDICATOR))
			{
				result.add(rawDef);
				continue;
			}


			final String processedDef = parseEmbeddedSimplifed(rawDef.split(" ")).stream()
				.map(SimpOgTuple::getOriginal)
				.collect(Collectors.joining(" "));
			result.add(processedDef);
		}
		return result;
	}

	private List<SimpOgTuple> parseEmbeddedSimplifed(String[] defWords)
	{
		final List<SimpOgTuple> result = new ArrayList<>();
		boolean skipDefWord = false;
		for(int i=0; i<defWords.length; i++)
		{
			if(skipDefWord)
			{
				skipDefWord = false;
				continue;
			}
			
			final String defWord = defWords[i];
			if(!defWord.contains(OG_SIMPLIFIED_SPLIT))
			{
				result.add(new SimpOgTuple(defWord, null));
				continue;
			}
			final String cleanedDefWord = defWord.replace("(", "");
			final int split = cleanedDefWord.indexOf(OG_SIMPLIFIED_SPLIT);

			/*
			 * The end of the original substring is where the split "|" is.
			 * It doesn't matter if the last char of the original was parseable by java or not.
			 * The "|" will always come after it.
			 */
			final String original = cleanedDefWord.substring(0, split).strip();

			/*
			 * Some times definitions have"traidional| simplified". 
			 * That space between "|" and the beginning of simplified messes up the parsing.
			 * The assumption was "traditional|simplified" which used "|" to remove the simplified portion.
			 * If there is nothing to remove after the "|", the simplified is the next word.
			 */
			final int simplifiedStart = split+1;
			if(simplifiedStart >= defWord.length())
			{
				result.add(new SimpOgTuple(original, defWords[i+1].strip()));
				skipDefWord = true;
			}
			else
			{
				/*
				 * Unlike the original, there is no predictable text that comes after the simplified.
				 * Can't use the original's string length. What if the simplified has unparseable chars, inflating the length?
				 * At the mercy of java's string parseability. It will need to be cleaned downstream.
				 */
				final String simplified = cleanedDefWord.substring(simplifiedStart, cleanedDefWord.length()).strip();
				result.add(new SimpOgTuple(original, simplified));
			}
		}
		return result;
	}

	
	private List<String> procDefsRmMeasureWords(List<String> rawDefinitions)
	{
		return rawDefinitions.stream().filter(rawDef -> !rawDef.contains(MEASURE_WORD_INDICATOR)).toList();
	}

	private List<ZhPinyin> procDefsMeasureWords(RawCedictLine line)
	{
		final List<ZhPinyin> result = new ArrayList<>();
		for(final String rawDef : line.getRawDefinitions())
		{
			if(!rawDef.contains(MEASURE_WORD_INDICATOR))
			{
				continue;
			}

			final String[] measureWords = rawDef.substring(MEASURE_WORD_INDICATOR.length()).split(",");
			for(final String rawMeasureWord : measureWords)
			{
				final int pinyinStart = rawMeasureWord.indexOf("[");
				final int pinyinEnd = rawMeasureWord.indexOf("]");
				final int NOT_FOUND = -1;
				final int split = rawMeasureWord.indexOf(OG_SIMPLIFIED_SPLIT);

				final String rawPinyin = rawMeasureWord.substring(pinyinStart, pinyinEnd+1);
				final String pinyin = PinyinParser.parse(rawPinyin).strip();
				final String measureChar = rawMeasureWord.substring(0, split == NOT_FOUND ? pinyinStart : split);
				result.add(new ZhPinyin(measureChar, pinyin));
			}
		}
		return result;
	}

	// Cedict file will break its own rules and have an unescaped "/" in the definition causing possible duplicates.
	private List<String> dedupDefinitions(List<String> rawDefinitions)
	{
		final Set<String> tracker = new HashSet<>();
		rawDefinitions.stream().forEach(def -> tracker.add(def.strip()));
		return new ArrayList<>(tracker);
	}
}

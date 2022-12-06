package dt.jdictionary.cedict;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dt.jdictionary.SimpleLookup;

public class CedictParser 
{
	private final String MEASURE_WORD_INDICATOR = "CL:";
	private final String OG_SIMPLIFIED_SPLIT = "|";

	public CedictDump parse(File cedictFile)
	{
		final CedictDump result = new CedictDump(new ArrayList<>(), new ArrayList<>(), new HashMap<>());
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
					continue;
				}
				final Map<String, String> selfSimplifiedChars = catalogSimplified(parsedLine.getOriginal(), parsedLine.getSimplified());
				final List<String> defsPinyinProced = procDefsEmbeddedPinyin(parsedLine.getRawDefinitions());
				final Map<String, String> defsSimplifiedChars = procDefsEmbeddedSimplified(defsPinyinProced);
				final List<String> defPinyinSimplifiedProced = procDefsRmSimplified(defsPinyinProced);
				final List<ZhPinyin> measureWords = procDefsMeasureWords(defPinyinSimplifiedProced);
				final List<String> defPinyinSimplifiedMeasureWordProced = procDefsRmMeasureWords(defPinyinSimplifiedProced);
				final List<String> dedupFinalDefinitions = dedupDefinitions(defPinyinSimplifiedMeasureWordProced);
				result.getDefinitions().add(new SimpleLookup(parsedLine.getOriginal(), parsedLine.getPinyin(), dedupFinalDefinitions));
				result.getSimplifiedChars().putAll(selfSimplifiedChars);
				result.getSimplifiedChars().putAll(defsSimplifiedChars);
				if(measureWords.size() > 0)
				{
					result.getMeasureWords().add(new MeasureWords(parsedLine.getOriginal(), measureWords));
				}

				line = cedictReader.readLine();
			}
			cedictReader.close();
		} 
		catch (FileNotFoundException e) 
		{
			System.out.println(cedictFile.getAbsolutePath() + " does not exist");
			e.printStackTrace();
		} 
		catch (IOException e) 
		{
			System.out.println("Couldn't read a line from the cedict file");
			e.printStackTrace();
		}
		System.out.println("Finished parsing.");
		return result;
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
			System.out.println("Couldn't parse line: " + line);
			return null;
		}

		final String zhPortion = line.substring(0, pinyinStart).strip();
		final String[] zhParts = zhPortion.split(" ");
		if(zhParts.length != 2)
		{
			System.out.println("Couldn't parse the simplified and traditional portion of: '" + zhPortion + "' from '" + line + "'");
			return null;
		}
		final String zhTraditional = zhParts[0].strip();
		final String zhSimplified = zhParts[1].strip();


		final String pinyinPortion = line.substring(pinyinStart, pinyinEnd+1).strip();
		final String definitionPortion = line.substring(pinyinEnd+1).strip();
		final String definitionCleaned = cleanRawDefinitionsString(definitionPortion);
		final String[] definitions = definitionCleaned.split("/");
		final List<String> useableDefinitions = new ArrayList<>();
		for(final String definition : definitions)
		{
			if(definition.length() != 0)
			{
				useableDefinitions.add(definition);
			}
		}
		return new RawCedictLine(zhTraditional, zhSimplified, pinyinPortion, useableDefinitions);
	}

	private final String cleanRawDefinitionsString(String rawDefinitions)
	{
		final String IMPROPER_MEASURE_WORD_MARKER = "(CL:";
		if(!rawDefinitions.contains(IMPROPER_MEASURE_WORD_MARKER))
		{
			return rawDefinitions;
		}

		/**
		 * On VERY rare occasions cedict breaks its own rules marking measure words as (CL:XX)
		 * instead of marking them as separate definitions.
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

		final String originalCleaned = original.replace("(", "");
		final boolean sameLength = originalCleaned.length() == simplified.length();

		if(!sameLength)
		{
			System.out.println(originalCleaned + " and " + simplified + " are not the same length. Ignoring.");
			return result;
		}

		final int maxIndex = sameLength ? originalCleaned.length() : Math.min(originalCleaned.length(), simplified.length());
		for(int i=0; i<maxIndex; i++)
		{
			final char ogchar = originalCleaned.charAt(i);
			final char simplifiedchar = simplified.charAt(i);
			if(ogchar != simplifiedchar)
			{
				result.put(Character.toString(ogchar), Character.toString(simplifiedchar));
			}
		}
		return result;
	}

	private List<String> procDefsEmbeddedPinyin(List<String> rawDefinitions)
	{
		final List<String> result = new ArrayList<>();
		for(final String rawDef : rawDefinitions)
		{
			if(rawDef.contains(MEASURE_WORD_INDICATOR)) // Let the measure word processor handle these.
			{
				result.add(rawDef);
				continue;
			}

			final int start = rawDef.indexOf("[");
			final int end = rawDef.indexOf("]");
			final int NOT_FOUND = -1;
			if(start == NOT_FOUND || end == NOT_FOUND || start >= end)
			{
				result.add(rawDef);
				continue;
			}

			final String first = rawDef.substring(0, start).strip();
			final String rawPinyin = rawDef.substring(start+1, end).strip();
			final String pinyin = PinyinParser.recreate(rawPinyin).strip();
			final String rest = rawDef.substring(end+1).strip();
			result.add(first + " " + pinyin + " " + rest);
		}

		return result;
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

			final String[] defWords = rawDef.split(" ");
			for(final String defWord : defWords)
			{
				if(!defWord.contains(OG_SIMPLIFIED_SPLIT))
				{
					continue;
				}
				final String cleanedDefWord = defWord.replace("(", "");
				final int split = cleanedDefWord.indexOf(OG_SIMPLIFIED_SPLIT);
				final String original = cleanedDefWord.substring(0, split).strip();
				final String simplified = cleanedDefWord.substring(split+1, split*2+1).strip();
				final Map<String, String> defResult = catalogSimplified(original, simplified);
				result.putAll(defResult);
			}
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

			final String[] defWords = rawDef.split(" ");
			String processedDef = "";
			for(final String defWord : defWords)
			{
				if(!defWord.contains(OG_SIMPLIFIED_SPLIT))
				{
					processedDef = processedDef + defWord + " ";
					continue;
				}
				final String cleanedDefWord = defWord.replace("(", "");
				final int split = cleanedDefWord.indexOf(OG_SIMPLIFIED_SPLIT);
				final String original = cleanedDefWord.substring(0, split).strip().replace("(", "");
				final String rest = cleanedDefWord.substring(split*2+1).strip();
				processedDef = processedDef + original + rest + " ";
			}
			result.add(processedDef.strip());
		}
		return result;
	}

	private List<String> procDefsRmMeasureWords(List<String> rawDefinitions)
	{
		final List<String> result = new ArrayList<>();
		for(final String rawDef : rawDefinitions)
		{
			if(!rawDef.contains(MEASURE_WORD_INDICATOR))
			{
				result.add(rawDef);
			}
		}
		return result;
	}

	private List<ZhPinyin> procDefsMeasureWords(List<String> rawDefinitions)
	{
		final List<ZhPinyin> result = new ArrayList<>();
		for(final String rawDef : rawDefinitions)
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

				final String rawPinyin = rawMeasureWord.substring(pinyinStart+1, pinyinEnd);
				final String pinyin = PinyinParser.recreate(rawPinyin);
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
		for(final String def : rawDefinitions)
		{
			final String cleaned = def.strip();
			tracker.add(cleaned);
		}
		final List<String> result =  new ArrayList<>();
		result.addAll(tracker);
		return result;
	}
}

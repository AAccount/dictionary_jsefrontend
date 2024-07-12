package dt.jdictionary.tui;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import dt.jdictionary.App;
import dt.jdictionary.ExhaustiveChineseLookup;
import dt.jdictionary.SimpleLookup;
import dt.jdictionary.sqlite.dbservice.ChineseDefinitionLookup;
import dt.jdictionary.sqlite.dbservice.DbService;
import dt.jdictionary.sqlite.dbservice.ExceptionPile;
import dt.jdictionary.ui.UiConstants;
import dt.jdictionary.util.ChineseText;
import dt.jdictionary.util.Debug;

public class TextUI
{
	private static final String CMD_HELP = ".h";
	private static final String CMD_FLAG = ".f";
	private static final String CMD_NRESULTS = ".r";
	private static final String CMD_QUIT = ".q";
	
	private static final String FLAG_SHORTHAND_RANK = "rank";
	private static final String FLAG_SHORTHAND_SWAP = "swap";
	private static final String FLAG_SHORTHAND_SINGLE_SUBSTRING = "single";
	private static final String FLAG_SHORTHAND_SAVE_HITS = "save";

	private int nresults = 20;
	private final DbService db;
	
	public TextUI() throws ClassNotFoundException, SQLException
	{
		db = new DbService();
	}
	
	public void print()
	{
		printHelp();
		printFlags();
		
		final Scanner kbdReader = new Scanner(System.in);
		while(true)
		{
			System.out.print("Entry: ");
			final String entry = kbdReader.nextLine().strip();
			
			if(entry.isEmpty())
			{
				continue;
			}
			else if(entry.charAt(0) == '.')
			{
				final boolean nextRound = handleCommand(entry);
				if(!nextRound)
				{
					break;
				}
			}
			else
			{
				handleLookup(entry.toLowerCase());
			}
		}
		kbdReader.close();
	}
	
	private void handleLookup(String entry)
	{
		if(ChineseText.hasChinese(entry))
		{
			printChineseLookup(entry);
		}
		else
		{
			printEnglishLookup(entry);
		}
	}
	
	private void printChineseLookup(String entry)
	{
		try
		{
			final ExhaustiveChineseLookup result = db.lookupChinese(entry, true);
			System.out.println("");
			printDefinition(result.getDefinition());
			for(final String alt : result.getSupplementaries().keySet())
			{
				printSimpleLookupList(alt, result.getSupplementaries().get(alt));
			}
		}
		catch(ExceptionPile e)
		{
			e.getExceptions().forEach(ex -> System.err.println(Debug.printStackTrace(ex.getCause().getStackTrace())));
		}
	}
	
	private void printDefinition(ChineseDefinitionLookup lookup)
	{
		final String lookedUp = lookup.getZh();
		System.out.println(String.format("Looked up: %s", lookedUp));
		for(final String pinyin : lookup.getResults().keySet())
		{
			System.out.println(String.format("Pinyin: %s", pinyin));
			final List<String> definitions = lookup.getResults().get(pinyin);
			System.out.println(String.format("Definition: %s", String.join(", ", definitions)));
		}
		
		final String simplified = lookup.getSimplified();
		if(!simplified.equals(lookedUp))
		{
			System.out.println(String.format("Simplified: %s", simplified));
		}
		
		final List<String> measureWords = lookup.getMeasureWords();
		if(!measureWords.isEmpty())
		{
			System.out.println(String.format("Measure words: %s", String.join(" ", measureWords)));
		}
		System.out.println("");
	}
	
	private void printEnglishLookup(String entry)
	{
		try
		{
			final Map<String, List<SimpleLookup>> results = db.lookupEnglish(entry);
			for(final String combo : results.keySet())
			{
				printSimpleLookupList(combo, results.get(combo));
			}
		}
		catch(ExceptionPile e)
		{
			e.getExceptions().forEach(ex -> System.err.println(Debug.printStackTrace(ex.getCause().getStackTrace())));
		}
	}
	
	private void printSimpleLookupList(String listName, List<SimpleLookup> list)
	{
		if(list.isEmpty())
		{
			return;
		}
		
		Collections.sort(list, Collections.reverseOrder());
		System.out.println(listName);
		final List<SimpleLookup> printedList = list.size() > nresults ? list.subList(0, nresults) : list;
		for(final SimpleLookup lookup : printedList)
		{
			if(UiConstants.getFlag(UiConstants.FLAG_RANK))
			{
				System.out.println(String.format("%s; %s; %s; %s", lookup.getZh(), lookup.getPinyin(), String.join("; ", lookup.getDefinitions()), lookup.getRank()));
			}
			else
			{
				System.out.println(String.format("%s; %s; %s", lookup.getZh(), lookup.getPinyin(), String.join("; ", lookup.getDefinitions())));
			}
		}
		System.out.println("");
	}

	/**
	 * @return should the text ui present another around or exit?
	 */
	private boolean handleCommand(String input)
	{
		final String[] parts = input.split(" ");
		final String command = parts[0];
		
		if(command.equals(CMD_HELP))
		{
			printHelp();
			return true;
		}
		else if(command.equals(CMD_QUIT))
		{
			return false;
		}
		else if(command.equals(CMD_FLAG))
		{
			handleFlag(parts);
			return true;
		}
		else if(command.equals(CMD_NRESULTS))
		{
			handleNResults(parts);
			return true;
		}
		return true;
	}
	
	private void handleNResults(String[] parts)
	{
		if(parts.length < 2)
		{
			return;
		}
		
		nresults = Integer.parseInt(parts[1], 10);
	}

	private void handleFlag(String[] parts)
	{
		final Set<String> allShorthands = Set.of(FLAG_SHORTHAND_RANK, FLAG_SHORTHAND_SAVE_HITS, FLAG_SHORTHAND_SINGLE_SUBSTRING, FLAG_SHORTHAND_SWAP);
		if(parts.length < 2 || !allShorthands.contains(parts[1]))
		{
			printFlags();
			return;
		}
		
		final String flag = parts[1];
		final Map<String, String> expandShorthand = Map.of(
			FLAG_SHORTHAND_RANK, UiConstants.FLAG_RANK,
			FLAG_SHORTHAND_SAVE_HITS, UiConstants.FLAG_SAVE_HITS,
			FLAG_SHORTHAND_SINGLE_SUBSTRING, UiConstants.FLAG_ALWAYS_SINGLE_SUBSTRING,
			FLAG_SHORTHAND_SWAP, UiConstants.FLAG_AUTOSWAP
		);
		UiConstants.toggleFlag(expandShorthand.get(flag));
	}
	
	private void printFlags()
	{
		System.out.println("Flags:");
		System.out.println(FLAG_SHORTHAND_RANK + " " + UiConstants.getFlag(UiConstants.FLAG_RANK));
		System.out.println(FLAG_SHORTHAND_SWAP + " " + UiConstants.getFlag(UiConstants.FLAG_AUTOSWAP));
		System.out.println(FLAG_SHORTHAND_SINGLE_SUBSTRING + " " + UiConstants.getFlag(UiConstants.FLAG_ALWAYS_SINGLE_SUBSTRING));
		System.out.println(FLAG_SHORTHAND_SAVE_HITS + " " + UiConstants.getFlag(UiConstants.FLAG_SAVE_HITS));
		System.out.println();
	}
	
	private void printHelp()
	{
		System.out.println("Dictionary " + App.VERSION);
		System.out.println(CMD_FLAG + " toggle flags: `.h _flag_; see flags: `.h`");
		System.out.println(CMD_HELP + " print this help message");
		System.out.println(CMD_NRESULTS + " change the maximum results shown");
		System.out.println(CMD_QUIT + " exit this program");
		System.out.println("");
	}
}

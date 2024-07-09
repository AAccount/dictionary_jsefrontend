package dt.jdictionary;

import dt.jdictionary.ui.UiConstants;
import dt.jdictionary.ui.UiMain;
import dt.jdictionary.util.Debug;

// V1.3: 2024-07-08: make List<SimpleLookup> based on past searches if available 
// V1.2: 2024-01-13: add history drop down menu and cache results
// V1.1: 2023-12-26: remove sketchy filtering of species names etc which is not useful
// V1.0: 2022-12-30: initial port of 2021 python gtk ui

public class App
{
	private static String ARG_TEST = "--test";
	
	public static void main(String[] args)
	{
		System.setProperty("apple.laf.useScreenMenuBar", "true");
		Debug.logTimestamp("Starting V1.3");
		UiConstants.initFlags();
		for(final String arg : args)
		{
			if(arg.equals(ARG_TEST))
			{
				UiConstants.flagMap.put(UiConstants.FLAG_SAVE_HITS, false);
				break;
			}
		}
		new UiMain().render();
	}
}

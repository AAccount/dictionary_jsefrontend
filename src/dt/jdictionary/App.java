package dt.jdictionary;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;

import dt.jdictionary.ui.UiConstants;
import dt.jdictionary.ui.UiMain;
import dt.util.Debug;

// V1.6: 2026-07-17: bring back sqlite backend. The in memory db takes forever to start andconsumes tons of ram. Also fix some cedict issues.
// V1.5: 2024-07-22: swap sqlite for glorified in memory key-value based db for common code with android
// V1.4: 2024-07-09: import known compound words in hopes of getting better English to Chinese results.
// V1.3: 2024-07-08: make List<SimpleLookup> rank based on past searches if available 
// V1.2: 2024-01-13: add history drop down menu and cache results
// V1.1: 2023-12-26: remove sketchy filtering of species names etc which is not useful
// V1.0: 2022-12-30: initial port of 2021 python gtk ui

public class App
{
	public static final String VERSION = "V1.6";
	private static final String ARG_TEST = "--test";
	
	public static void main(String[] args) throws IOException, ParseException, ClassNotFoundException, SQLException
	{
		System.setProperty("apple.laf.useScreenMenuBar", "true");
		Debug.logTimestamp("Starting " + VERSION);
		UiConstants.initFlags();
		
		for(final String arg : args)
		{
			if(arg.equals(ARG_TEST))
			{
				UiConstants.toggleFlag(UiConstants.FLAG_SAVE_HITS); // by default is true
			}
		}
		
		javax.swing.SwingUtilities.invokeLater(() -> {
			try 
			{
				new UiMain().render();
			} 
			catch (ClassNotFoundException | SQLException | IOException | ParseException e) 
			{
				e.printStackTrace();
			}
		});
	}
}

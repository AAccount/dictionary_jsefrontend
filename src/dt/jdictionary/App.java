package dt.jdictionary;

import dt.jdictionary.ui.UiConstants;
import dt.jdictionary.ui.UiMain;
import dt.jdictionary.util.Debug;

// V1.2: 2024-01-13: add history drop down menu and cache results
// V1.1: 2023-12-26: remove sketchy filtering of species names etc which is not useful when maginating
// V1.0: 2022-12-30: inital port of 2021 python gtk ui

public class App
{
	public static void main(String[] args)
	{
		System.setProperty("apple.laf.useScreenMenuBar", "true");
		Debug.logTimestamp("Starting V1.2");
		UiConstants.initFlags();
		new UiMain().render();
	}
}

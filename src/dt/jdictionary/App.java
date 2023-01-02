package dt.jdictionary;

import dt.jdictionary.ui.UiMain;

public class App
{
	public static void main(String[] args)
	{
		System.setProperty("apple.laf.useScreenMenuBar", "true");
		new UiMain().render();
	}
}

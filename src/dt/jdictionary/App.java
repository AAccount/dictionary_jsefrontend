package dt.jdictionary;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import dt.jdictionary.ui.UiMain;
import dt.util.LogUtils;

// V1.7: 2026-07-27: turn simplified input back into possible traditionals
// V1.6: 2026-07-17: bring back sqlite backend. The in memory db takes forever to start and consumes tons of ram. Also fix some cedict issues.
// V1.5: 2024-07-22: swap sqlite for glorified in memory key-value based db for common code with android
// V1.4: 2024-07-09: import known compound words in hopes of getting better English to Chinese results.
// V1.3: 2024-07-08: make List<SimpleLookup> rank based on past searches if available 
// V1.2: 2024-01-13: add history drop down menu and cache results
// V1.1: 2023-12-26: remove sketchy filtering of species names etc which is not useful
// V1.0: 2022-12-30: initial port of 2021 python gtk ui

public class App
{
	public static final String VERSION = "V1.7";
	
	public static void main(String[] args) throws IOException, ParseException, ClassNotFoundException, SQLException
	{
		System.setProperty("apple.laf.useScreenMenuBar", "true");
		setupLogger();
		final Logger logger = Logger.getLogger(App.class.getName());
		Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
			logger.severe(thread.getName() + " " + LogUtils.printStackTrace(throwable));
		});
		logger.info("Starting " + VERSION);
		
		javax.swing.SwingUtilities.invokeLater(() -> {
			try 
			{
				new UiMain().render();
			} 
			catch (Exception e) 
			{
				logger.severe(LogUtils.printStackTrace(e));
			}
		});
	}

	private static void setupLogger()
	{
		System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT.%1$tL [%4$-7s] %2$s - %5$s%n");
		try 
		{
			final Logger rootLogger = Logger.getLogger("");
			final String tmpDir = System.getProperty("java.io.tmpdir");
			final String logPattern = tmpDir + File.separator + "jdictionary-%g.log";
			final FileHandler fileHandler = new FileHandler(logPattern, 5_000_000, 3, true);
			
			fileHandler.setFormatter(new SimpleFormatter());
			fileHandler.setLevel(Level.ALL);

			rootLogger.addHandler(fileHandler);
			rootLogger.setLevel(Level.INFO);
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
}

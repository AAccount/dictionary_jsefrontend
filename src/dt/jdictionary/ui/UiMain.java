package dt.jdictionary.ui;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;

import dt.cedict.CedictDump;
import dt.cedict.CedictParser;
import dt.jdictionary.App;
import dt.jdictionary.ExceptionPile;
import dt.jdictionary.ProgressListener;
import dt.jdictionary.dbservice.DbService;
import dt.jdictionary.extload.WordBlob;
import dt.jdictionary.extload.WordList;
import dt.jdictionary.ui.UiUtils.Neighbor;
import dt.util.ChineseText;
import dt.util.Debug;

public class UiMain implements ActionListener, ProgressListener
{
	private static final String UI_ROOT = "root";
	private static final String UI_ENTRY = "entry";
	private static final String UI_RESULT = "result";
	private static final String UI_PROGRESS = "progress bar";
	private static final String MENU_SQLITE_INIT = "initalize sqlite";
	private static final String MENU_SQLITE_LOAD_LIST = "load known word list";
	private static final String MENU_SQLITE_LOAD_BLOB = "find words in blob";	

	private static final int UI_ROW_ENTRY = 0;
	private static final int UI_ROW_PROGRESS = 1;
	private static final int UI_ROW_RESULT = 2;
	private static final int UI_MAIN_COLUMN = 0;
	private static final int TOTAL_COLUMNS = 3;
	private static final int HISTORY_MANAGER_MAX = 10;
	private static final String HISTORY_MENU_UI_PREFIX = "kmFU2bYk"; // random string to easily identify history menu items's names
	private static final String JMENU_ITEM_UI_DELIM = ";";
	private static final String FLAG_MENU_UI_PREFIX = "sjkhfca"; // random string to easily identify flag menu items's names

	private DbService db;
	private final JTextField uiEntry;
	private final JProgressBar progressBar;
	private final JButton previous;
	private static final String UI_PREV = "previous button";
	private final JButton forward;
	private static final String UI_FWD = "forward button";
	private final JMenu historyMenu;
	private final JMenu flagMenu;

	private final HistoryManager<String> historyManager;

	public UiMain()
	{
		Debug.logTimestamp("start constructor");
		historyManager = new HistoryManager<>(HISTORY_MANAGER_MAX);

		final int ENTRY_INITIAL_WIDTH = 20;
		uiEntry = new JTextField(ENTRY_INITIAL_WIDTH);
		progressBar = new JProgressBar();
		previous = new JButton();
		forward = new JButton();

		historyMenu = new JMenu("History");
		flagMenu = new JMenu("Flags");
		Debug.logTimestamp("finish constructor");
	}

	public void render() throws ClassNotFoundException, SQLException, IOException, ParseException
	{
		Debug.logTimestamp("start render");
		final JFrame window = new JFrame("Dictionary " + App.VERSION);
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		final JPanel root = new JPanel(new GridBagLayout());
		root.setName(UI_ROOT);
		root.setBorder(UiConstants.TRACER());
		renderEntry(root);
		renderProgressBar(root);
		UiUtils.renderFiller(root, UI_ROW_RESULT);

		window.add(root);
		window.setJMenuBar(renderMenu());
		window.pack();
		window.setVisible(true);
		Debug.logTimestamp("start db");
		
		db = new DbService();
		Debug.logTimestamp("done db");
	}

	private JMenuBar renderMenu()
	{
		final JMenuBar menuBar = new JMenuBar();
		final JMenu sqliteMenu = new JMenu("SQLite");
		sqliteMenu.setMnemonic(KeyEvent.VK_S);
		sqliteMenu.getAccessibleContext().setAccessibleDescription("Modify the underlying sqlite dictionary.");

		final JMenuItem sqliteInit = new JMenuItem("Initalize with CEDICT");
		sqliteInit.setMnemonic(KeyEvent.VK_I);
		sqliteInit.setName(MENU_SQLITE_INIT);
		sqliteInit.addActionListener(this);
		sqliteMenu.add(sqliteInit);
		
		final JMenuItem loadList = new JMenuItem("Load a list of known words to past hits");
		loadList.setMnemonic(KeyEvent.VK_L);
		loadList.setName(MENU_SQLITE_LOAD_LIST);
		loadList.addActionListener(this);
		sqliteMenu.add(loadList);
		
		final JMenuItem loadlBlob = new JMenuItem("Parse a blob of text for compound words to past hits");
		loadlBlob.setMnemonic(KeyEvent.VK_B);
		loadlBlob.setName(MENU_SQLITE_LOAD_BLOB);
		loadlBlob.addActionListener(this);
		sqliteMenu.add(loadlBlob);
		
		historyMenu.setMnemonic(KeyEvent.VK_H);
		historyMenu.getAccessibleContext().setAccessibleDescription("Browse through the last "+HISTORY_MANAGER_MAX+" lookups.");
		
		flagMenu.setMnemonic(KeyEvent.VK_F);
		flagMenu.getAccessibleContext().setAccessibleDescription("Toggle behind the scenes flags.");
		renderFlagMenu();

		menuBar.add(sqliteMenu);
		menuBar.add(historyMenu);
		menuBar.add(flagMenu);
		return menuBar;
	}
	
	private void renderFlagMenu()
	{
		flagMenu.removeAll();
		for(final String flagName : UiConstants.allFlags())
		{
			final String label = (UiConstants.getFlag(flagName) ? "Disable" : "Enable") + " " + flagName;
			final JMenuItem flagItem = new JMenuItem(label);
			
			flagItem.setName(FLAG_MENU_UI_PREFIX + JMENU_ITEM_UI_DELIM + flagName);
			flagItem.addActionListener(this);
			flagMenu.add(flagItem);
		}
	}

	private void renderProgressBar(JPanel root)
	{
		progressBar.setName(UI_PROGRESS);
		progressBar.setBorder(UiConstants.TRACER());
		progressBar.setVisible(false);
		progressBar.setStringPainted(true);
		final GridBagConstraints progressBarConstraints = UiUtils.makeGridConstraint(UI_ROW_PROGRESS, UI_MAIN_COLUMN, true, false, UiUtils.makeInsets(Set.of(Neighbor.TOP, Neighbor.BOTTOM)));
		progressBarConstraints.gridwidth = TOTAL_COLUMNS;
		root.add(progressBar, progressBarConstraints);
	}

	private void renderEntry(JPanel root)
	{
		final int COL_PREV = 0;
		final int COL_FWD = 1;
		final int COL_ENTRY = 2;

		previous.setText("<");
		previous.setName(UI_PREV);
		previous.addActionListener(this);
		previous.setEnabled(false);
		root.add(previous, UiUtils.makeGridConstraint(UI_ROW_ENTRY, COL_PREV, false, false, UiUtils.makeInsets(Set.of(Neighbor.RIGHT))));

		forward.setText(">");
		forward.setName(UI_FWD);
		forward.addActionListener(this);
		forward.setEnabled(false);
		root.add(forward, UiUtils.makeGridConstraint(UI_ROW_ENTRY, COL_FWD, false, false, UiUtils.makeInsets(Set.of(Neighbor.LEFT, Neighbor.RIGHT))));

		uiEntry.setName(UI_ENTRY);
		uiEntry.setFont(UiUtils.makeFont(uiEntry, UiConstants.FONT_MEDIUM));
		uiEntry.setBorder(UiConstants.TRACER());

		uiEntry.addActionListener(this);
		root.add(uiEntry, UiUtils.makeGridConstraint(UI_ROW_ENTRY, COL_ENTRY, true, false, UiUtils.makeInsets(Set.of(Neighbor.LEFT, Neighbor.BOTTOM))));
	}

	@Override
	public void actionPerformed(ActionEvent arg0)
	{

			final JComponent source = (JComponent)arg0.getSource();
			final String sourceName = source.getName();
			switch(sourceName)
			{
				case UI_ENTRY:
					handleTextEntry((JTextField)source, true);
					return;
				case MENU_SQLITE_INIT:
					handleMenuSqliteInit();
					return;
				case MENU_SQLITE_LOAD_LIST:
					handleMenuSqliteLoadList();
					return;
				case MENU_SQLITE_LOAD_BLOB:
					handleMenuSqliteLoadBlob();
					return;
				case UI_PREV:
					handleHistory(historyManager.goBack());
					return;
				case UI_FWD:
					handleHistory(historyManager.goFwd());
					return;
			}

			if(sourceName.substring(0, HISTORY_MENU_UI_PREFIX.length()).equals(HISTORY_MENU_UI_PREFIX))
			{
				final String[] sourceNameParts = sourceName.split(JMENU_ITEM_UI_DELIM);
				final String entry = sourceNameParts[1];
				final int historyIndex = Integer.parseInt(sourceNameParts[2]);
				historyManager.setIndex(historyIndex);
				handleHistory(entry);
			}
			
			if(sourceName.substring(0, FLAG_MENU_UI_PREFIX.length()).equals(FLAG_MENU_UI_PREFIX))
			{
				final String flagName = sourceName.substring(FLAG_MENU_UI_PREFIX.length()+JMENU_ITEM_UI_DELIM.length());
				UiConstants.toggleFlag(flagName);
				renderFlagMenu();
			}
	}

	private void handleHistory(String historicalSearch) 
	{
		toggleHistoryButtons();
		uiEntry.setText(historicalSearch);
		handleTextEntry(uiEntry, false);
	}

	private void toggleHistoryButtons()
	{
		previous.setEnabled(historyManager.canGoBack());
		forward.setEnabled(historyManager.canGoFwd());
	}

	private void handleMenuSqliteInit()
	{
		final JFileChooser fc = new JFileChooser();
		final int returnVal = fc.showOpenDialog(null);
		if (returnVal != JFileChooser.APPROVE_OPTION) 
		{
			return;
		}

		final File file = fc.getSelectedFile();
		disableEntry("Importing " + file.getName());

		final Thread importer = new Thread(() -> { 
			
			try
			{
				final CedictDump dump = new CedictParser(this).parse(file);
				db.saveCedictDump(dump, this);
			}
			catch(Exception e)
			{
				printException(e);
			}
			enableEntry();
		});
		importer.start();	
	}
	
	private void handleMenuSqliteLoadList()
	{
		final JFileChooser fc = new JFileChooser();
		final int returnVal = fc.showOpenDialog(null);
		if (returnVal != JFileChooser.APPROVE_OPTION) 
		{
			return;
		}

		final File file = fc.getSelectedFile();
		disableEntry("Loading past known words from: " + file.getName());
		final Thread loader = new Thread(() -> {
			try
			{
				final List<String> wordList = new WordList(this).parse(file);
				final boolean verifyInDictionary = true;
				db.savePastHits(wordList, verifyInDictionary);
			}
			catch(Exception e)
			{
				printException(e);
			}
			enableEntry();
		});
		loader.start();	
	}
	
	private void handleMenuSqliteLoadBlob()
	{
		final JFileChooser fc = new JFileChooser();
		final int returnVal = fc.showOpenDialog(null);
		if (returnVal != JFileChooser.APPROVE_OPTION) 
		{
			return;
		}

		final File file = fc.getSelectedFile();
		disableEntry("Loading known words from blob: " + file.getName());
		final Thread loader = new Thread(() -> {
			try
			{
				final List<String> sentences = new WordBlob(this).parse(file);
				final List<String> wordList = db.extractCompoundWords(sentences);
				final boolean verifyInDictionary = false;
				db.savePastHits(wordList, verifyInDictionary);
			}
			catch(ExceptionPile p)
			{
				printFirstExceptionOfPile(p);
			}
			catch(Exception e)
			{
				printException(e);
			}
			enableEntry();
		});
		loader.start();	
	}
	
	private void disableEntry(String message)
	{
		uiEntry.setEditable(false);
		uiEntry.setText(message);
		progressBar.setVisible(true);
	}
	
	private void enableEntry()
	{
		progressBar.setVisible(false);
		uiEntry.setText("");
		uiEntry.setEditable(true);
	}

	private void handleTextEntry(JTextField entry, boolean newSearch)
	{
		final JPanel root = (JPanel)entry.getParent();
		final String received = entry.getText().trim().toLowerCase();
		Debug.logTimestamp("Input trimmed, to lower case: " + received);

		UiUtils.removeNamedComponents(root, Set.of(UI_RESULT, UiUtils.UI_FILLER));
		Debug.logTimestamp("removed ui filler");
		
		try
		{
			final boolean shouldSave = newSearch && UiConstants.getFlag(UiConstants.FLAG_SAVE_HITS);
			final JComponent result = ChineseText.hasChinese(received) ? 
					new UiChineseLookup().render(db.lookupChinese(UiConstants.getFlag(UiConstants.FLAG_AUTOSWAP) ? ChineseText.autoSwapChinese(received) : received, shouldSave)) : 
					new UiEnglishLookup().render(db.lookupEnglish(received));
			result.setName(UI_RESULT);
			result.setBorder(UiConstants.TRACER());
	
			final GridBagConstraints resultConstraints = UiUtils.makeGridConstraint(UI_ROW_RESULT, UI_MAIN_COLUMN, true, true, UiUtils.makeInsets(Set.of(Neighbor.TOP)));
			resultConstraints.gridwidth = TOTAL_COLUMNS;
			root.add(result, resultConstraints);
		}
		catch(ExceptionPile e)
		{
			printFirstExceptionOfPile(e);
		}
		catch(Exception e)
		{
			printException(e);
			e.printStackTrace();
		}
		if(newSearch)
		{
			historyManager.addSingleEntry(received);
		}
		toggleHistoryButtons();
		renderHistoryMenu();

		root.revalidate();
		root.repaint();
	}

	private void renderHistoryMenu()
	{
		historyMenu.removeAll();

		final List<String> historicalLookups = historyManager.getCompleteHistoryReadonly();
		int counter = 0;
		for(final String historicalLookup : historicalLookups)
		{
			final JMenuItem historyItem = new JMenuItem(counter+": " + historicalLookup);
			historyItem.setMnemonic(KeyEvent.VK_0 + counter);
			historyItem.setName(HISTORY_MENU_UI_PREFIX + JMENU_ITEM_UI_DELIM + historicalLookup + JMENU_ITEM_UI_DELIM + counter);
			historyItem.addActionListener(this);
			historyMenu.add(historyItem);
			counter++;
		}
	}

	private void printException(Exception e)
	{
		System.out.print(e);
		final String title = e.getClass().getName();
		final String errorMessage = e.getMessage();
		final String stackTrace = Debug.printStackTrace(e.getCause().getStackTrace());
		final String popupMessage = errorMessage + "\n" + stackTrace;

		JOptionPane.showMessageDialog(null, popupMessage, title, JOptionPane.ERROR_MESSAGE);
		System.err.println(Debug.printStackTrace(e.getCause().getStackTrace()));
	}

	private void printFirstExceptionOfPile(ExceptionPile pile)
	{
		final String title = String.format("First of %s exceptions", pile.getExceptions().size());
		final String errorMessage = pile.getMessage();
		final String stackTrace = Debug.printStackTrace(pile.getExceptions().get(0).getStackTrace());
		final String popupMessage = errorMessage + "\n" + stackTrace;

		JOptionPane.showMessageDialog(null, popupMessage, title, JOptionPane.ERROR_MESSAGE);
		pile.getExceptions().forEach(e -> System.err.println(Debug.printStackTrace(e.getCause().getStackTrace())));
	}

	private void updateImportProgress(String description, long current, long max)
	{
		if(current == 0)
		{
			progressBar.setValue(0);
			progressBar.setMaximum(100);
		}
		
		final int percentage = (int)(current*100/max);
		progressBar.setValue(percentage);
		progressBar.setString(description + " " + percentage + "%");
	}

	@Override
	public void onFractionalProgress(String description, long processed, long total)
	{
		updateImportProgress(description, processed, total);		
	}
}

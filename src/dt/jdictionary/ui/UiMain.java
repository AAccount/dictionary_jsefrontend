package dt.jdictionary.ui;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

import dt.jdictionary.FullLookup;
import dt.jdictionary.SimpleLookup;
import dt.jdictionary.Utils;
import dt.jdictionary.cedict.CedictDump;
import dt.jdictionary.cedict.CedictParser;
import dt.jdictionary.events.Event;
import dt.jdictionary.events.EventDispatcher;
import dt.jdictionary.events.EventListener;
import dt.jdictionary.events.EventUtils;
import dt.jdictionary.sqlite.DbEvent;
import dt.jdictionary.sqlite.dbservice.DbService;
import dt.jdictionary.ui.UiUtils.Neighbor;

public class UiMain implements ActionListener, EventListener
{
	private final String UI_ROOT = "root";
	private final String UI_ENTRY = "entry";
	private final String UI_RESULT = "result";
	private final String UI_PROGRESS = "progress bar";
	private final String MENU_INIT_SQLITE = "initalize sqlite";

	private final int UI_ROW_ENTRY = 0;
	private final int UI_ROW_PROGRESS = 1;
	private final int UI_ROW_RESULT = 2;
	private final int UI_MAIN_COLUMN = 0;
	private final int TOTAL_COLUMNS = 3;
	private final int HISTORY_MANAGER_MAX = 10;
	private final String HISTORY_MENU_UI_PREFIX = "kmFU2bYk"; // random string to easily identify history menu items's names
	private final String HISTORY_MENU_UI_DELIM = ";";

	private final DbService db;
	private final JTextField uiEntry;
	private final JProgressBar progressBar;
	private final JButton previous;
	private final String UI_PREV = "previous button";
	private final JButton forward;
	private final String UI_FWD = "forward button";
	private final JMenu historyMenu;

	private final HistoryManager<String> historyManager;

	public UiMain()
	{
		historyManager = new HistoryManager<>(HISTORY_MANAGER_MAX);
		db = new DbService();
		EventDispatcher.get().register(this);

		final int ENTRY_INITIAL_WIDTH = 20;
		uiEntry = new JTextField(ENTRY_INITIAL_WIDTH);
		progressBar = new JProgressBar();
		previous = new JButton();
		forward = new JButton();

		historyMenu = new JMenu("History");
	}

	public void render()
	{
		final JFrame window = new JFrame("Dictionary");
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		final JPanel root = new JPanel(new GridBagLayout());
		root.setName(UI_ROOT);
		root.setBorder(UiConstants.TRACER);
		renderEntry(root);
		renderProgressBar(root);
		UiUtils.renderFiller(root, UI_ROW_RESULT);

		window.add(root);
		window.setJMenuBar(renderMenu());
		window.pack();
		window.setVisible(true);
	}

	private JMenuBar renderMenu()
	{
		final JMenuBar menuBar = new JMenuBar();
		final JMenu sqliteMenu = new JMenu("SQLite");
		sqliteMenu.setMnemonic(KeyEvent.VK_S);
		sqliteMenu.getAccessibleContext().setAccessibleDescription("Modify the underlying sqlite dictionary.");

		final JMenuItem sqliteInit = new JMenuItem("Initalize with CEDICT");
		sqliteInit.setMnemonic(KeyEvent.VK_I);
		sqliteInit.setName(MENU_INIT_SQLITE);
		sqliteInit.addActionListener(this);
		sqliteMenu.add(sqliteInit);
		
		historyMenu.setMnemonic(KeyEvent.VK_H);
		historyMenu.getAccessibleContext().setAccessibleDescription("Browse through the last "+HISTORY_MANAGER_MAX+" lookups.");

		menuBar.add(sqliteMenu);
		menuBar.add(historyMenu);
		return menuBar;
	}

	private void renderProgressBar(JPanel root)
	{
		progressBar.setName(UI_PROGRESS);
		progressBar.setBorder(UiConstants.TRACER);
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
		uiEntry.setBorder(UiConstants.TRACER);

		uiEntry.addActionListener(this);
		root.add(uiEntry, UiUtils.makeGridConstraint(UI_ROW_ENTRY, COL_ENTRY, true, false, UiUtils.makeInsets(Set.of(Neighbor.LEFT, Neighbor.BOTTOM))));
	}

	@Override
	public void actionPerformed(ActionEvent arg0)
	{
		try
		{
			final JComponent source = (JComponent)arg0.getSource();
			final String sourceName = source.getName();
			switch(sourceName)
			{
				case UI_ENTRY:
					handleTextEntry((JTextField)source, true);
					return;
				case MENU_INIT_SQLITE:
					handleMenuSqliteInit();
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
				final String[] sourceNameParts = sourceName.split(HISTORY_MENU_UI_DELIM);
				final String entry = sourceNameParts[1];
				final int historyIndex = Integer.parseInt(sourceNameParts[2]);
				historyManager.setIndex(historyIndex);
				handleHistory(entry);
			}
		}
		catch(Exception e)
		{
			EventUtils.sendError(e);
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
		uiEntry.setEditable(false);
		uiEntry.setText("Importing " + file.getName());
		progressBar.setVisible(true);

		final Thread importer = new Thread(() -> { 
			final CedictDump dump = new CedictParser().parse(file);
			db.saveCedictDump(dump);

			progressBar.setVisible(false);
			uiEntry.setText("");
			uiEntry.setEditable(true);
		});
		importer.start();		
	}

	private void handleTextEntry(JTextField entry, boolean newSearch)
	{
		final JPanel root = (JPanel)entry.getParent();
		final String received = entry.getText().trim().toLowerCase();
		Utils.logTimestamp("Input trimmed, to lower case: " + received);

		UiUtils.removeNamedComponents(root, Set.of(UI_RESULT, UiUtils.UI_FILLER));

		final JComponent result = Utils.hasChinese(received) ? renderChineseLookup(received) : new UiList().render(db.lookupEnglish(received));
		result.setName(UI_RESULT);
		result.setBorder(UiConstants.TRACER);

		final GridBagConstraints resultConstraints = UiUtils.makeGridConstraint(UI_ROW_RESULT, UI_MAIN_COLUMN, true, true, UiUtils.makeInsets(Set.of(Neighbor.TOP)));
		resultConstraints.gridwidth = TOTAL_COLUMNS;
		root.add(result, resultConstraints);

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
			final JMenuItem testItem = new JMenuItem(counter+": " + historicalLookup);
			testItem.setMnemonic(KeyEvent.VK_0 + counter);
			testItem.setName(HISTORY_MENU_UI_PREFIX + HISTORY_MENU_UI_DELIM + historicalLookup + HISTORY_MENU_UI_DELIM + counter);
			testItem.addActionListener(this);
			historyMenu.add(testItem);
			counter++;
		}
	}

	private JComponent renderChineseLookup(String chinese)
	{
		final FullLookup directResults =  db.lookupChinese(chinese);		
		final Map<String, List<SimpleLookup>> supplementaries = new LinkedHashMap<>(); // linked hash map for predictable iteration order
		supplementaries.put("Same Front", db.lookupSameFront(chinese));
		supplementaries.put("Same Back", db.lookupSameBack(chinese));
		supplementaries.put("~4 Char Saying", db.try4CharLookup(chinese));
		supplementaries.put("Deinterlace", db.tryDeinterlace(chinese));
		supplementaries.put("Typo", db.tryTypoMatch(chinese));
		supplementaries.put("Substring", db.trySubstringMatch(chinese));

		return new UiChineseLookup().render(directResults, supplementaries);
	}

	@Override
	public void onEvent(Event event) 
	{
		switch(event.getType())
		{
			case CEDICT_PARSE:
				handleCedictEvent(event.getData());
				break;
			case DB_SAVE:
				handleDbSaveEvent(event.getData());
				break;
			case JAVA_EXCEPTION:
				printException(event.getData());
				break;
			case SELF_WARNING:
				printWarning(event.getData());
				break;
			default:
				JOptionPane.showMessageDialog(null, event.toString(), "Unknown Event", JOptionPane.WARNING_MESSAGE);
				break;
		}
	}

	private void printWarning(Map<String, Object> data)
	{
		final String warning = (String)data.get(EventUtils.EVENT_WARN_MSG);
		final String stackTrace = (String)data.get(EventUtils.EVENT_STACK_TRACE);
		final String popupMessage = warning + "\n" + stackTrace;

		JOptionPane.showMessageDialog(null, popupMessage, "Warning", JOptionPane.WARNING_MESSAGE);
	}

	private void printException(Map<String, Object> data)
	{
		final String title = (String)data.get(EventUtils.EVENT_ERR_CLASS);
		final String errorMessage = (String)data.get(EventUtils.EVENT_ERR_MSG);
		final String stackTrace = (String)data.get(EventUtils.EVENT_STACK_TRACE);
		final String popupMessage = errorMessage + "\n" + stackTrace;

		JOptionPane.showMessageDialog(null, popupMessage, title, JOptionPane.ERROR_MESSAGE);
	}

	private void handleCedictEvent(Map<String, Object> data)
	{
		final long processedBytes = (long)data.get(CedictParser.EVENT_PROCESSED_BYTES);
		final long totalBytes = (long)data.get(CedictParser.EVENT_TOTAL_BYTES);
		updateImportProgress((int)processedBytes, (int)totalBytes, "Parse CEDICT: ");
	}

	private void handleDbSaveEvent(Map<String, Object> data)
	{
		final int trxSofar = (int)data.get(DbEvent.EVENT_TRX_SOFAR);
		final int trxTotal = (int)data.get(DbEvent.EVENT_TRX_TOTAL);
		updateImportProgress(trxSofar, trxTotal, "Db Transactions: ");
	}

	private void updateImportProgress(int current, int max, String reason)
	{
		if(current == 0)
		{
			progressBar.setValue(0);
			progressBar.setMaximum(max);
		}
		progressBar.setValue(current);
		progressBar.setString(reason +  (int)(progressBar.getPercentComplete()*100) + "%");
	}
}

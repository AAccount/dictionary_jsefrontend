package dt.jdictionary.ui;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.awt.Component;

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
import dt.jdictionary.sqlite.DbService;
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
	private final int UI_SINGLE_COLUMN = 0;

	private final DbService db;
	private final JTextField uiEntry;
	private final JProgressBar progressBar;

	public UiMain()
	{
		db = new DbService();
		EventDispatcher.get().register(this);

		final int ENTRY_INITIAL_WIDTH = 20;
		uiEntry = new JTextField(ENTRY_INITIAL_WIDTH);
		progressBar = new JProgressBar();
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
		
		menuBar.add(sqliteMenu);
		return menuBar;
	}

	private void renderProgressBar(JPanel root)
	{
		progressBar.setName(UI_PROGRESS);
		progressBar.setBorder(UiConstants.TRACER);
		progressBar.setVisible(false);
		progressBar.setStringPainted(true);
		root.add(progressBar, uiMainConstraints(UI_ROW_PROGRESS, UI_SINGLE_COLUMN, false, UiUtils.makeInsets(Set.of(Neighbor.TOP, Neighbor.BOTTOM))));
	}

	private void renderEntry(JPanel root)
	{
		uiEntry.setName(UI_ENTRY);
		uiEntry.setFont(UiUtils.makeFont(uiEntry, UiConstants.FONT_MEDIUM));
		uiEntry.setBorder(UiConstants.TRACER);

		uiEntry.addActionListener(this);
		root.add(uiEntry, uiMainConstraints(UI_ROW_ENTRY, UI_SINGLE_COLUMN, false, UiUtils.makeInsets(Set.of(Neighbor.BOTTOM))));
	}

	// Trial and error special constraints for the "main" program window
	private GridBagConstraints uiMainConstraints(int row, int column, boolean expandy, Insets insets)
	{
		final GridBagConstraints entryConstraints = new GridBagConstraints();
		entryConstraints.gridx = column;
		entryConstraints.gridy = row;
		entryConstraints.weightx = UiConstants.GRIDBAG_AUTOEXPAND;
		entryConstraints.weighty = expandy ? UiConstants.GRIDBAG_AUTOEXPAND : UiConstants.GRIDBAG_NO_AUTOEXPAND;
		entryConstraints.anchor = GridBagConstraints.NORTH;
		entryConstraints.fill = expandy ? GridBagConstraints.BOTH : GridBagConstraints.HORIZONTAL;
		entryConstraints.insets = insets;
		return entryConstraints;
	}

	@Override
	public void actionPerformed(ActionEvent arg0)
	{
		final JComponent source = (JComponent)arg0.getSource();
		switch(source.getName())
		{
			case UI_ENTRY:
				handleTextEntry((JTextField)source);
				break;
			case MENU_INIT_SQLITE:
				handleMenuSqliteInit();
				break;
		}
	}

	private void handleMenuSqliteInit()
	{
		final JFileChooser fc = new JFileChooser();
		final int returnVal = fc.showOpenDialog(null);
		if (returnVal == JFileChooser.APPROVE_OPTION) 
		{
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
		else 
		{
			System.out.println("Open command cancelled by user.");
		}
	}

	private void handleTextEntry(JTextField entry)
	{
		final JPanel root = (JPanel)entry.getParent();
		final String received = entry.getText().trim().toLowerCase();
		System.out.println("Input trimmed, to lower case: " + received);

		final Component[] uiElements = (Component[])root.getComponents();
		for(final Component uiElement : uiElements)
		{
			if(uiElement.getName().equals(UI_RESULT) || uiElement.getName().equals(UiUtils.UI_FILLER))
			{
				root.remove(uiElement);
				break;
			}
		}

		final JComponent result = Utils.hasChinese(received) ?
		 renderChineseLookup(received) :
		 new UiList().render(db.lookupEnglish(received));

		result.setName(UI_RESULT);
		result.setBorder(UiConstants.TRACER);
		root.add(result, uiMainConstraints(UI_ROW_RESULT, UI_SINGLE_COLUMN, true, UiUtils.makeInsets(Set.of(Neighbor.TOP))));

		root.revalidate();
		root.repaint();
	}

	private JComponent renderChineseLookup(String chinese)
	{
		final FullLookup directResults =  db.lookupChinese(chinese);
		final boolean hasDirectResults = directResults.getResults().size() > 0;
		
		final boolean shouldTry4Chars = !hasDirectResults && chinese.length() >= DbService.MIN_4CHARS_SUBSTRING;
		final List<SimpleLookup> fourCharResults = shouldTry4Chars ? db.try4CharLookup(chinese) : List.of();
		final boolean has4CharResults = fourCharResults.size() > 0;

		// Last ditch probably sketchy results. Only do this if other attempts failed.
		final boolean shouldTryTypo = !hasDirectResults && !has4CharResults;
		final List<SimpleLookup> typoResults = shouldTryTypo ? db.tryTypoMatch(chinese) : List.of();
		final boolean hasTypoResults = typoResults.size() > 0;
		
		// If there are no alternative results, maybe the ui single char's related word tabs may be useful?
		if(hasDirectResults || (!hasDirectResults && !has4CharResults && !hasTypoResults))
		{
			return new UiSingleChar().render(directResults, db.lookupSameFront(chinese), db.lookupSameBack(chinese));
		}
		else if(has4CharResults)
		{
			return new UiList().render(fourCharResults);
		}
		else
		{
			return new UiList().render(typoResults);
		}
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
			default:
				JOptionPane.showMessageDialog(null, event.toString(), "Unknown Event", JOptionPane.WARNING_MESSAGE);
				break;
		}
	}

	private void printException(Map<String, Object> data)
	{
		final String title = (String)data.get(EventUtils.EVENT_ERR_CLASS);
		final String errorMessage = (String)data.get(EventUtils.EVENT_ERR_MSG);
		final String stackTrace = (String)data.get(EventUtils.EVENT_STACK_TRACE);
		final String message = errorMessage + "\n" + stackTrace;

		JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
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

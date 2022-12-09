package dt.jdictionary.ui;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Set;
import java.awt.Component;

import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTextField;

import dt.jdictionary.Utils;
import dt.jdictionary.cedict.CedictDump;
import dt.jdictionary.cedict.CedictParser;
import dt.jdictionary.sqlite.DbService;
import dt.jdictionary.ui.UiUtils.Neighbor;

public class UiMain implements ActionListener
{
	private final String UI_ROOT = "root";
	private final String UI_ENTRY = "entry";
	private final String UI_RESULT = "result";
	private final String MENU_INIT_SQLITE = "initalize sqlite";

	private final int UI_ROW_ENTRY = 0;
	private final int UI_ROW_RESULT = 1;
	private final int UI_SINGLE_COLUMN = 0;

	private final DbService db;

	public UiMain()
	{
		db = new DbService();
	}

	public void render()
	{
		final JFrame window = new JFrame("Dictionary");
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		final JPanel root = new JPanel(new GridBagLayout());
		root.setName(UI_ROOT);
		root.setBorder(UiConstants.TRACER);
		renderEntry(root);
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

	private void renderEntry(JPanel root)
	{
		final int ENTRY_INITIAL_WIDTH = 20;
		final JTextField entry = new JTextField(ENTRY_INITIAL_WIDTH);
		entry.setName(UI_ENTRY);
		entry.setFont(UiUtils.makeFont(entry, UiConstants.FONT_MEDIUM));
		entry.setBorder(UiConstants.TRACER);

		entry.addActionListener(this);
		root.add(entry, uiMainConstraints(UI_ROW_ENTRY, UI_SINGLE_COLUMN, false, UiUtils.makeInsets(Set.of(Neighbor.BOTTOM))));
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
			System.out.println("Using: " + file.getName());
			final CedictDump dump = new CedictParser().parse(file);
			db.saveCedictDump(dump);
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
		 new UiSingleChar().render(db.lookupChinese(received), db.lookupSameFront(received), db.lookupSameBack(received)) :
		 new UiList().render(db.lookupEnglish(received));

		result.setName(UI_RESULT);
		result.setBorder(UiConstants.TRACER);
		root.add(result, uiMainConstraints(UI_ROW_RESULT, UI_SINGLE_COLUMN, true, UiUtils.makeInsets(Set.of(Neighbor.TOP))));

		root.revalidate();
		root.repaint();
	}
}

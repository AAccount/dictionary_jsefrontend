package dt.jdictionary;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Component;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;

import dt.jdictionary.sqlite.DbService;
import dt.jdictionary.ui.UiConstants;
import dt.jdictionary.ui.UiList;
import dt.jdictionary.ui.UiSingleChar;
import dt.jdictionary.ui.UiUtils;

public class App implements ActionListener
{
	public static void main(String[] args)
	{
		new App().launchUI();
	}

	private final String UI_ROOT = "root";
	private final String UI_ENTRY = "entry";
	private final String UI_RESULT = "result";
	private final DbService db;

	public App()
	{
		db = new DbService();
	}

	public void launchUI()
	{
		final JFrame window = new JFrame("Dictionary");
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		final JPanel root = new JPanel(new GridBagLayout());
		root.setName(UI_ROOT);

		renderEntry(root);

		window.add(root);
		window.pack();
		window.setVisible(true);
	}

	private void renderEntry(JPanel root)
	{
		final JTextField entry = new JTextField(20);
		entry.setName(UI_ENTRY);
		entry.setFont(UiUtils.generateFont(entry, UiConstants.FONT_MEDIUM));
		entry.setBorder(UiConstants.TRACER);

		entry.addActionListener(this);
		root.add(entry, generateNorthAnchConstraints(0, 0, false, new Insets(10, 10, 5, 10)));
	}

	// Trial and error special constraints for the "main" program window
	private GridBagConstraints generateNorthAnchConstraints(int row, int column, boolean expandy, Insets insets)
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
		final JTextField entry = (JTextField)arg0.getSource();
		final JPanel root = (JPanel)entry.getParent();
		final String received = entry.getText().trim().toLowerCase();
		System.out.println("Input trimmed, to lower case: " + received);

		final Component[] uiElements = (Component[])root.getComponents();
		for(final Component uiElement : uiElements)
		{
			if(uiElement.getName().equals(UI_RESULT))
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
		root.add(result, generateNorthAnchConstraints(1, 0, true, new Insets(5, 10, 10, 10)));

		root.revalidate();
		root.repaint();
	}
}

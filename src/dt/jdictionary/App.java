package dt.jdictionary;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.GridBagConstraints;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Component;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;

import dt.jdictionary.ui.UiConstants;
import dt.jdictionary.ui.UiSingleChar;
import dt.jdictionary.ui.UiUtils;

public class App implements ActionListener
{
	public static void main(String[] args) throws Exception 
	{
		new App().launchUI();
	}

	private final String UI_ROOT = "root";
	private final String UI_ENTRY = "entry";
	private final String UI_RESULT = "result";

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

		final GridBagConstraints entryConstraints = new GridBagConstraints();
		entry.setBorder(UiConstants.TRACER);
		entryConstraints.gridx = 0;
		entryConstraints.gridy = 0;
		entryConstraints.weightx = 1;
		entryConstraints.weighty = .1;
		entryConstraints.anchor = GridBagConstraints.NORTH;
		entryConstraints.fill = GridBagConstraints.HORIZONTAL;
		entryConstraints.insets = new Insets(10, 10, 5, 10);
		entry.addActionListener(this);
		root.add(entry, entryConstraints);
	}

	@Override
	public void actionPerformed(ActionEvent arg0)
	{
		System.out.println("asdf");
		final JTextField entry = (JTextField)arg0.getSource();
		final JPanel root = (JPanel)entry.getParent();
		final String received = entry.getText().trim();
		System.out.println(received);

		final GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = 1;
		constraints.weightx = 1;
		constraints.weighty = 1;
		constraints.anchor = GridBagConstraints.NORTH;
		constraints.fill = GridBagConstraints.BOTH;
		constraints.insets = new Insets(10, 10, 10, 10);

		final Component[] uiElements = (Component[])root.getComponents();
		for(final Component uiElement : uiElements)
		{
			if(uiElement.getName().equals(UI_RESULT))
			{
				root.remove(uiElement);
				break;
			}
		}

		final JComponent result = new UiSingleChar().render(getPlaceholder(received), getRelatedPlacholder(), getRelatedPlacholder());
		result.setName(UI_RESULT);
		root.add(result, constraints);

		root.revalidate();
		root.repaint();
	}

	private ZhLookup getPlaceholder(String checkText)
	{
		List<String> definitions = new ArrayList<String>();
		definitions.add("definition 1");
		definitions.add("definition 2");
		definitions.add("definition 3");
		definitions.add("definition 4");
		definitions.add("definition 5");
		definitions.add("variant of");

		Map<String, List<String>> results = new HashMap<>();
		results.put("pinyin", definitions);
		List<String> measureWords = new ArrayList<>();
		measureWords.add("1");
		measureWords.add("2");
		ZhLookup placeholder = new ZhLookup("漢字", results, checkText, measureWords);
		return placeholder;
	}


	private List<SimpleLookup> getRelatedPlacholder()
	{
		final List<String> definitions = new ArrayList<String>();
		definitions.add("variant of");
		definitions.add("def 2");
		final SimpleLookup result1 = new SimpleLookup("阿", "a", definitions);
		final SimpleLookup result2 = new SimpleLookup("為", "wei", definitions);
		
		final List<SimpleLookup> result = new ArrayList<>();
		result.add(result1);
		result.add(result2);
		return result;
	}
}

package dt.jdictionary;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import dt.jdictionary.ui.UiConstants;
import dt.jdictionary.ui.UiSingleChar;
import dt.jdictionary.ui.UiUtils;

public class App
{
	public static void main(String[] args) throws Exception 
	{
		launchUI();
	}

	private static void launchUI()
	{
		final JFrame window = new JFrame("Dictionary");
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		final JPanel root = new JPanel(new GridBagLayout());

		generateEntry(root, window);
		

		window.add(root);
		window.pack();
		window.setVisible(true);
	}

	private static void generateEntry(JPanel root, JFrame window)
	{
		final JTextField entry = new JTextField(20);
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

		Action action = new AbstractAction()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
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

				JComponent result = new UiSingleChar().render(getPlaceholder(received), getRelatedPlacholder(), getRelatedPlacholder());
				root.add(result, constraints);

				root.revalidate();
				root.repaint();
				window.revalidate();
				window.repaint();
			}
		};
		entry.addActionListener(action);
		root.add(entry, entryConstraints);
	}

	private static ZhLookup getPlaceholder(String checkText)
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


	private static List<SimpleLookup> getRelatedPlacholder()
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

package dt.jdictionary.ui;

import java.util.List;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JTabbedPane;

import dt.jdictionary.dbrepo.DictionaryEntry;


public class UiEnglishLookup
{
	public JComponent render(Map<String, List<DictionaryEntry>> useableCombinations)
	{
		final JTabbedPane notebook = new JTabbedPane();
		notebook.setBorder(UiConstants.TRACER());
		useableCombinations.forEach((combo, results) -> {
			notebook.add(combo, new UiList(combo).render(results));
		});
		return notebook;
	}
}

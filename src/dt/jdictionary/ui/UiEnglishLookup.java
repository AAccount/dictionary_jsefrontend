package dt.jdictionary.ui;

import java.awt.Component;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.swing.JComponent;
import javax.swing.JTabbedPane;

import dt.jdictionary.ChineseSummaryLookup;


public class UiEnglishLookup
{
	public JComponent render(Map<String, List<ChineseSummaryLookup>> useableCombinations)
	{
		final JTabbedPane notebook = new JTabbedPane();
		notebook.setBorder(UiConstants.TRACER());

		

		final Map<String, CompletableFuture<Component>> tabFutures= new HashMap<>();
		useableCombinations.keySet().forEach(combo -> tabFutures.put(combo, CompletableFuture.supplyAsync(() -> {return new UiList().render(useableCombinations.get(combo));})));
		tabFutures.values().forEach(CompletableFuture::join);
		useableCombinations.keySet().forEach(useable -> notebook.addTab(useable, tabFutures.get(useable).join()));;
		return notebook;
	}
}

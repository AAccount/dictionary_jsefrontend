package dt.jdictionary.ui;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import javax.swing.JComponent;
import javax.swing.JTabbedPane;

import dt.jdictionary.ChineseSummaryLookup;


public class UiEnglishLookup
{
	public JComponent render(Map<String, List<ChineseSummaryLookup>> useableCombinations, ExecutorService executor)
	{
		final JTabbedPane notebook = new JTabbedPane();
		notebook.setBorder(UiConstants.TRACER());

		final Map<String, CompletableFuture<JComponent>> tabFutures = new LinkedHashMap<>();
		for(final String englishCombo : useableCombinations.keySet())
		{
			final List<ChineseSummaryLookup> summaries = useableCombinations.get(englishCombo);
			final CompletableFuture<JComponent> future = CompletableFuture.supplyAsync(() -> {return new UiList().render(summaries);}, executor);
			tabFutures.put(englishCombo, future);
		}

		final CompletableFuture<Void> allFinished = CompletableFuture.allOf(tabFutures.values().toArray(new CompletableFuture[0]));
		allFinished.join();

		for(final String englishCombo : tabFutures.keySet())
		{
			final JComponent tab = tabFutures.get(englishCombo).join();
			notebook.addTab(englishCombo, tab);
		}
		return notebook;
	}
}

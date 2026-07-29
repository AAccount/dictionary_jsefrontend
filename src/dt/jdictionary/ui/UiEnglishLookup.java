package dt.jdictionary.ui;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

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
			final CompletableFuture<JComponent> future = CompletableFuture.supplyAsync(() -> {return new UiList(englishCombo).render(summaries);}, executor);
			tabFutures.put(englishCombo, future);
		}

		CompletableFuture.allOf(tabFutures.values().toArray(new CompletableFuture[0]))
			.thenRun(() -> {
				SwingUtilities.invokeLater(() -> {
					tabFutures.forEach((combo, future) -> {notebook.addTab(combo, future.join());});
				});
			});
		return notebook;
	}
}

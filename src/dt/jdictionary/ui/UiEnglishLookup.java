package dt.jdictionary.ui;

import java.awt.Component;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.swing.JComponent;
import javax.swing.JTabbedPane;

import dt.jdictionary.SimpleLookup;
import dt.jdictionary.util.GenerateCombinations;


public class UiEnglishLookup
{
	public JComponent render(Map<String, List<SimpleLookup>> individualDefinitions)
	{
		final JTabbedPane notebook = new JTabbedPane();
		notebook.setBorder(UiConstants.TRACER());

		final Map<String, List<SimpleLookup>> useableCombinations = findUseableCombinations(individualDefinitions);
		final Map<String, CompletableFuture<Component>> tabFutures= new HashMap<>();
		useableCombinations.keySet().forEach(combo -> tabFutures.put(combo, CompletableFuture.supplyAsync(() -> {return new UiList().render(useableCombinations.get(combo));})));
		tabFutures.values().forEach(CompletableFuture::join);
		useableCombinations.keySet().forEach(useable -> notebook.addTab(useable, tabFutures.get(useable).join()));;
		return notebook;
	}
	
	private Map<String, List<SimpleLookup>> findUseableCombinations(Map<String, List<SimpleLookup>> individualDefinitions)
	{
		final List<List<String>> combinations = GenerateCombinations.generateCombinations(List.copyOf(individualDefinitions.keySet()));
		final Map<String, List<SimpleLookup>> result = new HashMap<String, List<SimpleLookup>>();
		for(final List<String> combination : combinations)
		{
			final List<SimpleLookup> combinedLookup = getQualifyingEntries(individualDefinitions, combination);
			if(!combinedLookup.isEmpty())
			{
				result.put(combination.toString(), combinedLookup);
			}
		}
		return result;
	}
	
	private List<SimpleLookup> getQualifyingEntries(Map<String, List<SimpleLookup>> individualDefinitions, List<String> combination)
	{
		if(combination.size() == 1)
		{
			return individualDefinitions.get(combination.get(0));
		}
		
		final List<SimpleLookup> result = new ArrayList<>(individualDefinitions.get(combination.get(0)));
		for(final String word : combination.subList(1, combination.size()))
		{
			final List<SimpleLookup> wordEntries = individualDefinitions.get(word);
			result.retainAll(wordEntries);
		}
		return result;
	}

}

package dt.jdictionary.ui;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.awt.GridBagLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;

import dt.jdictionary.ChineseDefinitionLookup;
import dt.jdictionary.ChineseSummaryLookup;
import dt.jdictionary.ExhaustiveChineseLookup;
import dt.jdictionary.dbservice.alternative.SubstringSearch;
import dt.jdictionary.ui.UiUtils.Neighbor;
import dt.util.ChineseText;
import dt.util.Debug;


class UiChineseLookup
{
	private final String DEFINITION_TAB = "Definition";
	private final int COL_LABEL = 0;
	private final int COL_VALUE = 1;
	private final int ROW_BIG_CHAR = 0;
	private final int ROW_START_DEFINITIONS = 1;

	public UiChineseLookup() {}
	
	public JComponent render(ExhaustiveChineseLookup dictionaryResult)
	{
		// Return the raw notebook. Don't prepackage it in a panel.
		final JTabbedPane notebook = new JTabbedPane();
		final Map<String, CompletableFuture<Component>> tabFutures= new HashMap<>();
	
		notebook.setBorder(UiConstants.TRACER());

		tabFutures.put(DEFINITION_TAB, CompletableFuture.supplyAsync(() -> {return this.renderZhDefinition(dictionaryResult.getDefinition());}));
		
		final Map<String, List<ChineseSummaryLookup>> supplementaries = dictionaryResult.getSupplementaries();
		supplementaries.keySet().stream()
			.filter(supplementary -> !supplementaries.get(supplementary).isEmpty())
			.forEach(supplementary -> tabFutures.put(supplementary, tabCompletable(supplementary, supplementaries.get(supplementary))));
		tabFutures.values().forEach(CompletableFuture::join);
		
		notebook.addTab(DEFINITION_TAB, tabFutures.get(DEFINITION_TAB).join());
		supplementaries.keySet().stream()
			.filter(supplementary -> !supplementaries.get(supplementary).isEmpty())
			.forEach(supplementary -> notebook.addTab(supplementary, tabFutures.get(supplementary).join()));
		return notebook;
	}
	
	private CompletableFuture<Component> tabCompletable(String supplementaryName, List<ChineseSummaryLookup> lookups)
	{
		if(supplementaryName.equals(SubstringSearch.LOOKUP_NAME) && UiConstants.getFlag(UiConstants.FLAG_ALWAYS_SINGLE_SUBSTRING))
		{
			final List<ChineseSummaryLookup> nonSingle = lookups.stream()
					.filter(result -> ChineseText.trueChars(result.getChinese()).size() > 1)
					.collect(Collectors.toCollection(ArrayList::new));
			return CompletableFuture.supplyAsync(() -> {return new UiList().render(new ArrayList<ChineseSummaryLookup>(nonSingle.isEmpty() ? lookups : nonSingle));});
		}
		return CompletableFuture.supplyAsync(() -> {return new UiList().render(new ArrayList<ChineseSummaryLookup>(lookups));});
	}

	private JPanel renderZhDefinition(ChineseDefinitionLookup dictionaryResult)
	{
		Debug.logTimestamp("start single char");

		final JPanel result = new JPanel(new GridBagLayout());
		result.setBorder(UiConstants.TRACER());

		final int rowsRendered = renderDictionaryResults(result, dictionaryResult);
		renderZhCharBig(dictionaryResult.getZh(), result);
		UiUtils.renderFiller(result, rowsRendered+1);

		Debug.logTimestamp("end single char");
		return result;
	}

	private int renderDictionaryResults(JComponent parent, ChineseDefinitionLookup dictionaryResult)
	{
		int row = ROW_START_DEFINITIONS;
		for(final String pinyin : dictionaryResult.getResults().keySet())
		{
			renderLabelValue(parent, "Pinyin", pinyin, row);
			row++;

			final List<String> definitions = dictionaryResult.getResults().get(pinyin);
			renderLabelValue(parent, "Definition", String.join(", ", definitions), row);

			row++;
		}

		if(!dictionaryResult.getZh().equals(dictionaryResult.getSimplified()))
		{
			renderLabelValue(parent, "Simplified", dictionaryResult.getSimplified(), row);
			row++;
		}

		if(dictionaryResult.getMeasureWords().size() > 0)
		{
			final List<String> measureWords = dictionaryResult.getMeasureWords();
			renderLabelValue(parent, "Measure Words", String.join(", ", measureWords), row);
			row++;
		}
		return row;
	}

	private void renderLabelValue(JComponent parent, String label, String value, int row)
	{
		UiUtils.renderLabelToGrid(parent, label, row, COL_LABEL, false);
		UiUtils.renderLabelToGrid(parent, value, row, COL_VALUE, true);
	}

	private void renderZhCharBig(String zhchar, JPanel target)
	{
		final JTextPane zhPane = new JTextPane();
		zhPane.setText(zhchar);
		zhPane.setBorder(UiConstants.TRACER());
		zhPane.setFont(UiUtils.makeFont(zhPane, UiConstants.FONT_LARGE));
		zhPane.setBackground(null);
		zhPane.setEditable(false);
		zhPane.setBorder(UiConstants.TRACER());

		final int ALL_COLUMNS = 2;
		final GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = COL_LABEL;
		constraints.gridy = ROW_BIG_CHAR;
		constraints.weightx = UiConstants.GRIDBAG_AUTOEXPAND;
		constraints.gridwidth = ALL_COLUMNS;
		constraints.anchor = GridBagConstraints.CENTER;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.insets = UiUtils.makeInsets(Set.of(Neighbor.BOTTOM));

		target.add(zhPane, constraints);
	}
}

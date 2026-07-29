package dt.jdictionary.ui;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;

import dt.jdictionary.ChineseDefinitionLookup;
import dt.jdictionary.ChineseSummaryLookup;
import dt.jdictionary.ExhaustiveChineseLookup;
import dt.jdictionary.dbservice.alternative.SubstringSearch;
import dt.jdictionary.ui.UiUtils.Neighbor;
import dt.util.ChineseText;
import dt.util.LogUtils;


class UiChineseLookup
{
	private static final Logger logger = Logger.getLogger(UiChineseLookup.class.getName());

	private final String DEFINITION_TAB = "Definition";
	private final int COL_LABEL = 0;
	private final int COL_VALUE = 1;
	private final int ROW_BIG_CHAR = 0;
	private final int ROW_START_DEFINITIONS = 1;

	public UiChineseLookup() {}
	
	public JComponent render(ExhaustiveChineseLookup dictionaryResult, ExecutorService executor)
	{
		// Return the raw notebook. Don't prepackage it in a panel.
		final JTabbedPane notebook = new JTabbedPane();
		notebook.setBorder(UiConstants.TRACER());

		final Map<String, CompletableFuture<JComponent>> tabFutures= new LinkedHashMap<>();
		tabFutures.put(DEFINITION_TAB, CompletableFuture.supplyAsync(() -> {return this.renderZhDefinition(dictionaryResult.getDefinition());}, executor));
		
		final Map<String, List<ChineseSummaryLookup>> supplementaries = dictionaryResult.getSupplementaries();
		supplementaries.keySet().stream()
			.filter(supplementary -> !supplementaries.get(supplementary).isEmpty())
			.forEach(supplementary -> tabFutures.put(supplementary, tabCompletable(supplementary, supplementaries.get(supplementary), executor)));
		
		CompletableFuture.allOf(tabFutures.values().toArray(new CompletableFuture[0]))
			.exceptionally(ex -> {
				logger.severe("problems with ui chinese lookup " + LogUtils.printStackTrace(ex.getCause()));
				return null;
			})	
			.thenRun(() -> {
				SwingUtilities.invokeLater(() -> {
					tabFutures.forEach((label, future) -> {notebook.addTab(label, future.join());;});
				});
			});
		
		return notebook;
	}
	
	private CompletableFuture<JComponent> tabCompletable(String supplementaryName, List<ChineseSummaryLookup> lookups, ExecutorService executor)
	{
		if(supplementaryName.equals(SubstringSearch.LOOKUP_NAME) && !UiConstants.getFlag(UiConstants.FLAG_ALWAYS_SINGLE_SUBSTRING))
		{
			final List<ChineseSummaryLookup> nonSingle = lookups.stream()
				.filter(result -> ChineseText.trueLength(result.getChinese()) > 1)
				.collect(Collectors.toCollection(ArrayList::new));
			return CompletableFuture.supplyAsync(() -> {return new UiList(supplementaryName).render(nonSingle.isEmpty() ? lookups : nonSingle);}, executor);
		}
		return CompletableFuture.supplyAsync(() -> {return new UiList(supplementaryName).render(lookups);}, executor);
	}

	private JPanel renderZhDefinition(ChineseDefinitionLookup dictionaryResult)
	{
		logger.info("start single char");

		final JPanel result = new JPanel(new GridBagLayout());
		result.setBorder(UiConstants.TRACER());

		final int rowsRendered = renderDictionaryResults(result, dictionaryResult);
		renderZhCharBig(dictionaryResult.getZh(), result);
		UiUtils.renderFiller(result, rowsRendered+1);

		logger.info("end single char");
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

		if(!dictionaryResult.getMeasureWords().isEmpty())
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

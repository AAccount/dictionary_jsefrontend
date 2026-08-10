package dt.jdictionary.ui;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;
import dt.jdictionary.ChineseDefinitionLookup;
import dt.jdictionary.ExhaustiveChineseLookup;
import dt.jdictionary.dbrepo.DictionaryEntry;
import dt.jdictionary.dbservice.alternative.SubstringSearch;
import dt.jdictionary.ui.UiUtils.Neighbor;
import dt.util.ChineseText;


class UiChineseLookup
{
	private static final Logger logger = Logger.getLogger(UiChineseLookup.class.getName());

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
		notebook.setBorder(UiConstants.TRACER());
		notebook.addTab(DEFINITION_TAB, this.renderChineseDefinition(dictionaryResult.getDefinition()));
		dictionaryResult.getSupplementaries().forEach((label, results) -> {notebook.add(label, renderTab(label, results));});	
		return notebook;
	}
	
	private JComponent renderTab(String supplementaryName, List<DictionaryEntry> lookups)
	{
		if(supplementaryName.equals(SubstringSearch.LOOKUP_NAME) && !UiConstants.getFlag(UiConstants.FLAG_ALWAYS_SINGLE_SUBSTRING))
		{
			final List<DictionaryEntry> nonSingle = lookups.stream()
				.filter(result -> ChineseText.trueLength(result.getChinese()) > 1)
				.collect(Collectors.toCollection(ArrayList::new));
			return new UiList(supplementaryName).render(nonSingle.isEmpty() ? lookups : nonSingle);
		}
		return new UiList(supplementaryName).render(lookups);
	}

	private JPanel renderChineseDefinition(ChineseDefinitionLookup dictionaryResult)
	{
		logger.info("start single char");

		final JPanel result = new JPanel(new GridBagLayout());
		result.setBorder(UiConstants.TRACER());

		final int rowsRendered = renderDictionaryResults(result, dictionaryResult);
		renderCharacterBig(dictionaryResult.getChinese(), result);
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

		if(!dictionaryResult.getChinese().equals(dictionaryResult.getSimplified()))
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

	private void renderCharacterBig(String character, JPanel target)
	{
		final JTextPane charPane = new JTextPane();
		charPane.setText(character);
		charPane.setBorder(UiConstants.TRACER());
		charPane.setFont(UiConstants.FONT_LARGE);
		charPane.setBackground(null);
		charPane.setEditable(false);

		final int ALL_COLUMNS = 2;
		final GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = COL_LABEL;
		constraints.gridy = ROW_BIG_CHAR;
		constraints.weightx = UiConstants.GRIDBAG_AUTOEXPAND;
		constraints.gridwidth = ALL_COLUMNS;
		constraints.anchor = GridBagConstraints.CENTER;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.insets = UiUtils.makeInsets(Set.of(Neighbor.BOTTOM));

		target.add(charPane, constraints);
	}
}

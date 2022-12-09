package dt.jdictionary.ui;
import java.util.List;
import java.util.Set;

import javax.swing.JPanel;

import dt.jdictionary.SimpleLookup;
import dt.jdictionary.Utils;
import dt.jdictionary.ui.UiUtils.Neighbor;
import dt.jdictionary.FullLookup;

import java.awt.*;
import javax.swing.*;

class UiSingleChar
{
	public UiSingleChar() {}

	// Need to be out here for independent thread rendering.
	private JComponent sameFrontTab; 
	private JComponent sameBackTab;

	private final int COL_LABEL = 0;
	private final int COL_VALUE = 1;
	private final int ROW_BIG_CHAR = 0;
	private final int ROW_START_DEFINITIONS = 1;

	public JComponent render(FullLookup dictionaryResult, List<SimpleLookup> sameFront, List<SimpleLookup> sameBack)
	{
		// Return the raw notebook. Don't prepackage it in a panel.
		Utils.logTimestamp("start single char");

		final JTabbedPane notebook = new JTabbedPane();
		notebook.setBorder(UiConstants.TRACER);
		notebook.addTab("Definition", renderZhDefinition(dictionaryResult));
		Utils.logTimestamp("end single char");

		renderRelatedWords(notebook, sameFront, sameBack);
		return notebook;
	}

	private JPanel renderZhDefinition(FullLookup dictionaryResult)
	{
		final JPanel result = new JPanel(new GridBagLayout());
		result.setBorder(UiConstants.TRACER);

		final int rowsRendered = renderDictionaryResults(result, dictionaryResult);
		renderZhCharBig(dictionaryResult.getZh(), result);
		UiUtils.renderFiller(result, rowsRendered+1);
		return result;
	}

	private void renderRelatedWords(JTabbedPane notebook,  List<SimpleLookup> sameFront, List<SimpleLookup> sameBack)
	{
		Thread sameFrontThread = null, sameBackThread = null;
		final boolean renderSameFront = sameFront.size() > 0;
		final boolean renderSameBack = sameBack.size() > 0;
		if(renderSameFront)
		{
			sameFrontThread = new Thread(() -> { sameFrontTab = new UiList().render(sameFront); });
			sameFrontThread.start();
		}
		if(renderSameBack)
		{
			sameBackThread = new Thread(() -> { sameBackTab =  new UiList().render(sameBack); });
			sameBackThread.start();
		}

		try // Wait for the tabs to finish in this order so they can be added in this order.
		{
			if(renderSameFront)
			{
				sameFrontThread.join();
				notebook.addTab("Same Front", sameFrontTab);
			}
			if(renderSameBack)
			{
				sameBackThread.join();
				notebook.addTab("Same Back", sameBackTab);
			}
		} 
		catch (InterruptedException e) 
		{
			e.printStackTrace();
		}

	}

	private int renderDictionaryResults(JComponent parent, FullLookup dictionaryResult)
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
		zhPane.setBorder(UiConstants.TRACER);
		zhPane.setFont(UiUtils.makeFont(zhPane, UiConstants.FONT_LARGE));
		zhPane.setBackground(null);
		zhPane.setEditable(false);
		zhPane.setBorder(UiConstants.TRACER);

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

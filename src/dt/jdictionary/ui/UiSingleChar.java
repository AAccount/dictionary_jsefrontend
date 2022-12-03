package dt.jdictionary.ui;
import java.util.List;
import javax.swing.JPanel;

import dt.jdictionary.SimpleLookup;
import dt.jdictionary.FullLookup;

import java.awt.*;
import javax.swing.*;

public class UiSingleChar
{
	public UiSingleChar() {}

	public JComponent render(FullLookup dictionaryResult, List<SimpleLookup> sameFront, List<SimpleLookup> sameBack)
	{
		// Return the raw notebook. Don't prepackage it in a panel.
		final JTabbedPane notebook = new JTabbedPane();
		notebook.setBorder(UiConstants.TRACER);
		notebook.addTab("Definition", renderZhDefinition(dictionaryResult));
		if(sameFront.size() > 0)
		{
			notebook.addTab("Same Front", new UiList().render(sameFront));
		}
		if(sameBack.size() > 0)
		{
			notebook.addTab("Same Back", new UiList().render(sameBack));
		}
		return notebook;
	}

	private JPanel renderZhDefinition(FullLookup dictionaryResult)
	{
		final JPanel result = new JPanel(new GridBagLayout());
		result.setBorder(UiConstants.TRACER);

		final int rowsRendered = renderDictionaryResults(result, dictionaryResult);
		renderZhCharBig(dictionaryResult.getZh(), result, rowsRendered);
		UiUtils.renderFiller(result, rowsRendered+1);
		
		return result;
	}

	private int renderDictionaryResults(JComponent parent, FullLookup dictionaryResult)
	{
		int row = 0;
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
		final int LABEL_COL = 1;
		UiUtils.renderLabelToGrid(parent, label, row, LABEL_COL, false);

		final int VALUE_COL = 2;
		UiUtils.renderLabelToGrid(parent, UiUtils.wordWrapHack(value), row, VALUE_COL, true);
	}

	private void renderZhCharBig(String zhchar, JPanel target, int height)
	{
		final JLabel zhLabel = new JLabel(zhchar);
		zhLabel.setFont(UiUtils.generateFont(zhLabel, UiConstants.FONT_LARGE));

		final GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.weightx = UiConstants.GRIDBAG_NO_AUTOEXPAND;
		constraints.gridheight = height;
		constraints.anchor = GridBagConstraints.LINE_START;
		constraints.fill = GridBagConstraints.VERTICAL;
		constraints.insets = new Insets(10, 10, 10, 5);

		zhLabel.setBorder(UiConstants.TRACER);
		target.add(zhLabel, constraints);
	}
}

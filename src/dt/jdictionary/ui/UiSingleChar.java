package dt.jdictionary.ui;
import java.util.List;
import javax.swing.JPanel;

import dt.jdictionary.SimpleLookup;
import dt.jdictionary.Utils;
import dt.jdictionary.FullLookup;

import java.awt.*;
import javax.swing.*;

public class UiSingleChar
{
	public UiSingleChar() {}

	// Need to be out here for independent thread rendering.
	private JComponent sameFrontTab; 
	private JComponent sameBackTab;

	public JComponent render(FullLookup dictionaryResult, List<SimpleLookup> sameFront, List<SimpleLookup> sameBack)
	{
		// Return the raw notebook. Don't prepackage it in a panel.
		Utils.logTimestamp("start single char");

		final JTabbedPane notebook = new JTabbedPane();
		notebook.setBorder(UiConstants.TRACER);
		notebook.addTab("Definition", renderZhDefinition(dictionaryResult));
		renderRelatedWords(notebook, sameFront, sameBack);
		Utils.logTimestamp("end single char");
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

	private void renderRelatedWords(JTabbedPane notebook,  List<SimpleLookup> sameFront, List<SimpleLookup> sameBack)
	{
		Thread sameFrontThread = null, sameBackThread = null;
		final boolean renderSameFront = sameFront.size() > 0;
		final boolean renderSameBack = sameBack.size() > 0;
		if(renderSameFront)
		{
			sameFrontThread = new Thread(() -> {
				sameFrontTab = new UiList().render(sameFront);
			});
			sameFrontThread.start();
		}
		if(renderSameBack)
		{
			sameBackThread = new Thread(() -> {
				sameBackTab =  new UiList().render(sameBack);
			});
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
		UiUtils.renderLabelToGrid(parent, value, row, VALUE_COL, true);
	}

	private void renderZhCharBig(String zhchar, JPanel target, int height)
	{
		final JTextPane zhPane = new JTextPane();
		zhPane.setText(zhchar);
		zhPane.setBorder(UiConstants.TRACER);
		zhPane.setFont(UiUtils.generateFont(zhPane, UiConstants.FONT_LARGE));
		zhPane.setBackground(null);
		zhPane.setEditable(false);
		zhPane.setBorder(UiConstants.TRACER);

		final GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.weightx = UiConstants.GRIDBAG_NO_AUTOEXPAND;
		constraints.gridheight = height;
		constraints.anchor = GridBagConstraints.LINE_START;
		constraints.fill = GridBagConstraints.VERTICAL;
		constraints.insets = new Insets(10, 10, 10, 5);

		target.add(zhPane, constraints);
	}
}

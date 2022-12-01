package dt.jdictionary.ui;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import dt.jdictionary.SimpleLookup;

import java.awt.GridBagLayout;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

public class UiList implements ItemListener
{
	private final String FLAG_CHINA_SPECIES = "china species";
	private final String FLAG_NAME = "name"; // hard to detect reliably
	private final String FLAG_VARIANT_OF = "variant_of";
	private final String FLAG_LINK = "link";
	private final String FLAG_TOO_LONG = "too long";
	private final String FLAG_NONE = "";

	private final Map<String, List<JComponent>> flag2Ui;
	private final JComponent root;

	public UiList() 
	{
		flag2Ui = new HashMap<>();
		root = new JPanel(new GridBagLayout());
		root.setBorder(UiConstants.TRACER);
	}

	public JComponent render(List<SimpleLookup> dbResults)
	{
		final JPanel flagCheckboxes = new JPanel();
		flagCheckboxes.setBorder(UiConstants.TRACER);
		root.add(flagCheckboxes, UiUtils.generateGridConstraint(0, 0, true, false, UiConstants.nopadding));

		final JPanel dbResultPanel = new JPanel(new GridBagLayout());
		dbResultPanel.setBorder(UiConstants.TRACER);
		final JScrollPane scrollPane = new JScrollPane(dbResultPanel);
		scrollPane.setBorder(UiConstants.TRACER);

		for(int row = 0; row < dbResults.size(); row++)
		{
			renderSimpleLookup(dbResults.get(row), dbResultPanel, row);
		}
		root.add(scrollPane, UiUtils.generateGridConstraint(1, 0, true, true, UiConstants.nopadding));

		renderFlagCheckboxes(flagCheckboxes);
		return root;
	}

	private void renderSimpleLookup(SimpleLookup dbresult, JComponent parent, int row)
	{
		final int COL_ZH = 0;
		JComponent zhLabel = UiUtils.renderLabelToGrid(parent, dbresult.getZh(), row, COL_ZH, false);
		
		final int COL_PINYIN = 1;
		JComponent pinyinLabel = UiUtils.renderLabelToGrid(parent, dbresult.getPinyin(), row, COL_PINYIN, false);
		
		final int COL_DEF = 2;
		final String definition = String.join(", ", dbresult.getDefinitions()).toLowerCase();
		JComponent defLabel = UiUtils.renderLabelToGrid(parent, UiUtils.wordWrapHack(definition), row, COL_DEF, true);

		final String flag = flagDbResult(dbresult);
		if(flag.equals(FLAG_NONE))
		{
			return;
		}

		addToFlagMap(flag, zhLabel);
		zhLabel.setVisible(false);
		addToFlagMap(flag, pinyinLabel);
		pinyinLabel.setVisible(false);
		addToFlagMap(flag, defLabel);
		defLabel.setVisible(false);
	}

	private void renderFlagCheckboxes(JComponent parent)
	{
		for(final String flag : flag2Ui.keySet())
		{
			final JCheckBox flagCheckBox = new JCheckBox(flag);
			flagCheckBox.setBorder(UiConstants.TRACER);
			flagCheckBox.setName(flag);
			flagCheckBox.addItemListener(this);
			parent.add(flagCheckBox);
		}
	}

	private void addToFlagMap(String key, JComponent ui)
	{
		if(!flag2Ui.keySet().contains(key))
		{
			flag2Ui.put(key, new ArrayList<>());
		}
		flag2Ui.get(key).add(ui);
	}

	private String flagDbResult(SimpleLookup dbresult)
	{
		final String definition = String.join(", ", dbresult.getDefinitions()).toLowerCase();

		if(dbresult.getZh().length() > 4) // longer than a "4 char saying"
		{
			return FLAG_TOO_LONG;
		}

		if(definition.contains("species of china"))
		{
			return FLAG_CHINA_SPECIES;
		}

		if(definition.contains("variant of"))
		{
			return FLAG_VARIANT_OF;
		}

		final String linkFlagText = "see ";
		if(definition.startsWith(linkFlagText, 0))
		{
			return FLAG_LINK;
		}

		final String pinyinNoAccents = Normalizer.normalize(dbresult.getPinyin(), Form.NFD);
		final String definitionNoAccents = Normalizer.normalize(definition, Form.NFD);
		if(definition.contains(" county") || definitionNoAccents.contains(pinyinNoAccents))
		{
			return FLAG_NAME;
		}
		
		return FLAG_NONE;
	}

	@Override
	public void itemStateChanged(ItemEvent arg0) 
	{
		final JComponent checkbox =(JComponent)arg0.getSource();
		final String flag = checkbox.getName();
		for(final JComponent ui : flag2Ui.get(flag))
		{
			final boolean currentVisibiliy = ui.isVisible();
			ui.setVisible(!currentVisibiliy);
		}
		root.revalidate();
		root.repaint();
	}
}

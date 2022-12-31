package dt.jdictionary.ui;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import dt.jdictionary.SimpleLookup;
import dt.jdictionary.Utils;
import dt.jdictionary.ui.UiUtils.Neighbor;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class UiList implements ItemListener, ActionListener
{
	private final String FLAG_CHINA_SPECIES = "china species";
	private final String FLAG_NAME = "name"; // hard to detect reliably
	private final String FLAG_VARIANT_OF = "variant_of";
	private final String FLAG_LINK = "link";
	private final String FLAG_TOO_LONG = "too long";
	private final String FLAG_HYBRID_SLANG = "hybrid slang"; //entries with english and chinese letters
	private final String FLAG_NONE = "";

	private final int UI_COLUMN_RESULTS= 0;
	private final int UI_COLUMN_BACK= 0;
	private final int UI_COLUMN_FORWARD= 1;
	private final int UI_COLUMN_PAGE_COUNTER= 2;
	private final int UI_COLUMN_CHECKBOXES= 3;
	private final int UI_COLUMNS_TOTAL= 4;

	private final int UI_ROW_UTILITY = 0;
	private final int UI_ROW_RESULTS = 1;

	private final JButton forwardBtn;
	private final JButton previousBtn;
	private final String LABEL_COUNTER = "current page / total pages";
	private final JLabel pageCounter;
	private final String SCROLLVIEW_RESULTS = "results scroll view";
	private final String JPANEL_CHECKBOXES = "definition flag checkboxes";

	private final int PAGE_SIZE = 10;

	private final Map<String, List<JComponent>> flag2Ui;
	private final JComponent root;
	private final HistoryManager<List<SimpleLookup>> pages;

	public UiList() 
	{
		flag2Ui = new HashMap<>();
		root = new JPanel(new GridBagLayout());
		root.setBorder(UiConstants.TRACER);
		pages = new HistoryManager<>();
		
		previousBtn = new JButton();
		forwardBtn= new JButton();
		pageCounter = new JLabel();
	}

	public JComponent render(List<SimpleLookup> dbResults)
	{
		Utils.logTimestamp("start ui list");
		pages.addAllEntries(Utils.subdivideList(dbResults, PAGE_SIZE));

		renderPageNavigation();
		renderPageOfResults(pages.setIndex(0));
		Utils.logTimestamp("stop ui list");
		return root;
	}

	private void renderPageOfResults(List<SimpleLookup> results)
	{
		UiUtils.removeNamedComponents(root, Set.of(SCROLLVIEW_RESULTS, JPANEL_CHECKBOXES));
		flag2Ui.clear();

		// Need to leave the scrollpane setup even after pagination because grid bag layout will render "funny" without it.
		final JPanel dbResultPanel = new JPanel(new GridBagLayout());
		dbResultPanel.setBorder(UiConstants.TRACER);
		final JScrollPane scrollPane = new JScrollPane(dbResultPanel);
		scrollPane.setName(SCROLLVIEW_RESULTS);
		scrollPane.setBorder(UiConstants.TRACER);

		for(int row = 0; row < results.size(); row++)
		{
			renderSimpleLookup(results.get(row), dbResultPanel, row);
		}
		final GridBagConstraints constraints =  UiUtils.makeGridConstraint(UI_ROW_RESULTS, UI_COLUMN_RESULTS, true, true, UiConstants.nopadding);
		constraints.gridwidth = UI_COLUMNS_TOTAL;
		root.add(scrollPane, constraints);

		// Corresponding checkboxes need to be rendered per page.
		renderFlagCheckboxes();
		pageCounter.setText((pages.getIndex()+1)+"/"+(pages.getSize()));
	}

	private void renderSimpleLookup(SimpleLookup dbresult, JComponent parent, int row)
	{
		final int COL_ZH = 0;
		JComponent zhLabel = UiUtils.renderLabelToGrid(parent, dbresult.getZh(), row, COL_ZH, false);
		
		final int COL_PINYIN = 1;
		JComponent pinyinLabel = UiUtils.renderLabelToGrid(parent, dbresult.getPinyin(), row, COL_PINYIN, false);
		
		final int COL_DEF = 2;
		final String definition = String.join(", ", dbresult.getDefinitions()).toLowerCase();
		JComponent defLabel = UiUtils.renderLabelToGrid(parent, definition, row, COL_DEF, true);

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

	private void renderPageNavigation()
	{
		if(pages.getSize() == 1)
		{
			return;
		}

		previousBtn.setText("<");
		previousBtn.addActionListener(this);
		previousBtn.setEnabled(false);
		root.add(previousBtn, UiUtils.makeGridConstraint(UI_ROW_UTILITY, UI_COLUMN_BACK, false, false, UiUtils.makeInsets(Set.of(Neighbor.RIGHT))));

		forwardBtn.setText(">");
		forwardBtn.addActionListener(this);
		forwardBtn.setEnabled(true);
		root.add(forwardBtn, UiUtils.makeGridConstraint(UI_ROW_UTILITY, UI_COLUMN_FORWARD, false, false, UiUtils.makeInsets(Set.of(Neighbor.LEFT, Neighbor.RIGHT))));

		pageCounter.setName(LABEL_COUNTER);
		root.add(pageCounter,UiUtils.makeGridConstraint(UI_ROW_UTILITY, UI_COLUMN_PAGE_COUNTER, false, false, UiUtils.makeInsets(Set.of(Neighbor.LEFT, Neighbor.RIGHT))));
	}

	private void renderFlagCheckboxes()
	{
		final JPanel flagCheckboxes = new JPanel();
		flagCheckboxes.setName(JPANEL_CHECKBOXES);
		flagCheckboxes.setBorder(UiConstants.TRACER);
		root.add(flagCheckboxes, UiUtils.makeGridConstraint(UI_ROW_UTILITY, UI_COLUMN_CHECKBOXES, true, false, UiConstants.nopadding));

		for(final String flag : flag2Ui.keySet())
		{
			final JCheckBox flagCheckBox = new JCheckBox(flag);
			flagCheckBox.setBorder(UiConstants.TRACER);
			flagCheckBox.setName(flag);
			flagCheckBox.addItemListener(this);
			flagCheckboxes.add(flagCheckBox);
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

		final int FOUR_CHAR_EXPR = 4;
		if(dbresult.getZh().length() > FOUR_CHAR_EXPR)
		{
			return FLAG_TOO_LONG;
		}

		if(definition.contains("species of china"))
		{
			return FLAG_CHINA_SPECIES;
		}

		if(definition.contains("variant of") && dbresult.getDefinitions().size() == 1)
		{
			return FLAG_VARIANT_OF; //flag it if its ONLY definition is "variant of ___"
		}

		final String linkFlagText = "see ";
		if(definition.startsWith(linkFlagText, 0))
		{
			return FLAG_LINK;
		}

		if(!Utils.allChinese(dbresult.getZh()))
		{
			return FLAG_HYBRID_SLANG;
		}

		final String pinyinNoAccents = Utils.normalizePinyin(dbresult.getPinyin()).strip();
		final String definitionNoAccents = Utils.normalizePinyin(definition).strip();
		if(definition.contains(" county") || definition.contains("district of ") || definitionNoAccents.contains(pinyinNoAccents))
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

	@Override
	public void actionPerformed(ActionEvent arg0) 
	{
		final JComponent source = (JComponent)arg0.getSource();
		if(!List.of(forwardBtn, previousBtn).contains(source))
		{
			return;
		}

		final List<SimpleLookup> page = source == forwardBtn ? pages.goFwd() : pages.goBack();
		previousBtn.setEnabled(pages.canGoBack());
		forwardBtn.setEnabled(pages.canGoFwd());

		renderPageOfResults(page);
		root.revalidate();
		root.repaint();
	}
}

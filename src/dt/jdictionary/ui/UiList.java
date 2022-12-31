package dt.jdictionary.ui;
import java.util.List;
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

	private final JComponent root;
	private final HistoryManager<List<SimpleLookup>> pages;
	private final UiListFlags flagManager;

	public UiList() 
	{
		flagManager = new UiListFlags();
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
		flagManager.clear();

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

		flagManager.flagDbResult(dbresult, List.of(zhLabel, pinyinLabel, defLabel));
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

		for(final String flag : flagManager.allFlags())
		{
			final JCheckBox flagCheckBox = new JCheckBox(flag);
			flagCheckBox.setBorder(UiConstants.TRACER);
			flagCheckBox.setName(flag);
			flagCheckBox.addItemListener(this);
			flagCheckboxes.add(flagCheckBox);
		}
	}

	@Override
	public void itemStateChanged(ItemEvent arg0) 
	{
		final JComponent checkbox =(JComponent)arg0.getSource();
		final String flag = checkbox.getName();
		flagManager.toggleFlaggedComponents(flag);
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

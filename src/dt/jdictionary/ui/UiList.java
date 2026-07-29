package dt.jdictionary.ui;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import dt.jdictionary.ChineseSummaryLookup;
import dt.jdictionary.ui.UiUtils.Neighbor;
import dt.util.ListUtils;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class UiList implements ActionListener
{
	private static final Logger logger = Logger.getLogger(UiList.class.getName());

	private final int UI_COLUMN_RESULTS= 0;
	private final int UI_COLUMN_BACK= 0;
	private final int UI_COLUMN_FORWARD= 1;
	private final int UI_COLUMN_PAGE_COUNTER= 2;
	private final int UI_COLUMNS_TOTAL= 4;

	private final int UI_ROW_UTILITY = 0;
	private final int UI_ROW_RESULTS = 1;

	private final JButton forwardBtn;
	private final JButton previousBtn;
	private final String LABEL_COUNTER = "current page / total pages";
	private final JLabel pageCounter;
	private final String SCROLLVIEW_RESULTS = "results scroll view";

	private final int PAGE_SIZE = 10;

	private final JComponent root;
	private final HistoryManager<List<ChineseSummaryLookup>> pages;
	private final String purpose;

	public UiList(String purpose) 
	{
		root = new JPanel(new GridBagLayout());
		root.setBorder(UiConstants.TRACER());
		pages = new HistoryManager<>();
		
		previousBtn = new JButton();
		forwardBtn= new JButton();
		pageCounter = new JLabel();
		this.purpose = purpose;
	}

	public JComponent render(List<ChineseSummaryLookup> dbResults)
	{
		logger.info("start ui list for " + purpose);
		Collections.sort(dbResults, Collections.reverseOrder());
		pages.addAllEntries(ListUtils.subdivideList(dbResults, PAGE_SIZE));

		renderPageNavigation();
		renderPageOfResults(pages.setIndex(0));
		logger.info("stop ui list " + purpose);
		return root;
	}

	private void renderPageOfResults(List<ChineseSummaryLookup> results)
	{
		UiUtils.removeNamedComponents(root, Set.of(SCROLLVIEW_RESULTS));

		// Need to leave the scrollpane setup even after pagination because grid bag layout will render "funny" without it.
		final JPanel dbResultPanel = new JPanel(new GridBagLayout());
		dbResultPanel.setBorder(UiConstants.TRACER());
		final JScrollPane scrollPane = new JScrollPane(dbResultPanel);
		scrollPane.setName(SCROLLVIEW_RESULTS);
		scrollPane.setBorder(UiConstants.TRACER());

		for(int row = 0; row < results.size(); row++)
		{
			renderSimpleLookup(results.get(row), dbResultPanel, row);
		}
		final GridBagConstraints constraints =  UiUtils.makeGridConstraint(UI_ROW_RESULTS, UI_COLUMN_RESULTS, true, true, UiConstants.nopadding);
		constraints.gridwidth = UI_COLUMNS_TOTAL;
		root.add(scrollPane, constraints);

		// Corresponding checkboxes need to be rendered per page.
		pageCounter.setText((pages.getIndex()+1)+"/"+(pages.getSize()));
	}

	private void renderSimpleLookup(ChineseSummaryLookup dbresult, JComponent parent, int row)
	{
		final int COL_ZH = 0;
		UiUtils.renderLabelToGrid(parent, dbresult.getChinese(), row, COL_ZH, false);
		
		final int COL_PINYIN = 1;
		UiUtils.renderLabelToGrid(parent, dbresult.getPinyin(), row, COL_PINYIN, false);
		
		final int COL_DEF = 2;
		final String definition = dbresult.getDefinition().toLowerCase();
		UiUtils.renderLabelToGrid(parent, definition, row, COL_DEF, true);
		
		if(UiConstants.getFlag(UiConstants.FLAG_RANK))
		{
			final int COL_RANK = 3;
			UiUtils.renderLabelToGrid(parent, String.valueOf(dbresult.getRank()), row, COL_RANK, true);
		}
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

	@Override
	public void actionPerformed(ActionEvent arg0) 
	{
		final JComponent source = (JComponent)arg0.getSource();
		if(!List.of(forwardBtn, previousBtn).contains(source))
		{
			logger.info("actionPerformed not from forwardBtn or previousBtn " + source);
			return;
		}

		final List<ChineseSummaryLookup> page = source == forwardBtn ? pages.goFwd() : pages.goBack();
		previousBtn.setEnabled(pages.canGoBack());
		forwardBtn.setEnabled(pages.canGoFwd());

		renderPageOfResults(page);
		root.revalidate();
		root.repaint();
	}
}

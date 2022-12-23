package dt.jdictionary.ui;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import dt.jdictionary.SimpleLookup;
import dt.jdictionary.Utils;

import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

class UiList implements ItemListener
{
	private final String FLAG_CHINA_SPECIES = "china species";
	private final String FLAG_NAME = "name"; // hard to detect reliably
	private final String FLAG_VARIANT_OF = "variant_of";
	private final String FLAG_LINK = "link";
	private final String FLAG_TOO_LONG = "too long";
	private final String FLAG_NONE = "";

	private final Map<String, List<JComponent>> flag2Ui;
	private final JComponent root;
	private final ExecutorService rowRenderThreads;
	private final Lock flag2UiLock;

	public UiList() 
	{
		flag2Ui = new HashMap<>();
		root = new JPanel(new GridBagLayout());
		root.setBorder(UiConstants.TRACER);

		final int MIN_THREADS = 4;
		final int cpus = Runtime.getRuntime().availableProcessors();
		rowRenderThreads = Executors.newFixedThreadPool(cpus > MIN_THREADS ? cpus : MIN_THREADS);
		flag2UiLock = new ReentrantLock();
	}

	public JComponent render(List<SimpleLookup> dbResults)
	{
		Utils.logTimestamp("start ui list");
		final int UI_SINGLE_COLUMN = 0;
		final int UI_ROW_CHECKBOXES = 0;
		final int UI_ROW_RESULTS = 1;

		final JPanel flagCheckboxes = new JPanel();
		flagCheckboxes.setBorder(UiConstants.TRACER);
		root.add(flagCheckboxes, UiUtils.makeGridConstraint(UI_ROW_CHECKBOXES, UI_SINGLE_COLUMN, true, false, UiConstants.nopadding));

		final JPanel dbResultPanel = new JPanel(new GridBagLayout());
		dbResultPanel.setBorder(UiConstants.TRACER);
		final JScrollPane scrollPane = new JScrollPane(dbResultPanel);
		scrollPane.setBorder(UiConstants.TRACER);

		for(int row = 0; row < dbResults.size(); row++)
		{
			final int itsrow = row;
			rowRenderThreads.execute(new Thread(() -> renderSimpleLookup(dbResults.get(itsrow), dbResultPanel, itsrow)));
		}
		root.add(scrollPane, UiUtils.makeGridConstraint(UI_ROW_RESULTS, UI_SINGLE_COLUMN, true, true, UiConstants.nopadding));
		
		rowRenderThreads.shutdown();
		try 
		{
			rowRenderThreads.awaitTermination(10, TimeUnit.SECONDS);
		} 
		catch (InterruptedException e) 
		{
			e.printStackTrace();
		}

		renderFlagCheckboxes(flagCheckboxes);
		Utils.logTimestamp("stop ui list");
		return root;
	}

	// Ok to write to the non threadsafe parent because all the ui elements are written to different areas.
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
		flag2UiLock.lock();
		if(!flag2Ui.keySet().contains(key))
		{
			flag2Ui.put(key, new ArrayList<>());
		}
		flag2Ui.get(key).add(ui);
		flag2UiLock.unlock();
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

		final String pinyinNoAccents = Utils.normalizePinyin(dbresult.getPinyin());
		final String definitionNoAccents = Utils.normalizePinyin(definition);
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
}

package dt.jdictionary.ui;

import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JTextPane;

import java.awt.Font;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.util.Set;

import dt.jdictionary.Utils;

class UiUtils 
{
	public enum Neighbor
	{
		TOP,
		BOTTOM,
		LEFT,
		RIGHT,
		EVERYWHERE
	}

	public static Font makeFont(Component target, int size)
	{
		final Font currentFont = target.getFont();
		return new Font(currentFont.getName(), currentFont.getStyle(), size);
	}

	public static GridBagConstraints makeGridConstraint(int row, int column, boolean expandx, boolean expandy, Insets insets)
	{
		final int weightx = expandx ? UiConstants.GRIDBAG_AUTOEXPAND : UiConstants.GRIDBAG_NO_AUTOEXPAND;
		final int weighty = expandy ? UiConstants.GRIDBAG_AUTOEXPAND : UiConstants.GRIDBAG_NO_AUTOEXPAND;

		final GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = column;
		constraints.gridy = row;
		constraints.weightx = weightx;
		constraints.weighty = weighty;
		constraints.anchor = expandy ? GridBagConstraints.FIRST_LINE_START : GridBagConstraints.LINE_START;
		constraints.fill = expandy ? GridBagConstraints.BOTH : GridBagConstraints.HORIZONTAL;
		constraints.insets = insets;
		return constraints;
	}

	// If applying the word wrap hack, ALL cells in a column must have it and it has to be the same WIDTH for all of them.
	private static String wordWrapHack(String string)
	{
		return "<html><div WIDTH=400>" + string + "</div></html>";
	}

	public static void renderLabelToGrid(JComponent parent, String text, int row, int col, boolean expandx)
	{
		final String renderedText = expandx ? wordWrapHack(text) : text;
		final JTextPane textPane = new JTextPane();
		textPane.setContentType("text/html");
		textPane.setText(renderedText);
		textPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
		textPane.setBorder(UiConstants.TRACER());
		textPane.setBackground(null);
		textPane.setEditable(false);

		if(Utils.allChinese(text.replaceAll("\\s+|,|，", ""))) // don't let spaces and commas NOT flag the text as all Chinese
		{
			textPane.setFont(makeFont(textPane, UiConstants.FONT_MEDIUM));
		}

		final Insets insets = makeInsets(Set.of(Neighbor.EVERYWHERE));
		final GridBagConstraints labelConstraints = makeGridConstraint(row, col, expandx, false, insets);
		parent.add(textPane, labelConstraints);
	}

	public static final String UI_FILLER = "filler";
	public static void renderFiller(JComponent parent, int row)
	{
		final int FIRST_COLUMN = 0;
		final JLabel filler = new JLabel();
		filler.setName(UI_FILLER);
		filler.setBorder(UiConstants.TRACER());
		parent.add(filler, UiUtils.makeGridConstraint(row, FIRST_COLUMN, false, true, UiConstants.nopadding));
	}

	public static Insets makeInsets(Set<Neighbor> neighbors)
	{
		if(neighbors.contains(Neighbor.EVERYWHERE))
		{
			neighbors = Set.of(Neighbor.LEFT, Neighbor.RIGHT, Neighbor.TOP, Neighbor.BOTTOM);
		}

		final int PADDING = 10;
		return new Insets(
			neighbors.contains(Neighbor.TOP) ? PADDING / 2 : PADDING, 
			neighbors.contains(Neighbor.LEFT) ? PADDING / 2 : PADDING, 
			neighbors.contains(Neighbor.BOTTOM)? PADDING / 2 : PADDING, 
			neighbors.contains(Neighbor.RIGHT) ? PADDING / 2 : PADDING
			);
	}

	public static void removeNamedComponents(JComponent source, Set<String> removals)
	{
		final Component[] uiElements = (Component[])source.getComponents();
		for(final Component uiElement : uiElements)
		{
			if(uiElement.getName() != null && removals.contains(uiElement.getName()))
			{
				source.remove(uiElement);
			}
		}
	}
}

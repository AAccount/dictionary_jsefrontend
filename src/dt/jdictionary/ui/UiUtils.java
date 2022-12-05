package dt.jdictionary.ui;
import java.awt.*;

import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JTextPane;

import dt.jdictionary.Utils;

class UiUtils 
{
	public static Font generateFont(Component target, int size)
	{
		final Font currentFont = target.getFont();
		return new Font(currentFont.getName(), currentFont.getStyle(), size);
	}

	public static GridBagConstraints generateGridConstraint(int row, int column, boolean expandx, boolean expandy, Insets insets)
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

	public static JComponent renderLabelToGrid(JComponent parent, String text, int row, int col, boolean expandx)
	{
		final String renderedText = expandx ? wordWrapHack(text) : text;
		final JTextPane textPane = new JTextPane();
		textPane.setContentType("text/html");
		textPane.setText(renderedText);
		textPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
		textPane.setBorder(UiConstants.TRACER);
		textPane.setBackground(null);
		textPane.setEditable(false);

		if(Utils.hasChinese(text) && !text.matches(".*[a-zA-Z]+.*")) // don't show definitions that happen to have chinese in huge font
		{
			textPane.setFont(generateFont(textPane, UiConstants.FONT_MEDIUM));
		}

		final Insets insets = new Insets(5, 5, 5, 5);
		final GridBagConstraints labelConstraints = generateGridConstraint(row, col, expandx, false, insets);
		parent.add(textPane, labelConstraints);

		return textPane;
	}

	public static final String UI_FILLER = "filler";
	public static void renderFiller(JComponent parent, int row)
	{
		final JLabel filler = new JLabel();
		filler.setName(UI_FILLER);
		filler.setBorder(UiConstants.TRACER);
		parent.add(filler, UiUtils.generateGridConstraint(row, 0, false, true, UiConstants.nopadding));
	}
}

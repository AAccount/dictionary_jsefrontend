import java.awt.*;

import javax.swing.JComponent;
import javax.swing.JLabel;

public class Utils 
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
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.insets = insets;
		return constraints;
	}

	public static String wordWrapHack(String string)
	{
		return "<html>" + string + "</html>";
	}

	public static JComponent renderLabelToGrid(JComponent parent, String text, int row, int col, boolean expandx)
	{
		final JLabel jlabel = new JLabel(text);
		jlabel.setBorder(UiConstants.TRACER);
		final boolean hasChinese = text.codePoints().anyMatch(codepoint -> Character.UnicodeScript.of(codepoint) == Character.UnicodeScript.HAN);
		if(hasChinese)
		{
			jlabel.setFont(generateFont(jlabel, UiConstants.FONT_MEDIUM));
		}

		final Insets insets = new Insets(row == 0 ? 10 : 5, 5, 5, 5);
		final GridBagConstraints labelConstraints = generateGridConstraint(row, col, expandx, false, insets);
		parent.add(jlabel, labelConstraints);

		return jlabel;
	}
}

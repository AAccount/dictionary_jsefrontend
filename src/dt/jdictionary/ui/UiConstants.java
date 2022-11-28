package dt.jdictionary.ui;
import javax.swing.BorderFactory;
import javax.swing.border.Border;
import java.awt.Color;
import java.awt.Insets;

public class UiConstants 
{
	public static final int FONT_MEDIUM = 20;
	public static final int FONT_LARGE = 45;

	public static final Border TRACER = null; //BorderFactory.createLineBorder(Color.BLUE, 2);
	public static final int GRIDBAG_NO_AUTOEXPAND = 0;
	public static final int GRIDBAG_AUTOEXPAND = 1; //anything > 0 will work

	public static final Insets nopadding = new Insets(0,0,0,0);
}

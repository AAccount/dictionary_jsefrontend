package dt.jdictionary.ui;
import javax.swing.BorderFactory;
import javax.swing.border.Border;
import java.awt.Color;
import java.awt.Insets;
import java.util.Map;
import java.util.Set;

public class UiConstants 
{
	public static final int FONT_MEDIUM = 20;
	public static final int FONT_LARGE = 45;

	public static final String FLAG_TRACER = "SHOW_TRACER";
	public static final String FLAG_RANK = "SHOW_RANK";
	public static final String FLAG_AUTOSWAP = "AUTO_SWAP";
	public static final String FLAG_ALWAYS_SINGLE_SUBSTRING = "ALWAYS_SINGLE_SUBSTRING";
	public static final String FLAG_SAVE_HITS = "SAVE_HITS";
	private static final Map<String, Boolean> flagMap = Map.ofEntries(
		Map.entry(FLAG_TRACER, false),
		Map.entry(FLAG_RANK, false),
		Map.entry(FLAG_AUTOSWAP, true),
		Map.entry(FLAG_ALWAYS_SINGLE_SUBSTRING, false),
		Map.entry(FLAG_SAVE_HITS, true)
	);


	//DO NOT USE ON BUTTONS Causes weird rendering.
	public static Border TRACER()
	{
		return flagMap.getOrDefault(FLAG_TRACER, false) ? BorderFactory.createLineBorder(Color.BLUE, 2) : null;
	}
	
	public static final int GRIDBAG_NO_AUTOEXPAND = 0;
	public static final int GRIDBAG_AUTOEXPAND = 1; //anything > 0 will work

	public static final Insets nopadding = new Insets(0,0,0,0);
	
	public static void toggleFlag(String flag)
	{
		flagMap.put(flag, !UiConstants.flagMap.get(flag));
	}
	
	public static boolean getFlag(String flag)
	{
		return flagMap.get(flag);
	}
	
	public static Set<String> allFlags()
	{
		return flagMap.keySet();
	}
}

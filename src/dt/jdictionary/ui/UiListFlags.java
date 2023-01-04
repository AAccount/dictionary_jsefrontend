package dt.jdictionary.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JComponent;

import dt.jdictionary.SimpleLookup;
import dt.jdictionary.Utils;
import dt.jdictionary.events.EventUtils;

public class UiListFlags 
{
	private final String FLAG_CHINA_SPECIES = "china species";
	private final String FLAG_NAME = "name"; // hard to detect reliably
	private final String FLAG_VARIANT_OF = "variant_of";
	private final String FLAG_LINK = "link";
	private final String FLAG_TOO_LONG = "too long";
	private final String FLAG_HYBRID_SLANG = "hybrid slang"; //entries with english and chinese letters
	private final Set<String> POSSIBLE_FLAGS = Set.of(FLAG_CHINA_SPECIES, FLAG_NAME, FLAG_VARIANT_OF, FLAG_LINK, FLAG_TOO_LONG, FLAG_HYBRID_SLANG);

	private final Map<String, List<JComponent>> flag2Component;

	public UiListFlags()
	{
		flag2Component = new HashMap<>();
	}

	public void clear()
	{
		flag2Component.clear();
	}

	public void flagDbResult(SimpleLookup dbresult, List<JComponent> components)
	{
		final String definition = String.join(", ", dbresult.getDefinitions()).toLowerCase();

		final int FOUR_CHAR_EXPR = 4;
		final String linkFlagText = "see ";
		final String pinyinNoAccents = Utils.normalizePinyin(dbresult.getPinyin()).strip();
		final String definitionNoAccents = Utils.normalizePinyin(definition).strip();

		if(!Utils.allChinese(dbresult.getZh().replaceAll("\\s+|,|，", "")))
		{
			addToFlagMap(FLAG_HYBRID_SLANG, components);
		}
		else if(dbresult.getZh().length() > FOUR_CHAR_EXPR)
		{
			addToFlagMap(FLAG_TOO_LONG, components);
		}
		else if(definition.contains("species of china"))
		{
			addToFlagMap(FLAG_CHINA_SPECIES, components);
		}

		else if(definition.contains("variant of") && dbresult.getDefinitions().size() == 1)
		{
			addToFlagMap(FLAG_VARIANT_OF, components); //flag it if its ONLY definition is "variant of ___"
		}

		else if(definition.startsWith(linkFlagText, 0))
		{
			addToFlagMap(FLAG_LINK, components);
		}
		else if(definition.contains(" county") || definition.contains("district of ") || definitionNoAccents.contains(pinyinNoAccents))
		{
			addToFlagMap(FLAG_NAME, components);
		}
	}

	private void addToFlagMap(String key, List<JComponent> components)
	{
		if(!flag2Component.keySet().contains(key))
		{
			flag2Component.put(key, new ArrayList<>());
		}
		flag2Component.get(key).addAll(components);
		components.stream().forEach(component -> component.setVisible(false));
	}

	public Collection<String> allFlags()
	{
		return flag2Component.keySet();
	}

	public void toggleFlaggedComponents(String flag)
	{
		if(!POSSIBLE_FLAGS.contains(flag))
		{
			EventUtils.sendWarning("Trying to toggle unknown flag " + flag);
			return;
		}
		
		for(final JComponent component : flag2Component.get(flag))
		{
			final boolean currentVisibiliy = component.isVisible();
			component.setVisible(!currentVisibiliy);
		}	
	}
}

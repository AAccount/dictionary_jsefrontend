package dt.jdictionary.uiutil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dt.cedict.MeasureWords;
import dt.cedict.SimpleLookup;
import dt.cedict.ZhPinyin;
import dt.jdictionary.ChineseSummaryLookup;
import dt.jdictionary.MeasureSummary;
import dt.util.MapUtil;

public class Convert
{
	public static ChineseSummaryLookup simpleLookupToChineseSummary(SimpleLookup simpleLookup)
	{
		return new ChineseSummaryLookup(simpleLookup.getZh(), simpleLookup.getPinyin(), String.join("; ", simpleLookup.getDefinitions()), simpleLookup.getRank());
	}
	
	public static Map<String, List<MeasureSummary>> flattenCedictMeasures(List<MeasureWords> cedictMeasures)
	{
		final Map<String, List<MeasureSummary>> result = new HashMap<String, List<MeasureSummary>>();
		for(final MeasureWords measureWords : cedictMeasures)
		{
			for(final ZhPinyin entry : measureWords.getMeasures())
			{
				MapUtil.addToListMap(result, measureWords.getZh(), new MeasureSummary(entry.getZh(), entry.getPinyin()));
			}
		}
		return result;
	}
}

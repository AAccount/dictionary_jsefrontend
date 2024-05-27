package dt.jdictionary.util;

import java.util.Collection;
import java.util.stream.Collectors;

public class Stats
{

	public static double stdev(Collection<Double> numbers)
	{
		final double mean = Stats.average(numbers);
		final double sd = numbers.stream().collect(Collectors.summingDouble(val -> Math.pow(val - mean, 2)));
		return Math.sqrt(sd / numbers.size());
	}

	public static double average(Collection<Double> numbers)
	{
		return numbers.stream().mapToDouble(i -> i).average().orElse(0);
	}

}

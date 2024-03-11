package dt.jdictionary.cedict;

class SimpOgTuple
{
	private final String original;
	private final String simplified;
	
	public SimpOgTuple(String original, String simplified)
	{
		super();
		this.original = original;
		this.simplified = simplified;
	}

	public String getOriginal()
	{
		return original;
	}

	public String getSimplified()
	{
		return simplified;
	}
	
}

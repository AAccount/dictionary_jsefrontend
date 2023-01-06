package dt.jdictionary.sqlite.raw;

public class RawVariantRow 
{
	private final String original;
	private final String variant;
	private final String variantType;

	public RawVariantRow(String original, String simplified, String variantType) 
	{
		this.original = original;
		this.variant = simplified;
		this.variantType = variantType;
	}

	public String getOriginal() 
	{
		return original;
	}

	public String getVariant() 
	{
		return variant;
	}

	@Override
	public String toString() 
	{
		return "RawSimplifiedRow [original=" + original + ", simplified=" + variant + "]";
	}

	@Override
	public int hashCode() 
	{
		return toString().hashCode();
	}

	@Override
	public boolean equals(Object obj) 
	{
		if(obj == null || !obj.getClass().equals(this.getClass()))
		{
			return false;
		}

		final RawVariantRow casted = (RawVariantRow)obj;
		return
			casted.original.equals(this.original) &&
			casted.variant.equals(this.variant);
	}

	public String getVariantType() 
	{
		return variantType;
	}
}

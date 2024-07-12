package dt.jdictionary.sqlite.dbservice;

import java.util.ArrayList;
import java.util.List;

public class ExceptionPile extends Exception
{
	private static final long serialVersionUID = 20240711200804L;
	private final List<Exception> exceptions;
	
	public ExceptionPile(String message, List<Exception> exceptions)
	{
		super(message);
		this.exceptions = exceptions;
	}
	
	public ExceptionPile(String message, Exception exception)
	{
		super(message);
		this.exceptions = new ArrayList<>();
		this.exceptions.add(exception);
	}
	
	public List<Exception> getExceptions()
	{
		return exceptions;
	}
	
}

package dt.jdictionary.sqlite;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DbRepo 
{
	private Connection db;

	public enum RelatedChar
	{
		SAME_FRONT,
		SAME_BACK
	}

	private static final String COL_ZH = "zh";
	private static final String COL_EN = "en";
	private static final String COL_PINYIN = "pinyin";
	private static final String COL_SIMPLIFIED = "simplified";
	private static final String COL_OG = "original";
	private static final String COL_MEASURE_WORD = "measure";


	public DbRepo()
	{
		try 
		{
			final String sqlitePath = System.getProperty("user.home") + "/Programs/mdbgrip.sqlite";
			Class.forName("org.sqlite.JDBC");
			this.db = DriverManager.getConnection("jdbc:sqlite:"+sqlitePath);
		} 
		catch (SQLException e) 
		{
			e.printStackTrace();
			System.exit(1);
		} 
		catch (ClassNotFoundException e) 
		{
			e.printStackTrace();
			System.exit(1);
		}
	}

	public List<RawDbRow> lookupChinese(String zh)
	{
		final String sql = "select * from dictionary where zh = ?";
		return lookupDictionaryTable(sql, zh);
	}

	private List<RawDbRow> lookupDictionaryTable(String sql, String target)
	{
		final List<RawDbRow> rawDbRows = new ArrayList<>();
		try 
		{
			final PreparedStatement pst = db.prepareStatement(sql);
			pst.setString(1, target);
			final ResultSet results = pst.executeQuery();

			while(results.next())
			{
				rawDbRows.add(new RawDbRow(results.getString(COL_ZH), results.getString(COL_PINYIN), results.getString(COL_EN)));
			}
		}
		catch (SQLException e) 
		{
			e.printStackTrace();
		}
		return rawDbRows;
	}

	public String lookupSimplified(String zh)
	{
		String zhSimplified = "";
		try
		{
			final String inQuestionMarks = "?, ".repeat(zh.length());
			final String sql = "select * from simplified where original in ("+inQuestionMarks.substring(0, inQuestionMarks.length()-2)+")";
			final PreparedStatement pst = db.prepareStatement(sql);
			for(int pstIndex = 0; pstIndex<zh.length(); pstIndex++)
			{
				pst.setString(pstIndex+1, Character.toString(zh.charAt(pstIndex)));
			}
			final ResultSet results = pst.executeQuery();

			final Map<String, String> charMapper = new HashMap<>();
			while(results.next())
			{
				final String simplified = results.getString(COL_SIMPLIFIED);
				final String og = results.getString(COL_OG);
				charMapper.put(og, simplified);
			}

			for(final char stringChar : zh.toCharArray())
			{
				final String charAsString = Character.toString(stringChar);
				final String resultchar = charMapper.keySet().contains(charAsString) ? charMapper.get(charAsString) : charAsString;
				zhSimplified = zhSimplified + resultchar;
			}
		}
		catch (SQLException e) 
		{
			e.printStackTrace();
		}
		return zhSimplified;
	}

	public List<String> lookupMeasureWords(String zh)
	{
		final List<String> measureWords = new ArrayList<>();
		try
		{
			final String sql = "select measure from measureword where zh = ?";
			final PreparedStatement pst = db.prepareStatement(sql);
			pst.setString(1, zh);
			final ResultSet results = pst.executeQuery();

			while(results.next())
			{
				measureWords.add(results.getString(COL_MEASURE_WORD));
			}
		}
		catch (SQLException e) 
		{
			e.printStackTrace();
		}
		return measureWords;
	}

	public List<RawDbRow> lookupRelatedWord(String zh, RelatedChar similarity)
	{
		final String zhlike = similarity == RelatedChar.SAME_FRONT ? zh + "%" : "%" + zh;
		final String sql = "select * from dictionary where zh like ? and length(zh)>1";
		return lookupDictionaryTable(sql, zhlike);
	}

	public List<RawDbRow> lookupEnglish(String en)
	{
		final String sql = "select * from dictionary_fts5(?)";
		return lookupDictionaryTable(sql, en);
	}
}

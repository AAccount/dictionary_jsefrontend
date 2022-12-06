package dt.jdictionary.sqlite;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class DbRepo 
{
	public enum RelatedChar
	{
		SAME_FRONT,
		SAME_BACK
	}

	private Connection db;

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
			db.setAutoCommit(false);
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

	public void init()
	{
		final String createDictionary = "CREATE TABLE dictionary (zh	TEXT NOT NULL, en	TEXT NOT NULL, pinyin	TEXT NOT NULL, PRIMARY KEY(zh,en,pinyin))";
		final String createMeasureWords = "CREATE TABLE measureword (zh	TEXT, measure	TEXT, measurePinyin	TEXT, PRIMARY KEY(zh,measure))";
		final String createSimplified = "CREATE TABLE simplified (original	TEXT NOT NULL, simplified	TEXT NOT NULL, PRIMARY KEY(original,simplified))";
		final String createFTS5 = "CREATE VIRTUAL TABLE dictionary_fts5 using fts5(zh, en, pinyin)";
		final String[] tables = {createDictionary, createMeasureWords, createSimplified, createFTS5};

		for(final String table : tables)
		{
			try 
			{
				final Statement stmt = db.createStatement();
				stmt.execute(table);
			} 
			catch (SQLException e) 
			{
				e.printStackTrace();
			}
		}
	}

	public void wipe()
	{
		try 
		{
			final Statement findTables = db.createStatement();
			final ResultSet foundTables = findTables.executeQuery("SELECT name FROM sqlite_master WHERE type='table'");
			final List<String> tables = new ArrayList<>();
			while(foundTables.next())
			{
				tables.add(foundTables.getString(1));
			}

			final Statement rm = db.createStatement();
			for(final String table : tables)
			{
				rm.execute("drop table if exists " + table + ";");
			}
			db.commit();

			db.setAutoCommit(true);
			final Statement vaccuum = db.createStatement();
			vaccuum.execute("vacuum;");
			db.setAutoCommit(false);
		} 
		catch (SQLException e) 
		{
			e.printStackTrace();
		}
	}

	public List<RawDictionaryRow> lookupChinese(String zh)
	{
		final String sql = "select * from dictionary where zh = ?";
		return lookupDictionaryTable(sql, zh);
	}

	private List<RawDictionaryRow> lookupDictionaryTable(String sql, String target)
	{
		final List<RawDictionaryRow> rawDbRows = new ArrayList<>();
		try 
		{
			final PreparedStatement pst = db.prepareStatement(sql);
			pst.setString(1, target);
			final ResultSet results = pst.executeQuery();

			while(results.next())
			{
				rawDbRows.add(new RawDictionaryRow(results.getString(COL_ZH), results.getString(COL_PINYIN), results.getString(COL_EN)));
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

	public List<RawDictionaryRow> lookupRelatedWord(String zh, RelatedChar similarity)
	{
		final String zhlike = similarity == RelatedChar.SAME_FRONT ? zh + "%" : "%" + zh;
		final String sql = "select * from dictionary where zh like ? and length(zh)>1";
		return lookupDictionaryTable(sql, zhlike);
	}

	public List<RawDictionaryRow> lookupEnglish(String en)
	{
		final String sql = "select * from dictionary_fts5(?)";
		return lookupDictionaryTable(sql, en);
	}

	public void fillDictionary(List<RawDictionaryRow> allRows)
	{
		final String sqlNormal = "INSERT INTO dictionary (zh, en, pinyin) VALUES (?,?,?)";
		final String sqlFTS5 = "INSERT INTO dictionary_fts5 (zh, en, pinyin) VALUES (?,?,?)";
		try 
		{
			final PreparedStatement pstNormal = db.prepareStatement(sqlNormal);
			final PreparedStatement pstFTS5 = db.prepareStatement(sqlFTS5);
			final PreparedStatement[] psts = {pstNormal, pstFTS5};
			for(final PreparedStatement pst : psts)
			{
				for(final RawDictionaryRow row : allRows)
				{
					pst.setString(1, row.getZh());
					pst.setString(2, row.getSingleDefinition());
					pst.setString(3, row.getPinyin());
					pst.addBatch();
				}
				pst.executeBatch();
			}
			db.commit();
		} 
		catch (SQLException e) 
		{
			e.printStackTrace();
		}
	}

	public void fillMeasureWords(List<RawMeasureWordRow> allRows)
	{
		final String sql = "INSERT INTO measureword (zh, measure, measurePinyin) VALUES (?,?,?)";
		try 
		{
			final PreparedStatement pst = db.prepareStatement(sql);
		
			for(final RawMeasureWordRow row : allRows)
			{
				pst.setString(1, row.getZh());
				pst.setString(2, row.getMeasure());
				pst.setString(3, row.getMeasurePinyin());
				pst.addBatch();
			}
			pst.executeBatch();
			db.commit();
		} 
		catch (SQLException e) 
		{
			e.printStackTrace();
		}
	}

	public void fillSimplified(List<RawSimplifiedRow> allRows)
	{
		final String sql = "INSERT INTO simplified (original, simplified) VALUES (?,?)";
		try 
		{
			final PreparedStatement pst = db.prepareStatement(sql);
		
			for(final RawSimplifiedRow row : allRows)
			{
				System.out.println(row);
				pst.setString(1, row.getOriginal());
				pst.setString(2, row.getSimplified());
				pst.executeUpdate();
				db.commit();
			}
			// pst.executeBatch();
		} 
		catch (SQLException e) 
		{
			e.printStackTrace();
		}
	}
}

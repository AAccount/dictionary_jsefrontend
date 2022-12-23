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

import dt.jdictionary.SimpleLookup;

class DbRepo 
{
	public enum RelatedChar
	{
		SAME_FRONT,
		SAME_BACK
	}

	private Connection db;

	private static final String COL_ZH = "zh";
	private static final String COL_DEF = "definition";
	private static final String COL_PINYIN = "pinyin";
	private static final String COL_PINYIN_NORM = "pinyinNormalized";
	private static final String COL_SIMPLIFIED = "simplified";
	private static final String COL_OG = "original";
	private static final String COL_MEASURE_WORD = "measure";
	private static final String COL_FIRST_CHAR = "firstChar";
	private static final String COL_LAST_CHAR = "lastChar";
	private static final String COL_4_CHAR = "fourChar";

	private final String DictionaryBaseSql = String.format("""
		select %s, %s, %s, %s, %s, %s 
		from ZhBase join English on ZhBase.id = English.zhBaseId where"""
		, COL_ZH, COL_PINYIN, COL_PINYIN_NORM, COL_DEF, COL_FIRST_CHAR, COL_LAST_CHAR);

	public DbRepo()
	{
		try 
		{
			final String sqlitePath = System.getProperty("user.home") + "/Programs/mdbg2.sqlite";
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
		final String createZhBase = """
			CREATE TABLE ZhBase (
				id	INTEGER NOT NULL, 
				zh	TEXT NOT NULL, pinyin	TEXT NOT NULL, 
				pinyinNormalized TEXT NOT NULL, 
				firstChar TEXT, 
				lastChar TEXT, 
				PRIMARY KEY(id AUTOINCREMENT)
			)
			""";
		final String createIndexZhBaseZh = "CREATE INDEX ZhBaseSortZh ON ZhBase (zh)";
		final String createIndexZhBaseFirstChar = "CREATE INDEX ZhBaseSortFirst ON ZhBase (firstChar)";
		final String createIndexZhBaseLastChar = "CREATE INDEX ZhBaseSortLast ON ZhBase (lastChar)";
		final String createIndexZhBasePinyinNorm = "CREATE INDEX ZhBaseSortPinyinNorm ON ZhBase (pinyinNormalized)";

		final String createEnglish = "CREATE TABLE English (zhBaseId	INTEGER NOT NULL, definition	TEXT NOT NULL);";
		final String createEnglishFTS5 = "CREATE VIRTUAL TABLE English_fts5 using fts5(definition, zhBaseId)";

		final String createMeasureWords = """
			CREATE TABLE measureword (
				zh	TEXT NOT NULL, 
				measure	TEXT NOT NULL, 
				measurePinyin	TEXT  NOT NULL, 
				PRIMARY KEY(zh,measure)
				)""";
		final String createIndexMeasureZh = "CREATE INDEX MeasureWordZh ON measureword (zh)";

		final String createSimplified = """
			CREATE TABLE simplified (
				original	TEXT NOT NULL, 
				simplified	TEXT NOT NULL, 
				PRIMARY KEY(original,simplified)
			)""";
		final String createIndexSimplifiedOg = "CREATE INDEX SimplifiedSortOriginal ON simplified (original)";

		final String create4Char = """
			CREATE TABLE FourCharSubstring (
				substring	TEXT NOT NULL, 
				fourChar	TEXT NOT NULL, 
				PRIMARY KEY(substring,fourChar)
			)""";
		final String create4CharIndex = "CREATE INDEX FourCharSubSortSub ON FourCharSubstring (substring)";

		final String[] creates = {
			createZhBase, createIndexZhBaseZh, createIndexZhBaseFirstChar, createIndexZhBaseLastChar, createIndexZhBasePinyinNorm,
			createEnglish, createEnglishFTS5,
			createMeasureWords, createIndexMeasureZh,
			createSimplified, createIndexSimplifiedOg,
			create4Char, create4CharIndex
		};

		for(final String creation : creates)
		{
			try 
			{
				final Statement stmt = db.createStatement();
				stmt.execute(creation);
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
			final ResultSet foundTables = findTables.executeQuery("SELECT name FROM sqlite_master WHERE type='table' and name not like 'sqlite_%'");
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
		final String sql = DictionaryBaseSql + " zh = ?";
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
				final RawDictionaryRow row =  new RawDictionaryRow(
					results.getString(COL_ZH), 
					results.getString(COL_PINYIN), 
					results.getString(COL_PINYIN_NORM),
					results.getString(COL_DEF), 
					results.getString(COL_FIRST_CHAR), 
					results.getString(COL_LAST_CHAR)
				);
				rawDbRows.add(row);
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
		final String column = similarity == RelatedChar.SAME_FRONT ? COL_FIRST_CHAR : COL_LAST_CHAR;
		final String sql = DictionaryBaseSql + " " + column+" = ?";
		return lookupDictionaryTable(sql, zh);
	}

	public List<RawDictionaryRow> lookupEnglish(String en)
	{
		final String sql = String.format("""
			select %s, %s, %s, English.%s, %s, %s 
			from ZhBase 
				join English_fts5 on ZhBase.id = English_fts5.zhBaseId 
				join English on ZhBase.id = English.zhBaseId 
			where English_fts5.definition match ?""",
			COL_ZH, COL_PINYIN, COL_PINYIN_NORM, COL_DEF, COL_FIRST_CHAR, COL_LAST_CHAR);
		return lookupDictionaryTable(sql, en);
	}

	public List<String> tryFourChars(String compoundWord)
	{
		final List<String> result = new ArrayList<>();
		try
		{
			final String sql = "select fourChar from FourCharSubstring where substring = ?";
			final PreparedStatement pst = db.prepareStatement(sql);
			pst.setString(1, compoundWord);
			final ResultSet results = pst.executeQuery();

			while(results.next())
			{
				result.add(results.getString(COL_4_CHAR));
			}
		}
		catch (SQLException e) 
		{
			e.printStackTrace();
		}
		return result;
	}

	public void fillDictionary(List<SimpleLookup> allEntries)
	{
		final String sqlZhBase = "INSERT INTO ZhBase (zh, pinyin, pinyinNormalized, firstChar, lastChar) VALUES (?,?,?,?,?)";
		final String sqlEnglish = "INSERT INTO English (zhBaseId, definition) VALUES (?,?)";
		final String sqlEnglishFTS5 =  "INSERT INTO English_fts5 (zhBaseId, definition) VALUES (?,?)";
		try 
		{
			final PreparedStatement pstZhBase = db.prepareStatement(sqlZhBase);
			final PreparedStatement pstEnglish = db.prepareStatement(sqlEnglish);
			final PreparedStatement pstEnglishFts5 = db.prepareStatement(sqlEnglishFTS5);

			final PreparedStatement[] englishPsts = {pstEnglish, pstEnglishFts5};

			for(final SimpleLookup entry : allEntries)
			{
				final RawDictionaryRow zhBase = new RawDictionaryRow(entry.getZh(), entry.getPinyin());
				pstZhBase.setString(1, zhBase.getZh());
				pstZhBase.setString(2, zhBase.getPinyin());
				pstZhBase.setString(3, zhBase.getPinyinNormalized());
				pstZhBase.setString(4, zhBase.getFirstChar());
				pstZhBase.setString(5, zhBase.getLastChar());
				pstZhBase.execute();

				final PreparedStatement getId = db.prepareStatement("select last_insert_rowid() as id;");
				final ResultSet getIdResults = getId.executeQuery();
				getIdResults.next();
				final int id = getIdResults.getInt("id");
				
				for(final PreparedStatement pstEn : englishPsts)
				{
					for(final String definition : entry.getDefinitions())
					{
						pstEn.setInt(1, id);
						pstEn.setString(2, definition);
					}
					pstEn.addBatch();
				}
			}
			pstEnglish.executeBatch();
			pstEnglishFts5.executeBatch();
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
				pst.setString(1, row.getOriginal());
				pst.setString(2, row.getSimplified());
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

	public void fill4Chars(List<Raw4CharRow> allRows)
	{
		final String sql = "INSERT INTO FourCharSubstring (substring, fourChar) VALUES (?,?)";
		try 
		{
			final PreparedStatement pst = db.prepareStatement(sql);
		
			for(final Raw4CharRow row : allRows)
			{
				pst.setString(1, row.getSubstring());
				pst.setString(2, row.getFourChar());
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
}

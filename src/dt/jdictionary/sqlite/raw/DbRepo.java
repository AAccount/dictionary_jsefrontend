package dt.jdictionary.sqlite.raw;

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
import dt.jdictionary.Utils;
import dt.jdictionary.events.EventUtils;
import dt.jdictionary.sqlite.DbEvent;

public class DbRepo 
{
	public enum RelatedChar
	{
		SAME_FRONT,
		SAME_BACK
	}
	public static final int INIT_TRX_COUNT = 2;
	public static final int DICT_EN_TRX = 2;
	public static final int POST_DICT_TRX = 3; // measure words, simplified, 4 chars

	public static final String VARIANT_SIMPLIFIED = "simplified";
	public static final String VARIANT_GENERIC = "some variant";
	public static final String VARIANT_OLD = "pre traditional";

	private Connection db;

	private static final String COL_ZH = "zh";
	private static final String COL_DEF = "definition";
	private static final String COL_PINYIN = "pinyin";
	private static final String COL_PINYIN_NORM = "pinyinNormalized";
	private static final String COL_VARIANT = "simplified";
	private static final String COL_OG = "original";
	private static final String COL_MEASURE_WORD = "measure";
	private static final String COL_MEASURE_PINYIN = "measurePinyin";
	private static final String COL_FIRST_CHAR = "firstChar";
	private static final String COL_LAST_CHAR = "lastChar";
	private static final String COL_ZHBASEID = "zhBaseId";
	private static final String COL_ID = "id";
	private static final String COL_VARIANT_TYPE = "variantType";
	private static final String COL_SUBSTRING = "substring";
	private static final String COL_FULL_STRING = "fullString";

	private static final String TABLE_ZHBASE = "ZhBase";
	private static final String TABLE_ENGLISH = "English";
	private static final String TABLE_ENGLISH_FTS5 = "English_fts5";
	private static final String TABLE_MEASUREWORD = "MeasureWord";
	private static final String TABLE_VARIANTS = "Variants";
	private static final String TABLE_SUBSTRING = "Substring";

	private final String DictionaryBaseSql = String.format("""
		select %s, %s, %s, %s, %s, %s 
		from %s join %s on %s.%s = %s.%s where"""
		, COL_ZH, COL_PINYIN, COL_PINYIN_NORM, COL_DEF, COL_FIRST_CHAR, COL_LAST_CHAR, 
		TABLE_ZHBASE, TABLE_ENGLISH, TABLE_ZHBASE, COL_ID, TABLE_ENGLISH, COL_ZHBASEID);

	private final Object user;

	public DbRepo(Object caller)
	{
		this.user = caller;
		try 
		{
			final String sqlitePath = System.getProperty("user.home") + "/Programs/mdbg2_1.sqlite";
			Class.forName("org.sqlite.JDBC");
			this.db = DriverManager.getConnection("jdbc:sqlite:"+sqlitePath);
			db.setAutoCommit(false);
			Utils.logTimestamp("new db repo for " + userToString());
		} 
		catch (SQLException e) 
		{
			EventUtils.sendError(e);
		} 
		catch (ClassNotFoundException e) 
		{
			EventUtils.sendError(e);
		}
	}

	public void close()
	{
		try 
		{
			db.close();
			Utils.logTimestamp("closed db repo for " + userToString());
		} 
		catch (SQLException e) 
		{
			EventUtils.sendError(e);
		}
	}

	private String userToString()
	{
		final String[] classPath = user.getClass().getName().split("\\.");
		return classPath[classPath.length-1] + " " + (user.hashCode() % 1000);
	}

	public void init()
	{
		final List<List<String>> indexes = new ArrayList<>();
		final String createZhBase = String.format("""
			CREATE TABLE %s (
				%s	INTEGER NOT NULL, 
				%s	TEXT NOT NULL, 
				%s	TEXT NOT NULL, 
				%s TEXT NOT NULL, 
				%s TEXT, 
				%s TEXT, 
				PRIMARY KEY(%s AUTOINCREMENT)
			)
			""", TABLE_ZHBASE, COL_ID, COL_ZH, COL_PINYIN, COL_PINYIN_NORM, COL_FIRST_CHAR, COL_LAST_CHAR, COL_ID);
		indexes.add(List.of(TABLE_ZHBASE, COL_ZH));
		indexes.add(List.of(TABLE_ZHBASE, COL_FIRST_CHAR));
		indexes.add(List.of(TABLE_ZHBASE, COL_LAST_CHAR));
		indexes.add(List.of(TABLE_ZHBASE, COL_PINYIN_NORM));

		final String createEnglish = String.format("""
			CREATE TABLE %s (
				%s	INTEGER NOT NULL, 
				%s	TEXT NOT NULL
			);""", TABLE_ENGLISH, COL_ZHBASEID, COL_DEF);
		final String createEnglishFTS5 = String.format("CREATE VIRTUAL TABLE %s using fts5(%s, %s)", TABLE_ENGLISH_FTS5, COL_DEF, COL_ZHBASEID);

		final String createMeasureWords = String.format("""
			CREATE TABLE %s (
				%s	TEXT NOT NULL, 
				%s	TEXT NOT NULL, 
				%s	TEXT NOT NULL, 
				PRIMARY KEY(%s,%s)
				)""", TABLE_MEASUREWORD, COL_ZH, COL_MEASURE_WORD, COL_MEASURE_PINYIN, COL_ZH, COL_MEASURE_WORD);
		indexes.add(List.of(TABLE_MEASUREWORD, COL_ZH));

		final String createVariants = String.format("""
			CREATE TABLE %s (
				%s	TEXT NOT NULL, 
				%s	TEXT NOT NULL,
				%s	TEXT NOT NULL,  
				PRIMARY KEY(%s,%s)
			)""", TABLE_VARIANTS, COL_OG, COL_VARIANT, COL_VARIANT_TYPE, COL_OG, COL_VARIANT);
		indexes.add(List.of(TABLE_VARIANTS, COL_OG));
		indexes.add(List.of(TABLE_VARIANTS, COL_VARIANT));
		indexes.add(List.of(TABLE_VARIANTS, COL_VARIANT_TYPE));

		final String create4Char = String.format("""
			CREATE TABLE %s (
				%s	TEXT NOT NULL, 
				%s	TEXT NOT NULL, 
				PRIMARY KEY(%s,%s)
			)""", TABLE_SUBSTRING, COL_SUBSTRING, COL_FULL_STRING, COL_SUBSTRING, COL_FULL_STRING);
		indexes.add(List.of(TABLE_SUBSTRING, COL_SUBSTRING));

		final String[] tables = {
			createZhBase,
			createEnglish, createEnglishFTS5,
			createMeasureWords,
			createVariants,
			create4Char
		};

		try 
		{
			for(final String table : tables)
			{
					final Statement stmt = db.createStatement();
					stmt.execute(table);
			}

			for(final List<String> index : indexes)
			{
				final Statement stmt = db.createStatement();
				final String table = index.get(0);
				final String column = index.get(1);
				stmt.execute(String.format("CREATE INDEX %sSort%s ON %s (%s)", table, column, table, column));
			}
		} 
		catch (SQLException e) 
		{
			EventUtils.sendError(e);
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
			EventUtils.sendError(e);
		}
	}

	public List<RawDictionaryRow> lookupChinese(String zh)
	{
		final String sql = DictionaryBaseSql + " " + COL_ZH + " = ?";
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
			EventUtils.sendError(e);
		}
		return rawDbRows;
	}

	public String lookupSimplified(String zh)
	{
		String zhSimplified = "";
		try
		{
			final String inQuestionMarks = "?, ".repeat(zh.length());
			final String sql = "select * from %s where %s in ("+inQuestionMarks.substring(0, inQuestionMarks.length()-2)+") and %s = %s";
			final PreparedStatement pst = db.prepareStatement(String.format(sql, TABLE_VARIANTS, COL_OG, COL_VARIANT_TYPE, VARIANT_SIMPLIFIED));
			for(int pstIndex = 0; pstIndex<zh.length(); pstIndex++)
			{
				pst.setString(pstIndex+1, Character.toString(zh.charAt(pstIndex)));
			}
			final ResultSet results = pst.executeQuery();

			final Map<String, String> charMapper = new HashMap<>();
			while(results.next())
			{
				final String simplified = results.getString(COL_VARIANT);
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
			EventUtils.sendError(e);
		}
		return zhSimplified;
	}

	public List<String> lookupMeasureWords(String zh)
	{
		final List<String> measureWords = new ArrayList<>();
		try
		{
			final String sql = String.format("select %s from %s where %s = ?", COL_MEASURE_WORD, TABLE_MEASUREWORD, COL_ZH);
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
			EventUtils.sendError(e);
		}
		return measureWords;
	}

	public List<RawDictionaryRow> lookupRelatedWord(String zh, RelatedChar similarity)
	{
		final String column = similarity == RelatedChar.SAME_FRONT ? COL_FIRST_CHAR : COL_LAST_CHAR;
		final String sql = DictionaryBaseSql + " " + column + " = ?";
		return lookupDictionaryTable(sql, zh);
	}

	public List<RawDictionaryRow> lookupEnglish(String en)
	{
		final String sql = String.format("""
			select %s, %s, %s, English.%s, %s, %s 
			from %s 
				join %s on %s.%s = %s.%s 
				join %s on %s.%s = %s.%s
			where %s.%s match ?""",
			COL_ZH, COL_PINYIN, COL_PINYIN_NORM, TABLE_ENGLISH, COL_DEF, COL_FIRST_CHAR, COL_LAST_CHAR,
			TABLE_ZHBASE,
			TABLE_ENGLISH_FTS5, TABLE_ZHBASE, COL_ID, TABLE_ENGLISH_FTS5, COL_ZHBASEID,
			TABLE_ENGLISH, TABLE_ZHBASE, COL_ID, TABLE_ENGLISH, COL_ZHBASEID,
			TABLE_ENGLISH_FTS5, COL_DEF);
		return lookupDictionaryTable(sql, en);
	}

	public List<String> trySubstring(String compoundWord)
	{
		final String sql = String.format("select %s from %s where %s = ?", COL_FULL_STRING, TABLE_SUBSTRING, COL_SUBSTRING);
		return getListOfString(sql, compoundWord, COL_FULL_STRING);
	}

	public List<String> findSimplifiedNormalizedPinyins(String zh)
	{
		final String sql = String.format("""
			select 
				distinct %s 
			from %s 
				join %s on %s.%s = %s.%s
			where 
				%s = ?
				""", 
			COL_PINYIN_NORM, 
			TABLE_VARIANTS, 
			TABLE_ZHBASE, TABLE_VARIANTS, COL_OG, TABLE_ZHBASE, COL_ZH, 
			COL_VARIANT
		);
		return getListOfString(sql, zh, COL_PINYIN_NORM);
	}

	public List<RawDictionaryRow> findByNormalizedPinyin(String normalizedPinyin)
	{
		final String sql = DictionaryBaseSql + "  " + COL_PINYIN_NORM +  "=?";
		return lookupDictionaryTable(sql, normalizedPinyin);
	}

	private List<String> getListOfString(String sql, String search, String column)
	{
		final List<String> result = new ArrayList<>();
		try
		{
			final PreparedStatement pst = db.prepareStatement(sql);
			pst.setString(1, search);
			final ResultSet results = pst.executeQuery();

			while(results.next())
			{
				result.add(results.getString(column));
			}
		}
		catch (SQLException e) 
		{
			EventUtils.sendError(e);
		}
		return result;	
	}

	public void fillDictionary(List<SimpleLookup> allEntries)
	{
		final String sqlZhBase = String.format("INSERT INTO %s (%s, %s, %s, %s, %s) VALUES (?,?,?,?,?)", TABLE_ZHBASE, COL_ZH, COL_PINYIN, COL_PINYIN_NORM, COL_FIRST_CHAR, COL_LAST_CHAR);
		final String sqlEnglish = String.format("INSERT INTO %s (%s, %s) VALUES (?,?)", TABLE_ENGLISH, COL_ZHBASEID, COL_DEF);
		final String sqlEnglishFTS5 =  String.format("INSERT INTO %s (%s, %s) VALUES (?,?)", TABLE_ENGLISH_FTS5, COL_ZHBASEID, COL_DEF);
		try 
		{
			final PreparedStatement pstZhBase = db.prepareStatement(sqlZhBase);
			final PreparedStatement pstEnglish = db.prepareStatement(sqlEnglish);
			final PreparedStatement pstEnglishFts5 = db.prepareStatement(sqlEnglishFTS5);

			final PreparedStatement[] englishPsts = {pstEnglish, pstEnglishFts5};

			final int uptoDictTrxes = INIT_TRX_COUNT + allEntries.size() + DICT_EN_TRX;
			final int totalTrxes = uptoDictTrxes + POST_DICT_TRX;
			int saved = 0;
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
						pstEn.addBatch();
					}
				}
				saved++;
				DbEvent.sendProgressEvent(INIT_TRX_COUNT + saved, totalTrxes);
			}
			pstEnglish.executeBatch();
			pstEnglishFts5.executeBatch();
			DbEvent.sendProgressEvent(uptoDictTrxes, totalTrxes);
			db.commit();
		} 
		catch (SQLException e) 
		{
			EventUtils.sendError(e);
		}
	}

	public void fillMeasureWords(List<RawMeasureWordRow> allRows)
	{
		final String sql = String.format("INSERT INTO %s (%s, %s, %s) VALUES (?,?,?)", TABLE_MEASUREWORD, COL_ZH, COL_MEASURE_WORD, COL_MEASURE_PINYIN);
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
			EventUtils.sendError(e);
		}
	}

	public void fillSimplified(List<RawVariantRow> allRows)
	{
		final String sql = String.format("INSERT INTO %s (%s, %s, %s) VALUES (?,?,?)", TABLE_VARIANTS, COL_OG, COL_VARIANT, COL_VARIANT_TYPE);
		try 
		{
			final PreparedStatement pst = db.prepareStatement(sql);
		
			for(final RawVariantRow row : allRows)
			{
				pst.setString(1, row.getOriginal());
				pst.setString(2, row.getVariant());
				pst.setString(3, row.getVariantType());
				pst.addBatch();
			}
			pst.executeBatch();
			db.commit();
		} 
		catch (SQLException e) 
		{
			EventUtils.sendError(e);
		}
	}

	public void fill4Chars(List<RawSubstringRow> allRows)
	{
		final String sql = String.format("INSERT INTO %s (%s, %s) VALUES (?,?)", TABLE_SUBSTRING, COL_SUBSTRING, COL_FULL_STRING);
		try 
		{
			final PreparedStatement pst = db.prepareStatement(sql);
		
			for(final RawSubstringRow row : allRows)
			{
				pst.setString(1, row.getSubstring());
				pst.setString(2, row.getFullString());
				pst.addBatch();
			}
			pst.executeBatch();
			db.commit();
		} 
		catch (SQLException e) 
		{
			EventUtils.sendError(e);
		}
	}
}

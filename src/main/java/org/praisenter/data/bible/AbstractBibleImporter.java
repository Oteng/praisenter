package org.praisenter.data.bible;

import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.praisenter.data.Database;
import org.praisenter.resources.translations.Translations;

public abstract class AbstractBibleImporter implements BibleImporter {
	/** The class-level logger */
	private static final Logger LOGGER = LogManager.getLogger();
	
	/** The prepared statement SQL for inserting a bible */
	private static final String INSERT_BIBLE = "INSERT INTO BIBLE (DATA_SOURCE,NAME,LANGUAGE) VALUES(?, ?, ?)";
	
	/** The prepared statement SQL for inserting a book */
	private static final String INSERT_BOOK = "INSERT INTO BIBLE_BOOK (BIBLE_ID,CODE,NAME) VALUES(?, ?, ?)";

	/** The prepared statement SQL for inserting a verse */
	private static final String INSERT_VERSE = "INSERT INTO BIBLE_VERSE (BIBLE_ID,BOOK_CODE,CHAPTER,VERSE,SUB_VERSE,ORDER_BY,TEXT) VALUES(?, ?, ?, ?, ?, ?, ?)";
	
	// index rebuilding
	
	/** SQL for dropping the BO_A index */
	private static final String DROP_INDEX_BO_A = "DROP INDEX BO_A";
	
	/** SQL for recreating the BO_A index */
	private static final String CREATE_INDEX_BO_A = "CREATE INDEX BO_A ON BIBLE_VERSE(BIBLE_ID,ORDER_BY)";
	
	/** SQL for dropping the BO_D index */
	private static final String DROP_INDEX_BO_D = "DROP INDEX BO_D";
	
	/** SQL for recreating the BO_D index */
	private static final String CREATE_INDEX_BO_D = "CREATE INDEX BO_D ON BIBLE_VERSE(BIBLE_ID,ORDER_BY DESC)";
	
	/** SQL for dropping the BBCV index */
	private static final String DROP_INDEX_BBCV = "DROP INDEX BBCV";
	
	/** SQL for recreating the BBCV index */
	private static final String CREATE_INDEX_BBCV = "CREATE INDEX BBCV ON BIBLE_VERSE(BIBLE_ID,BOOK_CODE,CHAPTER,VERSE)";

	private final Database database;
	
	public AbstractBibleImporter(Database database) {
		this.database = database;
	}
	
	protected final void insert(Bible bible, List<Book> books, List<Verse> verses) throws SQLException, BibleAlreadyExistsException, BibleImportException {
		LOGGER.debug("Importing new bible: " + bible.name);
		// insert all the data into the tables
		try (Connection connection = this.database.getConnection()) {
			// begin the transaction
			connection.setAutoCommit(false);
			
			// verify the bible doesn't exist already
			try (Statement bibleQuery = connection.createStatement();
				 ResultSet bqResult = bibleQuery.executeQuery("SELECT COUNT(*) FROM BIBLE WHERE NAME = '" + bible.name + "'");) {
				// make sure we didn't get anything
				if (bqResult.next() && bqResult.getInt(1) > 0) {
					connection.rollback();
					LOGGER.error("The bible already exists in the .");
					throw new BibleAlreadyExistsException(MessageFormat.format(Translations.getTranslation("bible.import.error.duplicate"), bible.name));
				}
			} catch (SQLException e) {
				connection.rollback();
				LOGGER.error("Failed to query existing bibles before import.");
				throw e;
			}
			
			int bibleId = -1;
			
			// insert the bible
			try (PreparedStatement bibleInsert = connection.prepareStatement(INSERT_BIBLE, Statement.RETURN_GENERATED_KEYS)) {
				// set the parameters
				bibleInsert.setString(1, bible.source);
				bibleInsert.setString(2, bible.name);
				bibleInsert.setString(3, bible.language);
				// insert the bible
				int n = bibleInsert.executeUpdate();
				// get the generated id
				if (n > 0) {
					ResultSet result = bibleInsert.getGeneratedKeys();
					if (result.next()) {
						// get the bible id
						int id = result.getInt(1);
						bibleId = id;
					}
				} else {
					// throw an error
					connection.rollback();
					LOGGER.error("The insert of the bible failed (0 records updated).");
					throw new BibleImportException(MessageFormat.format(Translations.getTranslation("bible.import.error.bible"), bible.name));
				}
			} catch (SQLException e) {
				connection.rollback();
				throw e;
			}
			
			// make sure the bible was saved first
			if (bibleId > 0) {
				LOGGER.debug("Bible inserted successfully: " + bible.name);
				
				// insert the books
				try (PreparedStatement bookInsert = connection.prepareStatement(INSERT_BOOK)) {
					for (Book book : books) {
						// set the parameters
						bookInsert.setInt(1, bibleId);
						bookInsert.setString(2, book.code);
						bookInsert.setString(3, book.name);
						// execute the insert
						int n = bookInsert.executeUpdate();
						// make sure it worked
						if (n <= 0) {
							// roll back anything we've done
							connection.rollback();
							// throw an error
							throw new BibleImportException(MessageFormat.format(Translations.getTranslation("bible.import.error.book"), book.name, bible.name));
						}
					}
				} catch (SQLException e) {
					connection.rollback();
					throw e;
				}
				
				LOGGER.debug("Bible books inserted successfully: " + bible.name);
				
				// insert the verses
				try (PreparedStatement verseInsert = connection.prepareStatement(INSERT_VERSE)) {
					for (Verse verse : verses) {
						// set the parameters
						verseInsert.setInt(1, bibleId);
						verseInsert.setString(2, verse.book.code);
						verseInsert.setInt(3, verse.chapter);
						verseInsert.setInt(4, verse.verse);
						verseInsert.setInt(5, verse.subVerse);
						verseInsert.setInt(6, verse.order);
						verseInsert.setClob(7, new StringReader(verse.text));
						// execute the insert
						try {
							int n = verseInsert.executeUpdate();
							// make sure it worked
							if (n <= 0) {
								// roll back anything we've done
								connection.rollback();
								// throw an error
								throw new BibleImportException(MessageFormat.format(Translations.getTranslation("bible.import.error.book"), verse.book.name, verse.chapter, verse.verse, bible.name));
							}
						} catch (SQLIntegrityConstraintViolationException e) {
							// its possible that the dumps have duplicate keys (book, chapter, verse, subverse)
							// in this case we will ignore these and continue but log them as warnings
							LOGGER.warn("Duplicate verse in file [" + verse.book.code + "|" + verse.chapter + "|" + verse.verse + "|" + verse.subVerse + "]. Dropping verse.");
						}
						// let the outer try/catch handle other exceptions
					}
				} catch (SQLException e) {
					connection.rollback();
					throw e;
				}
				
				LOGGER.debug("Bible verses inserted successfully: " + bible.name);
			}
			
			// commit all the changes
			connection.commit();
			
			LOGGER.debug("Bible imported successfully: " + bible.name);
			
			// rebuild the indexes after a bible has been imported
			LOGGER.debug("Rebuilding bible indexes.");
			rebuildIndexes();
		}
	}

	/**
	 * Rebuilds the indexes for the verses tables.
	 */
	private final void rebuildIndexes() {
		try (Connection connection = this.database.getConnection()) {
			connection.setAutoCommit(false);
			try (Statement statement = connection.createStatement();) {
				statement.execute(DROP_INDEX_BO_A);
				statement.execute(CREATE_INDEX_BO_A);
				
				statement.execute(DROP_INDEX_BO_D);
				statement.execute(CREATE_INDEX_BO_D);
				
				statement.execute(DROP_INDEX_BBCV);
				statement.execute(CREATE_INDEX_BBCV);
				
				connection.commit();
				
				LOGGER.debug("Bible indexes rebuilt successfully.");
			} catch (SQLException e) {
				// roll back any index changes we have done thus far
				connection.rollback();
				// just log this error
				LOGGER.warn("An error occurred when rebuilding the indexes after a successful import of a bible:", e);
			}
		} catch (SQLException e) {
			// just log this error
			LOGGER.warn("An error occurred when rebuilding the indexes after a successful import of a bible:", e);
		}
	}
	
}
/**
 * 
 */
package com.database_preservation.modules.jdbc.out;

import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.w3c.util.InvalidDateException;

import com.database_preservation.model.data.BinaryCell;
import com.database_preservation.model.data.Cell;
import com.database_preservation.model.data.ComposedCell;
import com.database_preservation.model.data.Row;
import com.database_preservation.model.data.SimpleCell;
import com.database_preservation.model.exception.InvalidDataException;
import com.database_preservation.model.exception.ModuleException;
import com.database_preservation.model.exception.UnknownTypeException;
import com.database_preservation.model.structure.ColumnStructure;
import com.database_preservation.model.structure.DatabaseStructure;
import com.database_preservation.model.structure.ForeignKey;
import com.database_preservation.model.structure.SchemaStructure;
import com.database_preservation.model.structure.TableStructure;
import com.database_preservation.model.structure.type.SimpleTypeBinary;
import com.database_preservation.model.structure.type.SimpleTypeBoolean;
import com.database_preservation.model.structure.type.SimpleTypeDateTime;
import com.database_preservation.model.structure.type.SimpleTypeNumericApproximate;
import com.database_preservation.model.structure.type.SimpleTypeNumericExact;
import com.database_preservation.model.structure.type.SimpleTypeString;
import com.database_preservation.model.structure.type.Type;
import com.database_preservation.model.structure.type.UnsupportedDataType;
import com.database_preservation.modules.DatabaseHandler;
import com.database_preservation.modules.SQLHelper;

/**
 * @author Luis Faria
 * 
 */
public class JDBCExportModule implements DatabaseHandler {

	private final Logger logger = Logger.getLogger(JDBCExportModule.class);

	protected static final boolean DEFAULT_CAN_DROP_DATABASE = false;

	protected static int BATCH_SIZE = 100;

	protected final String driverClassName;

	protected final String connectionURL;

	protected final Map<String, Connection> connections;

	protected final boolean canDropDatabase;

	protected Connection connection;

	protected Statement statement;

	protected DatabaseStructure databaseStructure;

	protected TableStructure currentTableStructure;

	protected SQLHelper sqlHelper;

	protected int batch_index;

	protected PreparedStatement currentRowInsertStatement;

	protected Set<String> ignoredSchemas;

	protected Set<String> existingSchemas;

	protected boolean currentIsIgnoredSchema;

	/**
	 * Generic JDBC export module constructor
	 * 
	 * @param driverClassName
	 *            the name of the JDBC driver class
	 * @param connectionURL
	 *            the URL to use in connection
	 */
	public JDBCExportModule(String driverClassName, String connectionURL) {
		this(driverClassName, connectionURL, new SQLHelper());
	}

	/**
	 * Generic JDBC export module constructor with SQLHelper definition
	 * 
	 * @param driverClassName
	 *            the name of the JDBC driver class
	 * @param connectionURL
	 *            the URL to use in connection
	 * @param sqlHelper
	 *            the SQLHelper instance to use
	 */
	public JDBCExportModule(String driverClassName, String connectionURL,
			SQLHelper sqlHelper) {
		logger.debug(driverClassName + ", " + connectionURL);
		this.driverClassName = driverClassName;
		this.connectionURL = connectionURL;
		this.sqlHelper = sqlHelper;
		this.connections = new HashMap<String, Connection>();
		this.canDropDatabase = DEFAULT_CAN_DROP_DATABASE;
		connection = null;
		statement = null;
		databaseStructure = null;
		currentTableStructure = null;
		batch_index = 0;
		currentRowInsertStatement = null;
		ignoredSchemas = new HashSet<String>();
		existingSchemas = null;
		currentIsIgnoredSchema = false;
	}

	/**
	 * Connect to the server using the properties defined in the constructor, or
	 * return the existing connection
	 * 
	 * @return the connection
	 * 
	 * @throws ModuleException
	 *             This exception can be thrown if the JDBC driver class is not
	 *             found or an SQL error occurs while connecting
	 */
	public Connection getConnection() throws ModuleException {
		if (connection == null) {
			try {
				logger.debug("Loading JDBC Driver " + driverClassName);
				Class.forName(driverClassName);
				logger.debug("Getting connection");
				logger.debug("Connection URL: " + connectionURL);
				connection = DriverManager.getConnection(connectionURL);
				connection.setAutoCommit(true);
				logger.debug("Connected");
			} catch (ClassNotFoundException e) {
				throw new ModuleException(
						"JDBC driver class could not be found", e);
			} catch (SQLException e) {
				throw new ModuleException("SQL error creating connection", e);
			}
		}
		return connection;
	}

	/**
	 * Get a connection to a database. This connection can be used to create the
	 * database
	 * 
	 * @param databaseName
	 *            the name of the database to connect
	 * 
	 * @return the JDBC connection
	 * @throws ModuleException
	 */
	public Connection getConnection(String databaseName, String connectionURL)
			throws ModuleException {
		Connection connection;
		try {
			logger.debug("Database: " + databaseName);
			logger.debug("Loading JDBC Driver " + driverClassName);
			Class.forName(driverClassName);
			logger.debug("Getting admin connection");
			logger.debug("Connection URL: " + connectionURL);
			connection = DriverManager.getConnection(connectionURL);
			connection.setAutoCommit(true);
			logger.debug("Connected");
			connections.put(databaseName, connection);
		} catch (ClassNotFoundException e) {
			throw new ModuleException("JDBC driver class could not be found", e);
		} catch (SQLException e) {
			throw new ModuleException("SQL error creating connection", e);
		}

		return connection;
	}

	/**
	 * Check if a database exists
	 * 
	 * @param defaultConnectionDb
	 *            an existing dbml database to establish the connection
	 * @param database
	 *            the name of the database to check
	 * @param connectionURL
	 *            the connection URL needed by getConnection
	 * 
	 * @return true if exists, false otherwise
	 * @throws ModuleException
	 */
	public boolean databaseExists(String defaultConnectionDb, String database,
			String connectionURL) throws ModuleException {
		boolean found = false;
		try {
			ResultSet result = getConnection(defaultConnectionDb, connectionURL)
					.createStatement().executeQuery(
							sqlHelper.getDatabases(database));
			while (result.next() && !found) {
				if (result.getString(1).equalsIgnoreCase(database)) {
					found = true;
				}
			}
		} catch (SQLException e) {
			throw new ModuleException("Error checking if database " + database
					+ " exists", e);
		}
		return found;
	}

	protected Statement getStatement() throws ModuleException {
		if (statement == null && getConnection() != null) {
			try {
				statement = getConnection().createStatement();
			} catch (SQLException e) {
				throw new ModuleException("SQL error creating statement", e);
			}
		}
		return statement;
	}

	public void initDatabase() throws ModuleException {
		logger.debug("on init db");
		getConnection();
		// nothing to do
	}

	/**
	 * Override this method to create the database
	 * 
	 * @param dbName
	 *            the database name
	 * @throws ModuleException
	 */
	protected void createDatabase(String dbName) throws ModuleException {
		// nothing will be done by default
	}

	public void handleStructure(DatabaseStructure structure)
			throws ModuleException, UnknownTypeException {
		this.databaseStructure = structure;
		try {
			this.existingSchemas = getExistingSchemasNames();
		} catch (SQLException e) {
			logger.error("An error occurred while getting the name "
					+ "of existing schemas");
		}
		createDatabase(structure.getName());
		int[] batchResult = null;
		if (getStatement() != null) {
			try {
				logger.info("Handling database structure");
				for (SchemaStructure schema : structure.getSchemas()) {
					// won't export ignored schemas
					if (isIgnoredSchema(schema)) {
						logger.warn("Schema not exported because it's defined "
								+ "as ignored (possibily is a system schema): "
								+ schema.getName());
						continue;
					}

					handleSchemaStructure(schema);
				}
				logger.debug("Executing table creation batch");
				batchResult = getStatement().executeBatch();
				getStatement().clearBatch();
				logger.info("Handling datababase structure finished");
			} catch (SQLException e) {
				if (batchResult != null) {
					for (int i = 0; i < batchResult.length; i++) {
						int result = batchResult[i];
						if (result == Statement.EXECUTE_FAILED) {
							logger.error("Batch failed at index " + i);
						}
					}
				}
				SQLException ei = e;
				do {
					if (ei != null) {
						logger.error("Error creating "
								+ "structure (next exception)", ei);
					}
					ei = ei.getNextException();
				} while (ei != null);
				throw new ModuleException("Error creating structure", e);
			}
		}
	}

	protected Set<String> getDefaultSchemas() {
		return new HashSet<String>();
	}

	protected void handleSchemaStructure(SchemaStructure schema)
			throws ModuleException, UnknownTypeException {
		logger.info("Handling schema structure " + schema.getName());
		try {
			if (!isExistingSchema(schema.getName())) {
				getStatement().addBatch(sqlHelper.createSchemaSQL(schema));
				getStatement().executeBatch();
				logger.debug("batch executed: " + schema.getName());
			}

			for (TableStructure table : schema.getTables()) {
				handleTableStructure(table);
			}

			logger.info("Handling schema structure " + schema.getName()
					+ " finished");
		} catch (SQLException e) {
			logger.error("Error handling schema structure", e);
			SQLException nextException = e.getNextException();
			if (nextException != null) {
				logger.error("Error details", nextException);
			}
			throw new ModuleException("Error while adding schema SQL to batch",
					e);
		}
	}

	/**
	 * Checks if a schema with 'schemaName' already exists on the database.
	 * 
	 * @param schemaName
	 *            the schema name to be checked.
	 * @return
	 * 
	 * @throws SQLException
	 * @throws ModuleException
	 */
	protected boolean isExistingSchema(String schemaName) throws SQLException,
			ModuleException {
		boolean exists = false;
		for (String existingName : getExistingSchemasNames()) {
			if (existingName.equalsIgnoreCase(schemaName)) {
				exists = true;
				break;
			}
		}
		return exists;
	}

	/**
	 * Gets the list of names of the existing schemas on a database.
	 * 
	 * @return The list of schemas names on a database.
	 * @throws SQLException
	 * @throws ModuleException
	 */
	protected Set<String> getExistingSchemasNames() throws SQLException,
			ModuleException {
		if (existingSchemas == null) {
			existingSchemas = new HashSet<String>();
			ResultSet rs = getConnection().getMetaData().getSchemas();
			while (rs.next()) {
				existingSchemas.add(rs.getString(1));
			}
		}
		return existingSchemas;
	}

	protected void handleTableStructure(TableStructure table)
			throws ModuleException, UnknownTypeException {

		if (getStatement() != null) {
			try {
				logger.info("Handling table structure " + table.getName());
				logger.debug("Adding to batch creation of table "
						+ table.getName());
				logger.debug("SQL: " + sqlHelper.createTableSQL(table));
				getStatement().addBatch(sqlHelper.createTableSQL(table));
				String pkeySQL = sqlHelper.createPrimaryKeySQL(table.getId(),
						table.getPrimaryKey());
				if (pkeySQL != null) {
					logger.debug("SQL: " + pkeySQL);
					getStatement().addBatch(pkeySQL);
				}
			} catch (SQLException e) {
				throw new ModuleException("Error while adding SQL to batch", e);
			}
		}
	}

	/**
	 * Sets the schemas to be ignored on the export. These schemas won't be
	 * exported
	 * 
	 * @param ignoredSchemas
	 *            ignored schemas name to be added to the list
	 */
	public void setIgnoredSchemas(Set<String> ignoredSchemas) {
		for (String s : ignoredSchemas) {
			this.ignoredSchemas.add(s);
		}
	}

	/**
	 * Checks if a given schema is set to be ignored
	 * 
	 * @param schema
	 *            The schema structure to be checked
	 * @return
	 */
	protected boolean isIgnoredSchema(SchemaStructure schema) {
		for (String s : ignoredSchemas) {
			if (schema.getName().matches(s)) {
				return true;
			}
		}
		return false;
	}

	public void handleDataOpenTable(String tableId) throws ModuleException {
		logger.debug("Started data open: " + tableId);
		if (databaseStructure != null) {
			TableStructure table = databaseStructure
					.lookupTableStructure(tableId);
			this.currentTableStructure = table;
			if (currentTableStructure != null) {
				currentIsIgnoredSchema = isIgnoredSchema(table.getSchema());
				if (!currentIsIgnoredSchema) {
					try {
						logger.info("Exporting data for " + table.getId());
						currentRowInsertStatement = getConnection()
								.prepareStatement(
										sqlHelper
												.createRowSQL(currentTableStructure));
						logger.debug("sql: "
								+ sqlHelper.createRowSQL(currentTableStructure));

					} catch (SQLException e) {
						throw new ModuleException("Error creating table "
								+ tableId + " prepared statement", e);
					}
				}
			} else {
				throw new ModuleException("Could not find table id '" + tableId
						+ "' in database structure");
			}
		} else {
			throw new ModuleException(
					"Cannot open table before database structure is created");
		}
	}

	public void handleDataCloseTable(String tableId) throws ModuleException {
		currentTableStructure = null;
		if (batch_index > 0) {
			try {
				currentRowInsertStatement.executeBatch();
			} catch (SQLException e) {
				logger.error("Error closing table", e);
				SQLException nextException = e.getNextException();
				if (nextException != null) {
					logger.error("More details", nextException);
				}
				throw new ModuleException("Error executing insert batch", e);
			}
			batch_index = 0;
			currentRowInsertStatement = null;
			currentIsIgnoredSchema = false;
		}
	}

	public void handleDataRow(Row row) throws InvalidDataException,
			ModuleException {
		if (!currentIsIgnoredSchema) {
			if (currentTableStructure != null
					&& currentRowInsertStatement != null) {
				Iterator<ColumnStructure> columnIterator = currentTableStructure
						.getColumns().iterator();
				List<CleanResourcesInterface> cleanResourcesList = new ArrayList<CleanResourcesInterface>();
				int index = 1;
				for (Cell cell : row.getCells()) {
					ColumnStructure column = columnIterator.next();
					CleanResourcesInterface cleanResources = handleDataCell(
							currentRowInsertStatement, index, cell,
							column.getType());
					cleanResourcesList.add(cleanResources);
					index++;
				}
				try {
					currentRowInsertStatement.addBatch();
					if (++batch_index > BATCH_SIZE) {
						currentRowInsertStatement.executeBatch();
						currentRowInsertStatement.clearBatch();
						batch_index = 0;
					}
				} catch (SQLException e) {
					throw new ModuleException("Error executing insert batch", e);
				} finally {
					for (CleanResourcesInterface clean : cleanResourcesList) {
						clean.clean();
					}
				}
			} else if (databaseStructure != null) {
				throw new ModuleException(
						"Cannot handle data row before a table "
								+ "is open and insert statement created");
			}
		}
	}

	public interface CleanResourcesInterface {
		public void clean();
	}

	protected CleanResourcesInterface handleDataCell(PreparedStatement ps,
			int index, Cell cell, Type type) throws InvalidDataException,
			ModuleException {
		CleanResourcesInterface ret = new CleanResourcesInterface() {
			public void clean() {

			}
		};
		try {
			if (cell instanceof SimpleCell) {
				SimpleCell simple = (SimpleCell) cell;
				String data = simple.getSimpledata();
				logger.debug("data: " + data);
				logger.debug("type: " + type.getOriginalTypeName());
				if (type instanceof SimpleTypeString) {
					handleSimpleTypeStringDataCell(data, ps, index, cell, type);
				} else if (type instanceof SimpleTypeNumericExact) {
					handleSimpleTypeNumericExactDataCell(data, ps, index, cell,
							type);
				} else if (type instanceof SimpleTypeNumericApproximate) {
					handleSimpleTypeNumericApproximateDataCell(data, ps, index,
							cell, type);
				} else if (type instanceof SimpleTypeDateTime) {
					handleSimpleTypeDateTimeDataCell(data, ps, index, cell,
							type);
				} else if (type instanceof SimpleTypeBoolean) {
					handleSimpleTypeBooleanDataCell(data, ps, index, cell, type);
				} else if (type instanceof UnsupportedDataType) {
					handleSimpleTypeStringDataCell(data, ps, index, cell, type);
				} else if (type instanceof SimpleTypeBinary) {
					if (data != null) {
						ps.setString(index, data);
					} else {
						ps.setNull(index, Types.BINARY);
					}
				} else {
					throw new InvalidDataException(type.getClass()
							.getSimpleName()
							+ " not applicable to simple cell or "
							+ "not yet supported");
				}
			} else if (cell instanceof BinaryCell) {
				final BinaryCell bin = (BinaryCell) cell;

				if (type instanceof SimpleTypeBinary) {
					if (bin.getInputstream() != null) {
						ps.setBinaryStream(index, bin.getInputstream(),
								(int) bin.getLength());
					} else {
						logger.debug("is null");
						ps.setNull(index, Types.BINARY);
					}
					ret = new CleanResourcesInterface() {

						public void clean() {
							bin.cleanResources();
						}

					};
				} else if (type instanceof SimpleTypeString) {
					handleSimpleTypeString(ps, index, bin);

					ret = new CleanResourcesInterface() {

						public void clean() {
							bin.cleanResources();
						}

					};

				} else {
					logger.error("Binary cell found when column type is "
							+ type.getClass().getSimpleName());
				}

			} else if (cell instanceof ComposedCell) {
				// ComposedCell comp = (ComposedCell) cell;
				// TODO export composed data
				throw new ModuleException("Composed data not yet supported");
			} else {
				throw new ModuleException("Unsuported cell type "
						+ cell.getClass().getName());
			}
		} catch (SQLException e) {
			throw new ModuleException("SQL error while handling cell "
					+ cell.getId(), e);
		} catch (InvalidDateException e) {
			throw new InvalidDataException("Error handling cell "
					+ cell.getId() + ":" + e.getMessage());
		}
		return ret;
	}

	protected void handleSimpleTypeStringDataCell(String data,
			PreparedStatement ps, int index, Cell cell, Type type)
			throws SQLException {
		if (data != null) {
			ps.setString(index, data);
		} else {
			ps.setNull(index, Types.VARCHAR);
		}
	}

	protected void handleSimpleTypeNumericExactDataCell(String data,
			PreparedStatement ps, int index, Cell cell, Type type)
			throws NumberFormatException, SQLException {
		if (data != null) {
			// logger.debug("big decimal: " + data);
			BigDecimal bd = new BigDecimal(data);
			ps.setBigDecimal(index, bd);
		} else {
			ps.setNull(index, Types.INTEGER);
		}
	}

	protected void handleSimpleTypeNumericApproximateDataCell(String data,
			PreparedStatement ps, int index, Cell cell, Type type)
			throws NumberFormatException, SQLException {
		if (data != null) {
			ps.setString(index, data);
		} else {
			ps.setNull(index, Types.FLOAT);
		}
	}

	protected void handleSimpleTypeDateTimeDataCell(String data,
			PreparedStatement ps, int index, Cell cell, Type type)
			throws InvalidDateException, SQLException {
		SimpleTypeDateTime dateTime = (SimpleTypeDateTime) type;
		if (dateTime.getTimeDefined()) {
			if (type.getSql99TypeName().equalsIgnoreCase("TIMESTAMP")
					|| type.getSql99TypeName().equalsIgnoreCase(
							"TIMESTAMP WITH TIME ZONE")) {
				if (data != null) {
					// logger.debug("timestamp before: " + data);
					Calendar cal = javax.xml.bind.DatatypeConverter
							.parseDateTime(data);
					Timestamp sqlTimestamp = new Timestamp(
							cal.getTimeInMillis());
					logger.debug("timestamp after: " + sqlTimestamp.toString());
					ps.setTimestamp(index, sqlTimestamp);
				} else {
					ps.setNull(index, Types.TIMESTAMP);
				}
			} else {
				if (data != null) {
					// logger.debug("TIME before: " + data);
					Time sqlTime = Time.valueOf(data);
					// logger.debug("TIME after: " + sqlTime.toString());
					ps.setTime(index, sqlTime);
				} else {
					ps.setNull(index, Types.TIME);
				}
			}
		} else {
			if (data != null) {
				// logger.debug("DATE before: " + data);
				java.sql.Date sqlDate = java.sql.Date.valueOf(data);
				// logger.debug("DATE after: " + sqlDate.toString());
				ps.setDate(index, sqlDate);
			} else {
				ps.setNull(index, Types.DATE);
			}
		}
	}

	protected void handleSimpleTypeBooleanDataCell(String data,
			PreparedStatement ps, int index, Cell cell, Type type)
			throws SQLException {
		if (data != null) {
			// logger.debug("boolData: " + data);
			ps.setBoolean(index, Boolean.valueOf(data));
		} else {
			ps.setNull(index, Types.BOOLEAN);
		}
	}

	protected void handleSimpleTypeString(PreparedStatement ps, int index,
			BinaryCell bin) throws SQLException, ModuleException {
		ps.setClob(index, new InputStreamReader(bin.getInputstream()),
				bin.getLength());
	}

	public void finishDatabase() throws ModuleException {
		if (databaseStructure != null) {
			handleForeignKeys();
			commit();
		}
	}

	protected void handleForeignKeys() throws ModuleException {
		logger.debug("Creating foreign keys");
		try {
			for (SchemaStructure schema : databaseStructure.getSchemas()) {
				if (isIgnoredSchema(schema)) {
					continue;
				}
				for (TableStructure table : schema.getTables()) {

					for (ForeignKey fkey : table.getForeignKeys()) {
						String originalReferencedSchema = fkey
								.getReferencedSchema();

						String tableId = originalReferencedSchema + "."
								+ fkey.getReferencedTable();

						TableStructure tableAux = databaseStructure
								.lookupTableStructure(tableId);
						if (tableAux != null) {
							if (isIgnoredSchema(tableAux.getSchema())) {
								logger.warn("Foreign key not exported: "
										+ "referenced schema ("
										+ fkey.getReferencedSchema()
										+ ") is ignored at export.");
								continue;
							}
						}

						String fkeySQL = sqlHelper.createForeignKeySQL(table,
								fkey);
						logger.debug("Returned fkey: " + fkeySQL);
						getStatement().addBatch(fkeySQL);
					}

				}
			}
			logger.debug("Getting fkeys finished");
			getStatement().executeBatch();
			getStatement().clearBatch();
		} catch (SQLException e) {
			SQLException ei = e;
			do {
				if (ei != null) {
					logger.error(
							"Error handleing foreign key (next exception)", ei);
					logger.error("Error description: ", ei);
				}
				ei = ei.getNextException();
			} while (ei != null);
			throw new ModuleException("Error creating foreign keys", e);
		}
	}

	protected void commit() throws ModuleException {
		// logger.debug("Commiting");
		// try {
		// getConnection().commit();
		// } catch (SQLException e) {
		// throw new ModuleException("Error while commiting", e);
		// }
	}

	/**
	 * Get the SQLHelper used by this instance
	 * 
	 * @return the SQLHelper
	 */
	public SQLHelper getSqlHelper() {
		return sqlHelper;
	}
}

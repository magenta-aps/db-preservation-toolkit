/**
 * 
 */
package com.database_preservation.modules.mySql.out;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.database_preservation.model.exception.ModuleException;
import com.database_preservation.model.exception.UnknownTypeException;
import com.database_preservation.model.structure.ColumnStructure;
import com.database_preservation.model.structure.DatabaseStructure;
import com.database_preservation.model.structure.SchemaStructure;
import com.database_preservation.model.structure.TableStructure;
import com.database_preservation.model.structure.type.SimpleTypeBinary;
import com.database_preservation.model.structure.type.Type;

/**
 * @author Luis Faria
 * 
 */
public class PhpMyAdminExportModule extends MySQLJDBCExportModule {

	private final Logger logger = Logger
			.getLogger(PhpMyAdminExportModule.class);

	/**
	 * the name of the PhpMyAdmin database, used to allow the advanced features
	 */
	private static final String DEFAULT_PHPMYADMIN_DATABASE = "phpmyadmin";

	private static final String DEFAULT_COLUMN_INFO_TABLE = "pma__column_info";

	private String phpmyadmin_database;

	private String column_info_table;

	/**
	 * PhpMyAdmin export module constructor
	 * 
	 * @param hostname
	 *            the hostname of the MySQL server
	 * @param database
	 *            the name of the database to import from
	 * @param username
	 *            the name of the user to use in connection
	 * @param password
	 *            the password of the user to use in connection
	 */
	public PhpMyAdminExportModule(String hostname, String database,
			String username, String password) {
		super(hostname, database, username, password);

		phpmyadmin_database = DEFAULT_PHPMYADMIN_DATABASE;
		column_info_table = DEFAULT_COLUMN_INFO_TABLE;
	}

	/**
	 * PhpMyAdmin export module constructor
	 * 
	 * @param hostname
	 *            the hostname of the MySQL server
	 * @param port
	 *            the port that the MySQL server is listening
	 * @param database
	 *            the name of the database to import from
	 * @param username
	 *            the name of the user to use in connection
	 * @param password
	 *            the password of the user to use in connection
	 */
	public PhpMyAdminExportModule(String hostname, int port, String database,
			String username, String password) {
		super(hostname, port, database, username, password);

		phpmyadmin_database = DEFAULT_PHPMYADMIN_DATABASE;
		column_info_table = DEFAULT_COLUMN_INFO_TABLE;
	}

	/**
	 * PhpMyAdmin export module constructor
	 * 
	 * @param hostname
	 *            the hostname of the MySQL server
	 * @param port
	 *            the port that the MySQL server is listening
	 * @param database
	 *            the name of the database to import from
	 * @param username
	 *            the name of the user to use in connection
	 * @param password
	 *            the password of the user to use in connection
	 * @param phpMyAdminDatabase
	 *            the name of the PhpMyAdmin database
	 * @param columnInfoTable
	 *            the name of the column_info table
	 */
	public PhpMyAdminExportModule(String hostname, int port, String database,
			String username, String password, String phpMyAdminDatabase,
			String columnInfoTable) {
		super(hostname, port, database, username, password);

		phpmyadmin_database = phpMyAdminDatabase;
		column_info_table = columnInfoTable;
	}

	public void initDatabase() throws ModuleException {
		try {
			logger.debug("Cleaning...");
			super.initDatabase();
			getConnection(phpmyadmin_database,
					createConnectionURL(phpmyadmin_database)).createStatement()
					.executeUpdate(
							"DELETE FROM " + column_info_table
									+ " WHERE db_name LIKE '" + database
									+ "\\_%'");
		} catch (SQLException e) {
			throw new ModuleException("Error creating database " + database, e);
		}
	}

	public Connection getConnection(String databaseName) throws ModuleException {
		Connection connection = null;
		if (!connections.containsKey(databaseName)) {
			try {
				super.getConnection(databaseName,
						createConnectionURL(databaseName));
			} catch (ModuleException e) {
				throw new ModuleException("Error getting connection", e);
			}
		} else {
			connection = connections.get(databaseName);
		}
		return connection;
	}

	public void handleStructure(DatabaseStructure structure)
			throws ModuleException, UnknownTypeException {
		super.handleStructure(structure);
		if (getStatement() != null) {
			logger.debug("Exporting columns info into PhpMyAdmin"
					+ " extended features database");
			try {
				Statement st = getConnection(phpmyadmin_database)
						.createStatement();
				List<PreparedStatement> statements = new ArrayList<PreparedStatement>();

				for (SchemaStructure schema : structure.getSchemas()) {
					for (TableStructure table : schema.getTables()) {
						for (ColumnStructure column : table.getColumns()) {
							Type type = column.getType();
							String comment = createColumnComment(
									column.getDescription(),
									type.getOriginalTypeName(),
									type.getDescription());
							String mimetype = "";
							String transformation = "";
							String transformation_options = "";
							if (type instanceof SimpleTypeBinary) {
								SimpleTypeBinary bin = (SimpleTypeBinary) type;
								if (bin.getFormatRegistryKey() != null
										&& bin.getFormatRegistryKey().equals(
												"MIME")) {
									mimetype = bin.getFormatRegistryName()
											.replace('/', '_');
									if (mimetype.equals("image_jpeg")) {
										transformation = "image_jpeg__link.inc.php";
									} else {
										transformation = "application_octetstream__download.inc.php";
									}

								} else {
									mimetype = "application_octet-stream";
									transformation = "application_octetstream__download.inc.php";
								}
							}

							statements.add(getInsertIntoPhpMyAdminStatement(
									schema.getName(), table.getName(),
									column.getName(), comment, mimetype,
									transformation, transformation_options));

						}
					}
				}
				st.executeBatch();
				st.clearBatch();

				for (PreparedStatement statement : statements) {
					statement.execute();
				}

			} catch (SQLException e) {
				throw new ModuleException(
						"Error inserting column info in PhpMyAdmin advanced features database",
						e);
			}
		}

	}

	protected String createColumnComment(String columnDescription,
			String originalType, String typeDescription) {
		String ret = "";
		if (columnDescription != null) {
			ret += "column description: " + columnDescription;
		}
		if (originalType != null) {
			ret += (ret.length() > 0 ? "; " : "");
			ret += "original type: " + originalType;
		}
		if (typeDescription != null) {
			ret += (ret.length() > 0 ? "; " : "");
			ret += "type description: " + typeDescription;
		}
		return ret;
	}

	protected PreparedStatement getInsertIntoPhpMyAdminStatement(String dbName,
			String tableName, String columnName, String comment,
			String mimetype, String transformation,
			String transformation_options) throws SQLException, ModuleException {

		// PreparedStatement ps = getConnection()
		PreparedStatement ps = getConnection(phpmyadmin_database)
				.prepareStatement(
						"INSERT INTO "
								+ phpmyadmin_database
								+ "."
								+ column_info_table
								+ "(db_name, table_name, column_name, comment, mimetype, transformation, transformation_options)"
								+ " VALUES(?, ?, ?, ?, ?, ?, ?)");

		ps.setString(1, dbName);
		ps.setString(2, tableName);
		ps.setString(3, columnName);
		ps.setString(4, comment);
		ps.setString(5, mimetype);
		ps.setString(6, transformation);
		ps.setString(7, transformation_options);

		return ps;
	}

	/**
	 * Check if a database exists
	 * 
	 * @param dbName
	 *            the name of the database
	 * @return true if exists, false otherwise
	 * @throws ModuleException
	 */
	public boolean databaseExists() throws ModuleException {
		boolean ret;
		try {
			ResultSet result = getConnection().createStatement().executeQuery(
					"SHOW DATABASES LIKE '" + database + "'");
			ret = result.next();
		} catch (SQLException e) {
			throw new ModuleException("Error checking if database " + database
					+ " exists", e);
		}
		return ret;
	}

	/**
	 * Check if a user exists
	 * 
	 * @param username
	 *            the user name
	 * @return true if user exists, false otherwise
	 * @throws ModuleException
	 */
	public boolean userExists(String username) throws ModuleException {
		boolean ret;
		try {
			ResultSet result = getConnection().createStatement().executeQuery(
					"SELECT User FROM mysql.user WHERE User =  '" + username
							+ "'");
			ret = result.next();
		} catch (SQLException e) {
			throw new ModuleException("Error checking if user " + username
					+ " exists", e);
		}
		return ret;
	}

	/**
	 * Set the user permissions for a database. If the user doesn't exist, it
	 * will be created. Permissions to other databases will be revoked.
	 * 
	 * @param username
	 *            the user name
	 * @param host
	 *            the host where the user is connecting from
	 * @param password
	 *            the user password
	 * @param database
	 *            the database name
	 * @throws ModuleException
	 */
	protected void setUserPermissions(String username, String host,
			String password, String database) throws ModuleException {
		try {
			Statement st = getConnection().createStatement();
			if (userExists(username)) {
				st.addBatch("GRANT SELECT, SHOW VIEW ON " + database
						+ ".* TO '" + username + "'@'" + host + "'");
			} else {
				st.addBatch("GRANT SELECT, SHOW VIEW ON " + database
						+ ".* TO '" + username + "'@'" + host
						+ "' IDENTIFIED BY '" + password + "'");
			}
			st.executeBatch();
			st.clearBatch();
		} catch (SQLException e) {
			throw new ModuleException("Error setting user '" + username
					+ "'permissions to database " + database, e);
		}
	}

	public void finishDatabase() throws ModuleException {
		super.finishDatabase();
		logger.debug("Setting guest permissions");
		setUserPermissions("guest", "localhost", "", database);
	}

}

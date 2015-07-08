package com.database_preservation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.log4j.Logger;

import com.database_preservation.model.exception.InvalidDataException;
import com.database_preservation.model.exception.ModuleException;
import com.database_preservation.model.exception.UnknownTypeException;
import com.database_preservation.modules.DatabaseHandler;
import com.database_preservation.modules.DatabaseImportModule;
import com.database_preservation.modules.db2.in.DB2JDBCImportModule;
import com.database_preservation.modules.db2.out.DB2JDBCExportModule;
import com.database_preservation.modules.dbml.in.DBMLImportModule;
import com.database_preservation.modules.dbml.out.DBMLExportModule;
import com.database_preservation.modules.msAccess.in.MsAccessUCanAccessImportModule;
import com.database_preservation.modules.mySql.in.MySQLJDBCImportModule;
import com.database_preservation.modules.mySql.out.MySQLJDBCExportModule;
import com.database_preservation.modules.mySql.out.PhpMyAdminExportModule;
import com.database_preservation.modules.oracle.in.Oracle12cJDBCImportModule;
import com.database_preservation.modules.postgreSql.in.PostgreSQLJDBCImportModule;
import com.database_preservation.modules.postgreSql.out.PostgreSQLJDBCExportModule;
import com.database_preservation.modules.siard.in.SIARDImportModule;
import com.database_preservation.modules.siard.out.SIARDExportModule;
import com.database_preservation.modules.sqlServer.in.SQLServerJDBCImportModule;
import com.database_preservation.modules.sqlServer.out.SQLServerJDBCExportModule;

/**
 * @author Luis Faria
 *
 */
public class Main {

	public static final String APP_NAME =
			"db-preservation-toolkit - KEEP SOLUTIONS";

	public static final String NAME = "db-preservation-toolkit";

	private static final Logger logger = Logger.getLogger(Main.class);


	/**
	 * @param args
	 *            the console arguments
	 */
	public static void main(String... args) {
		System.exit(internal_main(args));
	}

	protected static int internal_main(String... args) {
		List<String> importModuleArgs = new Vector<String>();
		List<String> exportModuleArgs = new Vector<String>();

		boolean parsingImportModule = false;
		boolean parsingExportModule = false;

		for (String arg : args) {
			if (arg.equals("-i")) {
				parsingImportModule = true;
				parsingExportModule = false;
			} else if (arg.equals("-o")) {
				parsingImportModule = false;
				parsingExportModule = true;
			} else if (parsingImportModule) {
				importModuleArgs.add(arg);
			} else if (parsingExportModule) {
				exportModuleArgs.add(arg);
			}
		}

		DatabaseImportModule importModule = null;
		DatabaseHandler exportModule = null;

		if (importModuleArgs.size() > 0) {
			importModule = getImportModule(importModuleArgs);
		}

		if (exportModuleArgs.size() > 0) {
			exportModule = getExportModule(exportModuleArgs);
		}

		int exitStatus = 1;
		if (importModule != null && exportModule != null) {
			try {
				long startTime = System.currentTimeMillis();
				logger.info("Translating database: "
						+ importModule.getClass().getSimpleName() + " to "
						+ exportModule.getClass().getSimpleName());
				importModule.getDatabase(exportModule);
				long duration = System.currentTimeMillis() - startTime;
				logger.info("Done in " + (duration / 60000) + "m "
						+ (duration % 60000 / 1000) + "s");
				exitStatus = 0;
			} catch (ModuleException e) {
				if (e.getCause() != null
						&& e.getCause() instanceof ClassNotFoundException
						&& e.getCause().getMessage()
								.equals("sun.jdbc.odbc.JdbcOdbcDriver")) {
					logger.error(
							"Could not find the Java ODBC driver, "
								+ "please run this program under Windows "
								+ "to use the JDBC-ODBC bridge.",
							e.getCause());
				} else if (e.getModuleErrors() != null) {
					for (Map.Entry<String, Throwable> entry : e
							.getModuleErrors().entrySet()) {
						logger.error(entry.getKey(), entry.getValue());
					}
				} else {
					logger.error("Error while importing/exporting", e);
				}
			} catch (UnknownTypeException e) {
				logger.error("Error while importing/exporting", e);
			} catch (InvalidDataException e) {
				logger.error("Error while importing/exporting", e);
			} catch (Exception e) {
				logger.error("Unexpected exception", e);
			}

		} else {
			printHelp();
			exitStatus = 0;
		}
		return exitStatus;
	}

	private static DatabaseImportModule getImportModule(
			List<String> importModuleArgs) {
		DatabaseImportModule importModule = null;
		if (importModuleArgs.get(0).equalsIgnoreCase("SQLServerJDBC")) {
			if (importModuleArgs.size() == 7) {
				importModule = new SQLServerJDBCImportModule(
						importModuleArgs.get(1), importModuleArgs.get(2),
						importModuleArgs.get(3), importModuleArgs.get(4),
						importModuleArgs.get(5).equals("true"),
						importModuleArgs.get(6).equals("true"));
			} else if (importModuleArgs.size() == 8) {
				try {
					importModule = new SQLServerJDBCImportModule(
							importModuleArgs.get(1), Integer.valueOf(
									importModuleArgs.get(2)).intValue(),
							importModuleArgs.get(3), importModuleArgs.get(4),
							importModuleArgs.get(5), importModuleArgs.get(6)
									.equals("true"), importModuleArgs.get(7)
									.equals("true"));
				} catch (NumberFormatException e) {
					importModule = new SQLServerJDBCImportModule(
							importModuleArgs.get(1), importModuleArgs.get(2),
							importModuleArgs.get(3), importModuleArgs.get(4),
							importModuleArgs.get(5), importModuleArgs.get(6)
									.equals("true"), importModuleArgs.get(7)
									.equals("true"));
				}
			} else {
				logger.error("Wrong argument number for "
						+ "SQLServerJDBC import module: "
						+ importModuleArgs.size());
			}
		} else if (importModuleArgs.get(0).equalsIgnoreCase("PostgreSQLJDBC")) {
			if (importModuleArgs.size() == 6) {
				importModule = new PostgreSQLJDBCImportModule(
						importModuleArgs.get(1), importModuleArgs.get(2),
						importModuleArgs.get(3), importModuleArgs.get(4),
						importModuleArgs.get(5).equals("true"));
			} else if (importModuleArgs.size() == 7) {
				importModule = new PostgreSQLJDBCImportModule(
						importModuleArgs.get(1), Integer.valueOf(
								importModuleArgs.get(2)).intValue(),
						importModuleArgs.get(3), importModuleArgs.get(4),
						importModuleArgs.get(5), importModuleArgs.get(6)
								.equals("true"));
			} else {
				logger.error("Wrong argument number for "
						+ "PostgreSQLJDBC import module: "
						+ importModuleArgs.size());
			}
		} else if (importModuleArgs.get(0).equalsIgnoreCase("DB2JDBC")) {
			if (importModuleArgs.size() == 6) {
				importModule = new DB2JDBCImportModule(importModuleArgs.get(1),
						Integer.valueOf(importModuleArgs.get(2)),
						importModuleArgs.get(3), importModuleArgs.get(4),
						importModuleArgs.get(5));
			} else {
				logger.error("Wrong argument number for DB2JDBC import module: "
						+ importModuleArgs.size());
				}
		} else if (importModuleArgs.get(0).equalsIgnoreCase("MySQLJDBC")) {
			if (importModuleArgs.size() == 5) {
				importModule = new MySQLJDBCImportModule(
						importModuleArgs.get(1), importModuleArgs.get(2),
						importModuleArgs.get(3), importModuleArgs.get(4));
			} else if (importModuleArgs.size() == 6) {
				importModule = new MySQLJDBCImportModule(
						importModuleArgs.get(1), Integer.valueOf(
								importModuleArgs.get(2)).intValue(),
						importModuleArgs.get(3), importModuleArgs.get(4),
						importModuleArgs.get(5));
			} else {
				logger.error("Wrong argument number for "
						+ "MySQLJDBC import module: " + importModuleArgs.size());
			}
		} else if (importModuleArgs.get(0).equals("DBML")) {
			if (importModuleArgs.size() == 2) {
				try {
					importModule = new DBMLImportModule(new File(
							importModuleArgs.get(1)));
				} catch (ModuleException e) {
					logger.error("Error creating DBML import module", e);
				}
			} else {
				logger.error("Wrong argument number for "
						+ "DBML import module: " + importModuleArgs.size());
			}
		} else if (importModuleArgs.get(0).equalsIgnoreCase("SIARD")) {
			if (importModuleArgs.size() == 2) {
				try {
					importModule = new SIARDImportModule(new File(
							importModuleArgs.get(1)));
				} catch (ModuleException e) {
					logger.error("Error creating SIARD import module", e);
				}
			} else {
				logger.error("Wrong argument number for "
						+ "SIARD import module: " + importModuleArgs.size());
			}
		} else if (importModuleArgs.get(0).equalsIgnoreCase("Oracle12cJDBC")) {
			if (importModuleArgs.size() == 6) {
				importModule = new Oracle12cJDBCImportModule(
						importModuleArgs.get(1), Integer.valueOf(
								importModuleArgs.get(2)).intValue(),
						importModuleArgs.get(3), importModuleArgs.get(4),
						importModuleArgs.get(5));
			} else {
				logger.error("Wrong argument number for "
						+ "Oracle12c import module: "
						+ importModuleArgs.size());
			}
//		} else if (importModuleArgs.get(0).equals("MSAccess")) {
//			if (importModuleArgs.size() == 2) {
//				importModule = new MsAccessImportModule(new File(
//						importModuleArgs.get(1)));
//			} else {
//				logger.error("Wrong argument number for "
//						+ "MSAccess import module: " + importModuleArgs.size());
//			}
		} else if (importModuleArgs.get(0).equals("MSAccessUCanAccess")) {
			if (importModuleArgs.size() == 2) {
				importModule = new MsAccessUCanAccessImportModule(new File(
						importModuleArgs.get(1)));
			} else {
				logger.error("Wrong argument number for "
						+ "MSAccessExp import module: "
						+ importModuleArgs.size());
			}
			//		} else if (importModuleArgs.get(0).equals("ODBC")) {
//			if (importModuleArgs.size() == 2) {
//				importModule = new ODBCImportModule(importModuleArgs.get(1));
//			} else if (importModuleArgs.size() == 4) {
//				importModule = new ODBCImportModule(importModuleArgs.get(1),
//						importModuleArgs.get(2), importModuleArgs.get(3));
//			} else {
//				logger.error("Wrong argument number for "
//						+ "ODBC import module: " + importModuleArgs.size());
//			}
		} else {
			logger.error("Unrecognized import module: "
					+ importModuleArgs.get(0));
		}
		return importModule;
	}

	private static DatabaseHandler getExportModule(List<String> exportModuleArgs) {
		DatabaseHandler exportModule = null;
		if (exportModuleArgs.get(0).equals("DBML")) {
			if (exportModuleArgs.size() == 2) {
				try {
					exportModule = new DBMLExportModule(new File(
							exportModuleArgs.get(1)));
				} catch (FileNotFoundException e) {
					logger.error("Could not find file for DBML export", e);
				} catch (UnsupportedEncodingException e) {
					logger.error("Unsupported encoding", e);
				}
			} else {
				logger.error("Wrong argument number for "
						+ "DBML export module: " + exportModuleArgs.size());
			}
		} else if (exportModuleArgs.get(0).equalsIgnoreCase("SIARD")) {
			if (exportModuleArgs.size() == 3) {
				try {
					exportModule = new SIARDExportModule(new File(
							exportModuleArgs.get(1)),
							exportModuleArgs.get(2).equals("compress"));
				} catch (FileNotFoundException e) {
					logger.error("Could not find file for SIARD export", e);
				}
			} else {
				logger.error("Wrong argument number for SIARD export module: "
						+ exportModuleArgs.size());
			}
		} else if (exportModuleArgs.get(0).equalsIgnoreCase("DB2JDBC")) {
			if (exportModuleArgs.size() == 6) {
				exportModule = new DB2JDBCExportModule(exportModuleArgs.get(1),
						Integer.valueOf(exportModuleArgs.get(2)),
						exportModuleArgs.get(3), exportModuleArgs.get(4),
						exportModuleArgs.get(5));
			} else {
				logger.error("Wrong argument number for DB2JDBC export module: "
						+ exportModuleArgs.size());
			}
		} else if (exportModuleArgs.get(0).equalsIgnoreCase("PostgreSQLJDBC")) {
			if (exportModuleArgs.size() == 6) {
				exportModule = new PostgreSQLJDBCExportModule(
						exportModuleArgs.get(1), exportModuleArgs.get(2),
						exportModuleArgs.get(3), exportModuleArgs.get(4),
						exportModuleArgs.get(5).equals("true"));
			} else if (exportModuleArgs.size() == 7) {
				exportModule = new PostgreSQLJDBCExportModule(
						exportModuleArgs.get(1), Integer.valueOf(
								exportModuleArgs.get(2)).intValue(),
						exportModuleArgs.get(3), exportModuleArgs.get(4),
						exportModuleArgs.get(5), exportModuleArgs.get(6)
								.equals("true"));
			} else {
				logger.error("Wrong argument number for "
						+ "PostgreSQLJDBC export module: "
						+ exportModuleArgs.size());
			}
//		} else if (exportModuleArgs.get(0).equals("PostgreSQLFile")) {
//			if (exportModuleArgs.size() == 2) {
//				try {
//					exportModule = new SQLFileExportModule(new File(
//							exportModuleArgs.get(1)), new PostgreSQLHelper());
//				} catch (ModuleException e) {
//					logger.error("Error creating PostgreSQLFile export module", e);
//				}
//			} else {
//				logger.error("Wrong argument number for "
//						+ "PostgreSQLFile export module: " + exportModuleArgs.size());
//			}
		} else if (exportModuleArgs.get(0).equalsIgnoreCase("MySQLJDBC")) {
			if (exportModuleArgs.size() == 5) {
				exportModule = new MySQLJDBCExportModule(
						exportModuleArgs.get(1), exportModuleArgs.get(2),
						exportModuleArgs.get(3), exportModuleArgs.get(4));
			} else if (exportModuleArgs.size() == 6) {
				exportModule = new MySQLJDBCExportModule(
						exportModuleArgs.get(1), Integer.valueOf(
								exportModuleArgs.get(2)).intValue(),
						exportModuleArgs.get(3), exportModuleArgs.get(4),
						exportModuleArgs.get(5));
			} else {
				logger.error("Wrong argument number for "
						+ "MySQLJDBC export module: " + exportModuleArgs.size());
			}
//		} else if (exportModuleArgs.get(0).equals("MySQLFile")) {
//			if (exportModuleArgs.size() == 2) {
//				try {
//					exportModule = new SQLFileExportModule(new File(
//							exportModuleArgs.get(1)), new MySQLHelper());
//				} catch (ModuleException e) {
//					logger.error("Error creating MySQLFile export module", e);
//				}
//
//			} else {
//				logger.error("Wrong argument number for "
//						+ "MySQLFile export module: " + exportModuleArgs.size());
//			}
		} else if (exportModuleArgs.get(0).equals("PhpMyAdmin")) {
			if (exportModuleArgs.size() == 5) {
				exportModule = new PhpMyAdminExportModule(
						exportModuleArgs.get(1), exportModuleArgs.get(2),
						exportModuleArgs.get(3), exportModuleArgs.get(4));
			} else if (exportModuleArgs.size() == 6) {
				exportModule = new PhpMyAdminExportModule(
						exportModuleArgs.get(1), Integer.valueOf(
								exportModuleArgs.get(2)).intValue(),
						exportModuleArgs.get(3), exportModuleArgs.get(4),
						exportModuleArgs.get(5));
			} else {
				logger.error("Wrong argument number for "
						+ "PhpMyAdmin export module: "
						+ exportModuleArgs.size());
			}
		} else if (exportModuleArgs.get(0).equalsIgnoreCase("SQLServerJDBC")) {
			if (exportModuleArgs.size() == 7) {
				exportModule = new SQLServerJDBCExportModule(
						exportModuleArgs.get(1), exportModuleArgs.get(2),
						exportModuleArgs.get(3), exportModuleArgs.get(4),
						exportModuleArgs.get(5).equals("true"),
						exportModuleArgs.get(6).equals("true"));
			} else if (exportModuleArgs.size() == 8) {
				try {
					exportModule = new SQLServerJDBCExportModule(
							exportModuleArgs.get(1), Integer.valueOf(
									exportModuleArgs.get(2)).intValue(),
							exportModuleArgs.get(3), exportModuleArgs.get(4),
							exportModuleArgs.get(5), exportModuleArgs.get(6)
									.equals("true"), exportModuleArgs.get(7)
									.equals("true"));
				} catch (NumberFormatException e) {
					exportModule = new SQLServerJDBCExportModule(
							exportModuleArgs.get(1), exportModuleArgs.get(2),
							exportModuleArgs.get(3), exportModuleArgs.get(4),
							exportModuleArgs.get(5), exportModuleArgs.get(6)
									.equals("true"), exportModuleArgs.get(7)
									.equals("true"));
				}
			} else {
				logger.error("Wrong argument number for "
						+ "SQLServerJDBC import module: "
						+ exportModuleArgs.size());
			}
//		} else if (exportModuleArgs.get(0).equals("SQLServerFile")) {
//			if (exportModuleArgs.size() == 2) {
//				try {
//					exportModule = new SQLFileExportModule(new File(
//							exportModuleArgs.get(1)), new SQLServerHelper());
//				} catch (ModuleException e) {
//					logger.error("Error creating SQLServerFile export module", e);
//				}
//
//			} else {
//				logger.error("Wrong argument number for "
//						+ "SQLServerFile export module: "
//						+ exportModuleArgs.size());
//			}
//		} else if (exportModuleArgs.get(0).equals("GenericSQLFile")) {
//			if (exportModuleArgs.size() == 2) {
//				try {
//					exportModule = new SQLFileExportModule(new File(
//							exportModuleArgs.get(1)), new SQLHelper());
//				} catch (ModuleException e) {
//					logger.error("Error creating GenericSQLFile export module", e);
//				}
//
//			} else {
//				logger.error("Wrong argument number for "
//						+ "GenericSQLFile export module: "
//						+ exportModuleArgs.size());
//			}
		} else {
			logger.error("Unrecognized export module: "
					+ exportModuleArgs.get(0));
		}
		return exportModule;
	}

	private static void printHelp() {
		System.out.println("Synopsys: java -jar " + NAME + ".jar"
				+ " -i IMPORT_MODULE [options...]"
				+ " -o EXPORT_MODULE [options...]");
		System.out.println("Available import modules:");
		System.out.println("\tSIARD dir compress|store");
		System.out
				.println("\tSQLServerJDBC serverName [port|instance] database username password useIntegratedSecurity encrypt");
		System.out
				.println("\tPostgreSQLJDBC hostName [port] database username password encrypt");
		System.out
				.println("\tMySQLJDBC hostName [port] database username password");
		System.out.println("\tDB2JDBC hostname port database username password");
		System.out
				.println("\tOracle12c hostName port database username password");
//		System.out.println("\tMSAccess database.mdb|accdb");
		System.out.println("\tMSAccessUCanAccess database.mdb|accdb");
		// System.out.println("\tODBC source [username password]");
		System.out.println("\tDBML baseDir");

		System.out.println("Available export modules:");
		System.out.println("\tSIARD dir");
		System.out
				.println("\tSQLServerJDBC serverName [port|instance] database username password useIntegratedSecurity encrypt");
		System.out
				.println("\tPostgreSQLJDBC [port] hostName database username password encrypt");
		System.out
				.println("\tMySQLJDBC hostName [port] database username password");
		System.out
				.println("\tDB2JDBC hostname port database username password");
		System.out
		 		.println("\tPhpMyAdmin hostName [port] database username password");
		System.out.println("\tDBML baseDir");
//		System.out
//				.println("\tPostgreSQLFile sqlFile <- SQL file optimized for PostgreSQL");
//		System.out
//				.println("\tMySQLFile sqlFile <- SQL file optimized for MySQL");
//		System.out
//				.println("\tSQLServerFile sqlFile <- SQL file optimized for SQL Server");
//		System.out.println("\tGenericSQLFile sqlFile <- generic SQL file");
	}

}

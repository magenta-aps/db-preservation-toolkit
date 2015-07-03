/**
 * 
 */
package com.database_preservation.modules.sqlFile.out;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Set;

import org.apache.log4j.Logger;

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
import com.database_preservation.model.structure.type.SimpleTypeNumericApproximate;
import com.database_preservation.model.structure.type.SimpleTypeNumericExact;
import com.database_preservation.modules.DatabaseHandler;
import com.database_preservation.modules.SQLHelper;
import com.database_preservation.modules.SQLHelper.CellSQLHandler;

/**
 * @author Luis Faria
 * 
 */
public class SQLFileExportModule implements DatabaseHandler {
	private static final Logger logger = Logger
			.getLogger(SQLFileExportModule.class);

	protected File sqlFile;

	protected FileOutputStream sqlOutput;

	protected BufferedWriter sqlWriter;

	protected SQLHelper sqlHelper;

	protected DatabaseStructure structure;

	protected TableStructure currentTable;

	/**
	 * Create a new SQLFile export module, specifying the SQL helper to use
	 * 
	 * @param sqlFile
	 *            the file where to dump the SQL
	 * @param sqlHelper
	 *            the SQL helper
	 * @throws ModuleException
	 */
	public SQLFileExportModule(File sqlFile, SQLHelper sqlHelper)
			throws ModuleException {
		this.sqlFile = sqlFile;
		this.sqlHelper = sqlHelper;
		try {
			sqlOutput = new FileOutputStream(sqlFile);
			sqlWriter = new BufferedWriter(new OutputStreamWriter(sqlOutput));
		} catch (FileNotFoundException e) {
			throw new ModuleException("Error creating output writer", e);
		}
		structure = null;
		currentTable = null;
	}

	public void initDatabase() throws ModuleException {
		// nothing to do
	}

	public void setIgnoredSchemas(Set<String> ignoredSchemas) {
		// nothing to do
	}
	
	public void handleStructure(DatabaseStructure structure)
			throws ModuleException, UnknownTypeException {
		try {
			this.structure = structure;
			for (SchemaStructure schema : structure.getSchemas()) {
				for (TableStructure table : schema.getTables()) {
					sqlWriter.write(sqlHelper.createTableSQL(table) + ";\n");
					String pkeySQL = sqlHelper.createPrimaryKeySQL(
							table.getId(), table.getPrimaryKey());
					logger.debug("PKEY: " + sqlHelper.createPrimaryKeySQL(
							table.getId(), table.getPrimaryKey()));
					if (pkeySQL != null) {
						sqlWriter.write(pkeySQL + ";\n");
					}
				}
			}
			sqlWriter.flush();
		} catch (IOException e) {
			throw new ModuleException("Error while handling structure", e);
		}
	}

	public void handleDataOpenTable(String tableId) throws ModuleException {
		if (structure != null) {
			currentTable = structure.lookupTableStructure(tableId);
		} else {
			throw new ModuleException("Table " + tableId
					+ " opened before struture was defined");
		}
	}

	public void handleDataRow(Row row) throws InvalidDataException,
			ModuleException {
		if (currentTable != null) {
			byte[] rowSQL = sqlHelper.createRowSQL(currentTable, row,
					new CellSQLHandler() {

						public byte[] createCellSQL(Cell cell,
								ColumnStructure column)
								throws InvalidDataException, ModuleException {
							byte[] ret;
							if (cell instanceof SimpleCell) {
								SimpleCell simple = (SimpleCell) cell;
								if (simple.getSimpledata() == null) {
									ret = "NULL".getBytes();
								} else if (column.getType() instanceof SimpleTypeNumericExact
										|| column.getType() instanceof SimpleTypeNumericApproximate) {
									ret = simple.getSimpledata().getBytes();
								} else {
									ret = (escapeString(simple.getSimpledata()))
											.getBytes();
								}

							} else if (cell instanceof BinaryCell) {
								BinaryCell bin = (BinaryCell) cell;
								ByteArrayOutputStream bout = new ByteArrayOutputStream();
								try {
									escapeBinary(bin.getInputstream(), bout);
								} catch (IOException e) {
									throw new ModuleException(
											"Error getting binary from binary cell",
											e);
								}

								ret = bout.toByteArray();
								bin.cleanResources();

							} else if (cell instanceof ComposedCell) {
								throw new ModuleException(
										"Composed cell export not yet supported");
							} else {
								throw new ModuleException(cell.getClass()
										.getSimpleName()
										+ " not supported");
							}
							return ret;
						}

					});
			byte[] rowSQLCollon = new byte[rowSQL.length + 2];
			System.arraycopy(rowSQL, 0, rowSQLCollon, 0, rowSQL.length);
			System.arraycopy(";\n".getBytes(), 0, rowSQLCollon, rowSQL.length,
					2);
			try {
				sqlOutput.write(rowSQLCollon);
			} catch (IOException e) {
				throw new ModuleException("Error writing row to file", e);
			}
		}
	}

	public void handleDataCloseTable(String tableId) throws ModuleException {
		currentTable = null;
	}

	public void finishDatabase() throws ModuleException {
		for (SchemaStructure schema : structure.getSchemas()) {
			for (TableStructure table : schema.getTables()) {
				for (ForeignKey fkey : table.getForeignKeys()) {
					try {
						String fkeySQL = 
								sqlHelper.createForeignKeySQL(table, fkey);
						sqlWriter.write(fkeySQL + ";\n");
						sqlWriter.flush();
					} catch (IOException e) {
						throw new ModuleException("Error writing foreign key: "
								+ fkey, e);
					}
				}
			}
		}		
	}

	/**
	 * Encode a string with escapes:
	 * <ul>
	 * <li>'\\': '\\\\'</li>
	 * </ul>
	 * 
	 * @param string
	 *            the original string
	 * @return the escaped string
	 */
	public static String escapeString(String string) {
		String ret = string;
		ret = ret.replaceAll("\\\\", "\\\\\\\\");
		ret = ret.replaceAll("'", "''");
		return ret.equals(string) ? "'" + ret + "'" : "E'" + ret + "'";
	}

	/**
	 * Escape string literal
	 * 
	 * @param in
	 *            the original string input stream
	 * @param out
	 *            the escaped string output stream
	 * @throws IOException
	 */
	public static void escapeStringLiteral(InputStream in, OutputStream out)
			throws IOException {
		// BufferedInputStream buffin = new BufferedInputStream(in);
		// BufferedOutputStream buffout = new BufferedOutputStream(out);
 
		out.write("E'".getBytes());

		int ibyte = in.read();
		while (ibyte != -1) {
			switch (ibyte) {
			case '\'':
				out.write("''".getBytes());
				break;
			case '\\':
				out.write("\\\\".getBytes());
				break;
			default:
				if (ibyte > 0 && ibyte < 31 || ibyte > 127 && ibyte <= 255) {
					out.write(("\\" + ibyte).getBytes());
				} else {
					out.write(ibyte);
				}
				break;
			}

			ibyte = in.read();
		}

		out.write("'".getBytes());
	}

	/**
	 * Encode binary with escapes:
	 * <ul>
	 * <li>'\000': "\\000"</li>
	 * <li>'\'': '\\\''</li>
	 * <li>'\\': '\\\\'</li>
	 * <li>0-31 and 127-255: \xxx</li>
	 * </ul>
	 * 
	 * @param in
	 *            the binary input stream where to read the original binary
	 * @param out
	 *            the binary output stream where to write the encoded binary
	 * @throws IOException
	 */
	public static void escapeBinary(InputStream in, OutputStream out)
			throws IOException {

		BufferedInputStream bin = new BufferedInputStream(in);
		BufferedOutputStream bout = new BufferedOutputStream(out);

		bout.write("E'".getBytes());
		int ibyte = bin.read();
		while (ibyte != -1) {

			switch (ibyte) {
			case 0:
				bout.write("\\\\000".getBytes());
				break;
			case '\'':
				bout.write("''''".getBytes());
				break;
			case '\\':
				bout.write("\\\\\\\\".getBytes());
				break;
			default:
				if (ibyte > 0 && ibyte < 31 || ibyte > 127 && ibyte <= 255) {
					bout.write(("\\\\" + ibyte).getBytes());
				} else {
					bout.write(ibyte);
				}
				break;
			}

			ibyte = bin.read();
		}
		bout.write("'::bytea".getBytes());
		bout.flush();

	}

	/**
	 * Escape binary and then escape string literal
	 * 
	 * @param in
	 *            the binary input stream
	 * @param out
	 *            the doubble escaped binary output stream
	 * @throws IOException
	 * @deprecated use escape binary instead
	 */
	public static void escapeBinaryAsStringLiteral(final InputStream in,
			final OutputStream out) throws IOException {
		final PipedInputStream bin = new PipedInputStream();
		final PipedOutputStream bout = new PipedOutputStream(bin);

		Runnable writerRunnable = new Runnable() {
			public void run() {
				try {
					escapeBinary(in, bout);
					bout.close();
				} catch (IOException e) {
					logger.error("Error escaping binary into circular buffer",
							e);
				}
			}
		};
		new Thread(writerRunnable).start();
		escapeStringLiteral(bin, out);
	}

}

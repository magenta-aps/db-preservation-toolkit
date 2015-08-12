package com.databasepreservation.modules.siard.out.output;

import com.databasepreservation.model.data.Row;
import com.databasepreservation.model.exception.InvalidDataException;
import com.databasepreservation.model.exception.ModuleException;
import com.databasepreservation.model.exception.UnknownTypeException;
import com.databasepreservation.model.structure.DatabaseStructure;
import com.databasepreservation.model.structure.SchemaStructure;
import com.databasepreservation.model.structure.TableStructure;
import com.databasepreservation.modules.DatabaseHandler;
import com.databasepreservation.modules.siard.out.content.ContentExportStrategy;
import com.databasepreservation.modules.siard.out.metadata.MetadataExportStrategy;
import com.databasepreservation.modules.siard.common.SIARDArchiveContainer;
import com.databasepreservation.modules.siard.out.write.WriteStrategy;

import java.util.Set;

/**
 * @author Bruno Ferreira <bferreira@keep.pt>
 */
public class SIARDExportDefault implements DatabaseHandler {
	private final SIARDArchiveContainer mainContainer;
	private final WriteStrategy writeStrategy;
	private final MetadataExportStrategy metadataStrategy;
	private final ContentExportStrategy contentStrategy;

	private DatabaseStructure dbStructure;
	private SchemaStructure currentSchema;
	private TableStructure currentTable;

	public SIARDExportDefault(ContentExportStrategy contentStrategy, SIARDArchiveContainer mainContainer,
							  WriteStrategy writeStrategy, MetadataExportStrategy metadataStrategy) {
		this.contentStrategy = contentStrategy;
		this.mainContainer = mainContainer;
		this.writeStrategy = writeStrategy;
		this.metadataStrategy = metadataStrategy;
	}

	@Override
	public void initDatabase() throws ModuleException {
		writeStrategy.setup(mainContainer);
	}

	@Override
	public void setIgnoredSchemas(Set<String> ignoredSchemas) {
		// nothing to do
	}

	@Override
	public void handleStructure(DatabaseStructure structure) throws ModuleException, UnknownTypeException {
		if (structure == null) {
			throw new ModuleException("Database structure must not be null");
		}

		dbStructure = structure;
	}

	@Override
	public void handleDataOpenTable(String schemaName, String tableId) throws ModuleException {
		currentSchema = dbStructure.getSchemaByName(schemaName);
		currentTable = dbStructure.lookupTableStructure(tableId);

		if (currentSchema == null) {
			throw new ModuleException("Couldn't find schema with name: " + schemaName);
		}

		if (currentTable == null) {
			throw new ModuleException("Couldn't find table with id: " + tableId);
		}

		contentStrategy.openTable(currentSchema, currentTable);
	}

	@Override
	public void handleDataCloseTable(String schemaName, String tableId) throws ModuleException {
		currentSchema = dbStructure.getSchemaByName(schemaName);
		currentTable = dbStructure.lookupTableStructure(tableId);

		if (currentSchema == null) {
			throw new ModuleException("Couldn't find schema with name: " + schemaName);
		}

		if (currentTable == null) {
			throw new ModuleException("Couldn't find table with id: " + tableId);
		}

		contentStrategy.closeTable(currentSchema, currentTable);
	}

	@Override
	public void handleDataRow(Row row) throws InvalidDataException, ModuleException {
		contentStrategy.tableRow(row);
	}

	@Override
	public void finishDatabase() throws ModuleException {
		metadataStrategy.writeMetadataXML(dbStructure, mainContainer);
		metadataStrategy.writeMetadataXSD(dbStructure, mainContainer);
		writeStrategy.finish(mainContainer);
	}
}
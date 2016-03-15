package com.databasepreservation.modules.listTables;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import com.databasepreservation.model.data.Row;
import com.databasepreservation.model.exception.InvalidDataException;
import com.databasepreservation.model.exception.ModuleException;
import com.databasepreservation.model.exception.UnknownTypeException;
import com.databasepreservation.model.modules.DatabaseExportModule;
import com.databasepreservation.model.modules.ModuleSettings;
import com.databasepreservation.model.structure.ColumnStructure;
import com.databasepreservation.model.structure.DatabaseStructure;
import com.databasepreservation.model.structure.SchemaStructure;
import com.databasepreservation.model.structure.TableStructure;
import com.databasepreservation.modules.siard.constants.SIARDDKConstants;

/**
 * @author Andreas Kring <andreas@magenta.dk>
 *
 */
public class ListCLOBs implements DatabaseExportModule {

  private final String SEPARATOR = ".";

  private Path outputFile;
  private OutputStream outputStream;
  private OutputStreamWriter writer;
  private DatabaseStructure dbStructure;
  private SchemaStructure currentSchema;
  private TableStructure currentTable;

  public ListCLOBs(Path outputFile) {
    this.outputFile = outputFile;
  }

  @Override
  public ModuleSettings getModuleSettings() throws ModuleException {
    return new ModuleSettings() {
      @Override
      public boolean shouldFetchRows() {
        return false;
      }
    };
  }

  @Override
  public void initDatabase() throws ModuleException {
    try {
      outputStream = Files.newOutputStream(outputFile);
      writer = new OutputStreamWriter(outputStream, "UTF8");
      System.out.println("writer created");
    } catch (IOException e) {
      throw new ModuleException("Could not create file " + outputFile.toAbsolutePath().toString(), e);
    }
  }

  @Override
  public void setIgnoredSchemas(Set<String> ignoredSchemas) {
    // Nothing to do
  }

  @Override
  public void handleStructure(DatabaseStructure structure) throws ModuleException, UnknownTypeException {
    if (structure == null) {
      throw new ModuleException("Database structure must not be null");
    }
    dbStructure = structure;
  }

  @Override
  public void handleDataOpenSchema(String schemaName) throws ModuleException {
    currentSchema = dbStructure.getSchemaByName(schemaName);

    if (currentSchema == null) {
      throw new ModuleException("Couldn't find schema with name: " + schemaName);
    }
  }

  @Override
  public void handleDataOpenTable(String tableId) throws ModuleException {
    try {
      currentTable = dbStructure.lookupTableStructure(tableId);
      if (currentTable == null) {
        throw new ModuleException("Couldn't find table with id: " + tableId);
      }

      for (ColumnStructure columnStructure : currentTable.getColumns()) {
        if (columnStructure.getType().getSql99TypeName().equals(SIARDDKConstants.CHARACTER_LARGE_OBJECT)) {
          String clobLocation = new StringBuilder().append(currentSchema.getName()).append(SEPARATOR)
            .append(currentTable.getName()).append(SEPARATOR).append(columnStructure.getName()).toString();
          System.out.println(clobLocation);
          writer.append(clobLocation).append("\n");
        }
      }

    } catch (IOException e) {
      throw new ModuleException("Could not write to file (" + outputFile.toAbsolutePath().toString() + ")", e);
    }
  }

  @Override
  public void handleDataRow(Row row) throws InvalidDataException, ModuleException {
    // Nothing to do
  }

  @Override
  public void handleDataCloseTable(String tableId) throws ModuleException {
    // Nothing to do
  }

  @Override
  public void handleDataCloseSchema(String schemaName) throws ModuleException {
    // Nothing to do
  }

  @Override
  public void finishDatabase() throws ModuleException {
    try {
      writer.close();
    } catch (IOException e) {
      throw new ModuleException("Could not close file writer stream (file: " + outputFile.toAbsolutePath().toString()
        + ")", e);
    }

    try {
      outputStream.close();
    } catch (IOException e) {
      throw new ModuleException("Could not close file stream (file: " + outputFile.toAbsolutePath().toString() + ")", e);
    }
  }
}

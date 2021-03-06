package com.databasepreservation.modules.siard.in.input;

import com.databasepreservation.common.ObservableModule;
import com.databasepreservation.model.Reporter;
import com.databasepreservation.model.exception.InvalidDataException;
import com.databasepreservation.model.exception.ModuleException;
import com.databasepreservation.model.exception.UnknownTypeException;
import com.databasepreservation.model.modules.DatabaseExportModule;
import com.databasepreservation.model.modules.DatabaseImportModule;
import com.databasepreservation.model.modules.ModuleSettings;
import com.databasepreservation.model.structure.DatabaseStructure;
import com.databasepreservation.modules.siard.common.SIARDArchiveContainer;
import com.databasepreservation.modules.siard.in.content.ContentImportStrategy;
import com.databasepreservation.modules.siard.in.metadata.MetadataImportStrategy;
import com.databasepreservation.modules.siard.in.read.ReadStrategy;

/**
 * @author Bruno Ferreira <bferreira@keep.pt>
 */
public class SIARDImportDefault extends ObservableModule implements DatabaseImportModule {
  private final ReadStrategy readStrategy;
  private final SIARDArchiveContainer mainContainer;
  private final ContentImportStrategy contentStrategy;
  private final MetadataImportStrategy metadataStrategy;
  private ModuleSettings moduleSettings;
  private Reporter reporter;

  public SIARDImportDefault(ContentImportStrategy contentStrategy, SIARDArchiveContainer mainContainer,
    ReadStrategy readStrategy, MetadataImportStrategy metadataStrategy) {
    this.readStrategy = readStrategy;
    this.mainContainer = mainContainer;
    this.contentStrategy = contentStrategy;
    this.metadataStrategy = metadataStrategy;
  }

  @Override
  public void getDatabase(DatabaseExportModule handler) throws ModuleException, UnknownTypeException,
    InvalidDataException {
    moduleSettings = handler.getModuleSettings();
    readStrategy.setup(mainContainer);
    handler.initDatabase();
    notifyOpenDatabase();
    try {
      metadataStrategy.loadMetadata(readStrategy, mainContainer, moduleSettings);

      DatabaseStructure dbStructure = metadataStrategy.getDatabaseStructure();

      // handler.setIgnoredSchemas(null);

      handler.handleStructure(dbStructure);
      notifyStructureObtained(dbStructure);

      contentStrategy.importContent(handler, mainContainer, dbStructure, moduleSettings, (ObservableModule) this);

      notifyCloseDatabase(dbStructure);
      handler.finishDatabase();
    } finally {
      readStrategy.finish(mainContainer);
    }
  }

  /**
   * Provide a reporter through which potential conversion problems should be
   * reported. This reporter should be provided only once for the export module
   * instance.
   *
   * @param reporter
   *          The initialized reporter instance.
   */
  @Override
  public void setOnceReporter(Reporter reporter) {
    this.reporter = reporter;
    metadataStrategy.setOnceReporter(reporter);
  }
}

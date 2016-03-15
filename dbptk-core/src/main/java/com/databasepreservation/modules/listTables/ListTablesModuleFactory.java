package com.databasepreservation.modules.listTables;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.naming.OperationNotSupportedException;

import org.apache.commons.lang3.StringUtils;

import com.databasepreservation.model.exception.LicenseNotAcceptedException;
import com.databasepreservation.model.modules.DatabaseExportModule;
import com.databasepreservation.model.modules.DatabaseImportModule;
import com.databasepreservation.model.modules.DatabaseModuleFactory;
import com.databasepreservation.model.parameters.Parameter;
import com.databasepreservation.model.parameters.Parameters;

/**
 * Exposes an export module that produces a list of tables contained in the
 * database. This list can then be used by other modules (e.g. the SIARD2 export
 * module) to specify the tables that should be processed.
 *
 * @author Bruno Ferreira <bferreira@keep.pt>
 */
public class ListTablesModuleFactory implements DatabaseModuleFactory {

  private static final String TABLES = "tables";
  private static final String CLOBS = "clobs";

  private static final Parameter file = new Parameter().shortName("f").longName("file")
    .description("Path to output file that can be read by SIARD2 export module").hasArgument(true)
    .setOptionalArgument(false).required(true);

  private static final Parameter listType = new Parameter().shortName("t").longName("type")
    .description("Set to 'tables' (default) or 'clobs'").hasArgument(true).setOptionalArgument(false).required(false)
    .valueIfNotSet(TABLES);

  @Override
  public boolean producesImportModules() {
    return false;
  }

  @Override
  public boolean producesExportModules() {
    return true;
  }

  @Override
  public String getModuleName() {
    return "list-tables";
  }

  @Override
  public Map<String, Parameter> getAllParameters() {
    HashMap<String, Parameter> parameterHashMap = new HashMap<String, Parameter>();
    parameterHashMap.put(file.longName(), file);
    parameterHashMap.put(listType.longName(), listType);
    return parameterHashMap;
  }

  @Override
  public Parameters getImportModuleParameters() throws OperationNotSupportedException {
    throw DatabaseModuleFactory.ExceptionBuilder.OperationNotSupportedExceptionForImportModule();
  }

  @Override
  public Parameters getExportModuleParameters() throws OperationNotSupportedException {
    return new Parameters(Arrays.asList(file, listType), null);
  }

  @Override
  public DatabaseImportModule buildImportModule(Map<Parameter, String> parameters)
    throws OperationNotSupportedException, LicenseNotAcceptedException {
    throw DatabaseModuleFactory.ExceptionBuilder.OperationNotSupportedExceptionForImportModule();
  }

  @Override
  public DatabaseExportModule buildExportModule(Map<Parameter, String> parameters)
    throws OperationNotSupportedException, LicenseNotAcceptedException {
    Path pFile = Paths.get(parameters.get(file));

    String pListType = null;
    if (StringUtils.isNoneBlank(parameters.get(listType))) {
      pListType = parameters.get(listType);
    } else {
      pListType = listType.valueIfNotSet();
    }

    if (pListType.equals(TABLES)) {
      return new ListTables(pFile);
    } else if (pListType.equals(CLOBS)) {
      return new ListCLOBs(pFile);
    } else {
      // TODO: what is the proper way to handle this?
      return null;
    }

  }
}

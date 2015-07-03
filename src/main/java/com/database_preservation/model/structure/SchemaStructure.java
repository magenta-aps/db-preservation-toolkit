package com.database_preservation.model.structure;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author Miguel Coutada
 *
 */

public class SchemaStructure {
	
	private String name;
	
	private String folder;
	
	private String description;
	
	private List<TableStructure> tables;
	
	private List<ViewStructure> views;
	
	private List<RoutineStructure> routines;
	
	
	public SchemaStructure() {
		tables = new ArrayList<TableStructure>();
		views = new ArrayList<ViewStructure>();
		routines = new ArrayList<RoutineStructure>();
	}

	/**
	 * @param name
	 * @param folder
	 * @param description
	 * @param tables
	 * @param views
	 * @param routines
	 */
	public SchemaStructure(String name, String folder, String description,
			List<TableStructure> tables, List<ViewStructure> views,
			List<RoutineStructure> routines) {
		this.name = name;
		this.folder = folder;
		this.description = description;
		this.tables = tables;
		this.views = views;
		this.routines = routines;
	}


	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}


	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}


	/**
	 * @return the folder
	 */
	public String getFolder() {
		return folder;
	}
	

	/**
	 * @param folder the folder to set
	 */
	public void setFolder(String folder) {
		folder = folder.replaceAll("\\s+","-");
		this.folder = folder;
	}
	

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}


	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}



	/**
	 * @return the tables
	 */
	public List<TableStructure> getTables() {
		return tables;
	}



	/**
	 * @param tables the tables to set
	 */
	public void setTables(List<TableStructure> tables) {
		this.tables = tables;
	}


	/**
	 * @return the views
	 */
	public List<ViewStructure> getViews() {
		return views;
	}
	

	/**
	 * @param views the views to set
	 */
	public void setViews(List<ViewStructure> views) {
		this.views = views;
	}


	/**
	 * @return the routines
	 */
	public List<RoutineStructure> getRoutines() {
		return routines;
	}


	/**
	 * @param routines the routines to set
	 */
	public void setRoutines(List<RoutineStructure> routines) {
		this.routines = routines;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {		
		StringBuilder builder = new StringBuilder();
		builder.append("\n****** SCHEMA: " + name + " ******");
		builder.append("\n");
		builder.append("folder : ");
		builder.append(folder);
		builder.append("\n");
		builder.append("description=");
		builder.append(description);
		builder.append("\n");
		builder.append("tables=");
		builder.append(tables);
		builder.append("\n");
		builder.append("views=");
		builder.append(views);
		builder.append("\n");
		builder.append("routines=");
		builder.append(routines);
		builder.append("\n");
		builder.append("****** END SCHEMA ******");
		builder.append("\n");
		return builder.toString();
	}
	
}

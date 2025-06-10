package simpledb;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Catalog keeps track of all available tables in the database and their
 * associated schemas.
 * For now, this is a stub catalog that must be populated with tables by a
 * user program before it can be used -- eventually, this should be converted
 * to a catalog that reads a catalog table from disk.
 *
 * @Threadsafe
 */
public class Catalog {

    // A class to store metadata about a table.
    private static class Table {
        public final DbFile file;       // The DbFile associated with the table
        public final String name;       // The name of the table
        public final String primaryKey; // The primary key of the table

        public Table(DbFile file, String name, String primaryKey) {
            this.file = file;
            this.name = name;
            this.primaryKey = primaryKey;
        }
    }

    // Maps table IDs to Table objects
    private final ConcurrentHashMap<Integer, Table> tableById;
    // Maps table names to their respective IDs
    private final ConcurrentHashMap<String, Integer> tableNameToId;

    /**
     * Constructor.
     * Creates a new, empty catalog.
     */
    public Catalog() {
        // Initialize maps to hold table metadata
        tableById = new ConcurrentHashMap<>();
        tableNameToId = new ConcurrentHashMap<>();
    }

    /**
     * Add a new table to the catalog.
     * If a table with the same name already exists, it is replaced.
     *
     * @param file       the DbFile storing the contents of the table
     * @param name       the name of the table
     * @param pkeyField  the primary key field for the table
     */
    public void addTable(DbFile file, String name, String pkeyField) {
        // Validate the inputs
        if (file == null || name == null) {
            throw new IllegalArgumentException("DbFile and name cannot be null");
        }

        int tableId = file.getId();

        // Add or replace the table in both maps
        tableById.put(tableId, new Table(file, name, pkeyField));
        tableNameToId.put(name, tableId);
    }

    /**
     * Overloaded method to add a table with no primary key field.
     */
    public void addTable(DbFile file, String name) {
        // Use an empty string as the default primary key
        addTable(file, name, "");
    }

    /**
     * Overloaded method to add a table with a generated name.
     */
    public void addTable(DbFile file) {
        // Use a random UUID as the default table name
        addTable(file, UUID.randomUUID().toString());
    }

    /**
     * Return the ID of the table with the specified name.
     *
     * @param name  the name of the table
     * @return the table ID
     * @throws NoSuchElementException if the table does not exist
     */
    public int getTableId(String name) throws NoSuchElementException {
        // Check if the table name exists in the catalog
        if (name == null || !tableNameToId.containsKey(name)) {
            throw new NoSuchElementException("Table " + name + " does not exist.");
        }
        return tableNameToId.get(name);
    }

    /**
     * Return the schema (TupleDesc) of the specified table.
     *
     * @param tableId  the ID of the table
     * @return the TupleDesc of the table
     * @throws NoSuchElementException if the table does not exist
     */
    public TupleDesc getTupleDesc(int tableId) throws NoSuchElementException {
        // Validate the table ID and return its schema
        if (!tableById.containsKey(tableId)) {
            throw new NoSuchElementException("Table ID " + tableId + " does not exist.");
        }
        return tableById.get(tableId).file.getTupleDesc();
    }

    /**
     * Return the DbFile associated with the specified table.
     *
     * @param tableId  the ID of the table
     * @return the DbFile of the table
     * @throws NoSuchElementException if the table does not exist
     */
    public DbFile getDatabaseFile(int tableId) throws NoSuchElementException {
        // Validate the table ID and return its DbFile
        if (!tableById.containsKey(tableId)) {
            throw new NoSuchElementException("Table ID " + tableId + " does not exist.");
        }
        return tableById.get(tableId).file;
    }

    /**
     * Return the primary key field of the specified table.
     *
     * @param tableId  the ID of the table
     * @return the primary key field
     */
    public String getPrimaryKey(int tableId) {
        // Retrieve the primary key of the table
        if (!tableById.containsKey(tableId)) {
            throw new NoSuchElementException("Table ID " + tableId + " does not exist.");
        }
        return tableById.get(tableId).primaryKey;
    }

    /**
     * Return an iterator over all table IDs.
     *
     * @return an iterator over table IDs
     */
    public Iterator<Integer> tableIdIterator() {
        // Provide an iterator over the set of table IDs
        return tableById.keySet().iterator();
    }

    /**
     * Return the name of the table with the specified ID.
     *
     * @param tableId  the ID of the table
     * @return the name of the table
     */
    public String getTableName(int tableId) {
        // Retrieve the table name using its ID
        if (!tableById.containsKey(tableId)) {
            throw new NoSuchElementException("Table ID " + tableId + " does not exist.");
        }
        return tableById.get(tableId).name;
    }

    /**
     * Delete all tables from the catalog.
     */
    public void clear() {
        // Clear both maps to remove all catalog entries
        tableById.clear();
        tableNameToId.clear();
    }

    /**
     * Reads the schema from a file and creates the appropriate tables in the database.
     *
     * @param catalogFile  the file containing schema information
     */
    public void loadSchema(String catalogFile) {
        String line = "";
        String baseFolder = new File(new File(catalogFile).getAbsolutePath()).getParent();

        try (BufferedReader br = new BufferedReader(new FileReader(new File(catalogFile)))) {
            while ((line = br.readLine()) != null) {
                // Parse the line to extract table name and fields
                String name = line.substring(0, line.indexOf("(")).trim();
                String fields = line.substring(line.indexOf("(") + 1, line.indexOf(")")).trim();
                String[] els = fields.split(",");
                ArrayList<String> fieldNames = new ArrayList<>();
                ArrayList<Type> fieldTypes = new ArrayList<>();
                String primaryKey = "";

                for (String e : els) {
                    String[] parts = e.trim().split(" ");
                    fieldNames.add(parts[0].trim());
                    if (parts[1].trim().equalsIgnoreCase("int")) {
                        fieldTypes.add(Type.INT_TYPE);
                    } else if (parts[1].trim().equalsIgnoreCase("string")) {
                        fieldTypes.add(Type.STRING_TYPE);
                    } else {
                        throw new IOException("Unknown field type " + parts[1]);
                    }
                    if (parts.length == 3 && parts[2].trim().equalsIgnoreCase("pk")) {
                        primaryKey = parts[0].trim();
                    }
                }

                // Create TupleDesc and HeapFile for the table
                TupleDesc td = new TupleDesc(fieldTypes.toArray(new Type[0]), fieldNames.toArray(new String[0]));
                HeapFile hf = new HeapFile(new File(baseFolder + "/" + name + ".dat"), td);

                // Add the table to the catalog
                addTable(hf, name, primaryKey);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error loading schema from file: " + catalogFile);
        }
    }
}

# SimpleDB - Database Project

This project is a simple relational database implementation in Java, developed as a laboratory project for a database systems course.

## Project Structure

- `src/java/simpledb/` : Main database source code
- `bin/` : Compiled binary files
- `lib/` : External libraries
- `test/` : Unit tests
- `build.xml` : Apache Ant build configuration

## Main Features

- Transaction management
- Basic SQL operators (SELECT, INSERT, DELETE)
- B+ Tree indexing structure
- Buffer Pool management
- Database file management system

## Compilation and Execution

The project uses Apache Ant for compilation. To compile and run the project:

```bash
ant compile    # Compile the project
ant run        # Run the program
```

## Basic Commands

The main program ([SimpleDb.java](cci:7://file:///c:/Users/youri/OneDrive/Bureau/a_mettre_dans_git/DBSYS/Lab2-18/Lab2-18/src/java/simpledb/SimpleDb.java:0:0-0:0)) supports the following commands:

- `convert` : Converts a text file to binary format
  ```bash
  java simpledb.SimpleDb convert file.txt number_of_fields
  ```

## Main Classes

- `simpledb.SimpleDb` : Main program entry point
- `simpledb.Database` : Database manager
- `simpledb.BufferPool` : Buffer Pool manager
- `simpledb.BTreeFile` : B+ Tree index implementation
- `simpledb.Insert` : Tuple insertion operator
- `simpledb.Delete` : Tuple deletion operator

## File Structure

- Data files are stored in binary format (.dat)
- Text files (.txt) can be converted to binary format
- B+ Tree structure is used for efficient indexing

## Prerequisites

- Java JDK
- Apache Ant

## Testing

Unit tests are located in the `test/` directory and can be run using Ant.

## Documentation

For more details on implementation and architecture, consult the included [Report.pdf](cci:7://file:///c:/Users/youri/OneDrive/Bureau/a_mettre_dans_git/DBSYS/Lab2-18/Lab2-18/Report.pdf:0:0-0:0) file.

## License

This project is an academic work and is intended for educational use only.





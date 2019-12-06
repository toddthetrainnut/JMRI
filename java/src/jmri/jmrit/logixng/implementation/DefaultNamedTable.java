package jmri.jmrit.logixng.implementation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jmri.InstanceManager;
import jmri.JmriException;
import jmri.Manager;
import jmri.NamedBean;
import jmri.NamedBean.BadUserNameException;
import jmri.NamedBean.BadSystemNameException;
import jmri.implementation.AbstractNamedBean;
import jmri.jmrit.logixng.AnonymousTable;
import jmri.jmrit.logixng.NamedTable;
import jmri.jmrit.logixng.NamedTableManager;

/**
 * The default implementation of a NamedTable
 */
public class DefaultNamedTable extends AbstractNamedBean implements NamedTable {

    private int _state = NamedBean.UNKNOWN;
    private final AnonymousTable _internalTable;
    
    // The system appropriate newline separator.
    private static final String _nl = System.getProperty("line.separator"); // NOI18N
    
    /**
     * Create a new named table.
     * @param sys the system name
     * @param user the user name or null if no user name
     * @param numRows the number or rows in the table
     * @param numColumns the number of columns in the table
     */
    public DefaultNamedTable(
            @Nonnull String sys, @CheckForNull String user,
            int numRows, int numColumns)
            throws BadUserNameException, BadSystemNameException {
        super(sys,user);
        _internalTable = new DefaultAnonymousTable(numRows, numColumns);
    }
    
    /**
     * Create a new named table with an existing array of cells.
     * Row 0 has the column names and column 0 has the row names.
     * @param systemName the system name
     * @param userName the user name
     * @param data the data in the table. Note that this data is not copied to
     * an new array but used by the table as is.
     */
    public DefaultNamedTable(
            @Nonnull String systemName, @CheckForNull String userName,
            @Nonnull Object[][] data)
            throws BadUserNameException, BadSystemNameException {
        super(systemName,userName);
        
        // Do this test here to ensure all the tests are using correct system names
        Manager.NameValidity isNameValid = InstanceManager.getDefault(NamedTableManager.class).validSystemNameFormat(mSystemName);
        if (isNameValid != Manager.NameValidity.VALID) {
            throw new IllegalArgumentException("system name is not valid");
        }
        _internalTable = new DefaultAnonymousTable(data);
    }
    
    private static NamedTable loadFromCSV(
            @Nonnull List<String> lines,
            @CheckForNull String systemName, @CheckForNull String userName)
            throws NamedBean.BadUserNameException, NamedBean.BadSystemNameException {
        
        NamedTableManager manager = InstanceManager.getDefault(NamedTableManager.class);
        
//        List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        
//        System.out.format("loadFromCSV()%n");
//        System.out.format("loadFromCSV(): num lines: %d%n", lines.size());
//        for (String l : lines) {
//            System.out.format("Line: '%s'%n", l);
//        }
        if (systemName == null && userName == null) {
            String[] firstRow = lines.get(0).split("\t");
            systemName = firstRow[0];
            userName = firstRow.length > 1 ? firstRow[1] : "";
            if (systemName.isEmpty()) systemName = manager.getAutoSystemName();
//            System.out.format("firstRow: %s, %s%n", firstRow[0], firstRow[1]);
//            System.out.format("systemName: %s, userName: %s%n", systemName, userName);
        }
        
        // First row is system name and user name. Second row is column names.
        int numRows = lines.size() - 2;
        
        // If the last row is empty string, ignore it.
        if (lines.get(lines.size()-1).isEmpty()) numRows--;
        
        int numColumns = 0;
        
        String[][] csvCells = new String[numRows+1][];
        for (int rowCount = 1; rowCount < numRows+2; rowCount++) {
            String[] columns = lines.get(rowCount).split("\t");
            if (numColumns+1 < columns.length) numColumns = columns.length-1;
            csvCells[rowCount-1] = columns;
        }
        
        for (int rowCount = 1; rowCount < numRows+2; rowCount++) {
            Object[] cells = csvCells[rowCount-1];
            if (cells.length <= numColumns) {
                String[] newCells = new String[numColumns+1];
                System.arraycopy(cells, 0, newCells, 0, cells.length);
                csvCells[rowCount-1] = newCells;
                for (int i=cells.length; i <= numColumns; i++) newCells[i] = "";
            }
        }
        
        NamedTable table = new DefaultNamedTable(systemName, userName, csvCells);
        manager.register(table);
        
        return table;
    }
    
    public static NamedTable loadTableFromCSV_Text(@Nonnull String text)
            throws BadUserNameException, BadSystemNameException {
        
        List<String> lines = Arrays.asList(text.split("\\r?\\n",-1));
//        List<String> lines = Arrays.asList(text.split(_nl));
        return loadFromCSV(lines, null, null);
    }
    
    public static NamedTable loadTableFromCSV_File(@Nonnull File file)
            throws BadUserNameException, BadSystemNameException, IOException {
        
        List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        return loadFromCSV(lines, null, null);
    }
    
    static public NamedTable loadTableFromCSV_File(
            @Nonnull File file,
            @Nonnull String systemName, @CheckForNull String userName)
            throws NamedBean.BadUserNameException, NamedBean.BadSystemNameException, IOException {
        
        List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        return loadFromCSV(lines, systemName, userName);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void storeTableAsCSV(@Nonnull File file)
            throws FileNotFoundException {
        _internalTable.storeTableAsCSV(file, getSystemName(), getUserName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void storeTableAsCSV(
            @Nonnull File file,
            @CheckForNull String systemName, @CheckForNull String userName)
            throws FileNotFoundException {
        
        _internalTable.storeTableAsCSV(file, systemName, userName);
    }
    
    @Override
    public void setState(int s) throws JmriException {
        _state = s;
    }

    @Override
    public int getState() {
        return _state;
    }

    @Override
    public String getBeanType() {
        return Bundle.getMessage("BeanNameLogixNG");
//        return Manager.LOGIXNGS;
//        return NamedTable.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getCell(int row, int column) {
        return _internalTable.getCell(row, column);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCell(Object value, int row, int column) {
        _internalTable.setCell(value, row, column);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int numRows() {
        return _internalTable.numRows();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int numColumns() {
        return _internalTable.numColumns();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getRowNumber(String rowName) {
        return _internalTable.getRowNumber(rowName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getColumnNumber(String columnName) {
        return _internalTable.getColumnNumber(columnName);
    }

}

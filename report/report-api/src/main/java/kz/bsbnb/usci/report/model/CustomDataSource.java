package kz.bsbnb.usci.report.model;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;



public class CustomDataSource implements JRDataSource
  {

    private int currentRecordIndex = -1;
    private ArrayList<Object[]> dataSet;
    private String[] columnNames;
    private HashMap<String, Integer> columnIndicesMap;
    private int columnCount;

    public CustomDataSource(ResultSet rs) {
        try {
            ResultSetMetaData rsmd = rs.getMetaData();
            columnCount = rsmd.getColumnCount();
            columnNames = new String[columnCount];
            columnIndicesMap = new HashMap<String, Integer>();
            for (int columnIndex = 1; columnIndex <= columnCount; columnIndex++) {
                columnNames[columnIndex - 1] = rsmd.getColumnName(columnIndex);
                columnIndicesMap.put(columnNames[columnIndex - 1], columnIndex - 1);
            }
            for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
                Class columnClass = String.class;
                if (rsmd.getColumnType(columnIndex + 1) == Types.TIMESTAMP) {
                    columnClass = Date.class;
                } else if (rsmd.getColumnType(columnIndex + 1) == Types.NUMERIC) {
                    columnClass = BigDecimal.class;
                }
            }
            dataSet = new ArrayList<Object[]>();
            while (rs.next()) {
                Object[] record = new Object[columnCount];
                for (int columnIndex = 1; columnIndex <= columnCount; columnIndex++) {
                    Object obj = rs.getObject(columnIndex);
                    if (rsmd.getColumnType(columnIndex) == Types.TIMESTAMP && obj != null) {
                        record[columnIndex - 1] = rs.getTimestamp(columnIndex);
                    } else if (rsmd.getColumnType(columnIndex) == Types.NUMERIC && obj != null) {
                        record[columnIndex - 1] = rs.getBigDecimal(columnIndex);
                    } else {
                        record[columnIndex - 1] = rs.getString(columnIndex);
                    }
                }
                dataSet.add(record);
            }

        } catch (SQLException sqle) {
            //todo сделат ReportException
        }
    }


    @Override
    public boolean next() throws JRException {
        currentRecordIndex++;
        return currentRecordIndex < dataSet.size();
    }

    @Override
    public Object getFieldValue(JRField jrf) throws JRException {
        if (currentRecordIndex >= dataSet.size()) {
            throw new JRException("Error getFieldValue 256");
        }
        String name = jrf.getName();
        try {
            return getFieldValue(name);
        } catch (NoSuchFieldException nsfe) {
            //todo сделат ReportException
            throw new JRException("Error getFieldValue 257");
        }
    }

    public Object getFieldValue(String fieldName) throws NoSuchFieldException {
        Integer index = columnIndicesMap.get(fieldName);
        if ("ROWNUM".equalsIgnoreCase(fieldName)) {
            return (currentRecordIndex + 1);
        }
        if (index == null) {
            //todo сделат ReportException
            throw new NoSuchFieldException(fieldName);
        }
        return dataSet.get(currentRecordIndex)[index];
    }

    public void reset() {
        currentRecordIndex = 0;
    }

    public String[] getColumnNames() {
        return columnNames.clone();
    }


}
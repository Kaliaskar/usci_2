package kz.bsbnb.usci.report.model;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportData {

    private  ResultSet rs;

    private int columnCount;

    public ReportData(ResultSet rs) {
        this.rs = rs;
    }

    public List<Map<String, Object>>  getMap() throws SQLException {
        ResultSetMetaData md = rs.getMetaData();
        int columns = md.getColumnCount();
        List<Map<String,Object>> list = new ArrayList<Map<String,Object>>();

        while (rs.next()) {
            HashMap<String,Object> row = new HashMap<String, Object>(columns);
            for(int i=1; i<=columns; ++i) {
                row.put(md.getColumnName(i),rs.getObject(i));
            }
            list.add(row);
        }

        return list;
    }

    public int getColumnCount() { return columnCount;}

}

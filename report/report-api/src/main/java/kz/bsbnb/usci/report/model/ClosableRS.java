package kz.bsbnb.usci.report.model;

import java.sql.ResultSet;

public interface ClosableRS {
    ResultSet getResultSet();
    void close();
}
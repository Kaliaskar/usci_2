package kz.bsbnb.usci.wsclient.dao;

import kz.bsbnb.usci.wsclient.model.ctrkgd.Request;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface KGDDao {

    List<Map<String, Object>> entityRows(LocalDate reportDate);

    void insertRequest(Request request);

    void updateRequest(Request request);

    List<Request> getCtrRequestList(LocalDate reportDate);


}

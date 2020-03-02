package kz.bsbnb.usci.util;

import com.google.gson.Gson;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Baurzhan Makhambetov
 */

public class JsonMaker {
    private static Gson gson = new Gson();

    public static class ExtJsJsonList {
        int totalCount;
        List<Object> data;
        boolean success;

        public ExtJsJsonList(List data) {
            this.data = data;
            totalCount = data.size();
            success = true;
        }
    }

    public static String getJson(List data){
        return gson.toJson(new ExtJsJsonList(data));
    }

    public static String getJson(Object data){
        Map m = new HashMap();
        m.put("data",data);
        m.put("success",true);
        return gson.toJson(m);
    }

    public static String getJson(Map m){
        m.put("success", true);
        return gson.toJson(m);
    }

}


package kz.bsbnb.usci.util.json.ext;

import java.util.Map;

/**
 * @author Jandos Iskakov
 */

public class ExtJsTree {
    private String id;
    private Map<String, Object> data;
    private String text;
    private String cls;
    private boolean leaf;

    public String getId() {
        return id;
    }

    public ExtJsTree setId(String id) {
        this.id = id;
        return this;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public ExtJsTree setData(Map<String, Object> data) {
        this.data = data;
        return this;
    }

    public String getText() {
        return text;
    }

    public ExtJsTree setText(String text) {
        this.text = text;
        return this;
    }

    public String getCls() {
        return cls;
    }

    public ExtJsTree setCls(String cls) {
        this.cls = cls;
        return this;
    }

    public boolean isLeaf() {
        return leaf;
    }

    public ExtJsTree setLeaf(boolean leaf) {
        this.leaf = leaf;
        return this;
    }

}

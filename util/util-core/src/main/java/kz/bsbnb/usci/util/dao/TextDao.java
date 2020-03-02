package kz.bsbnb.usci.util.dao;

import kz.bsbnb.usci.model.util.Text;

import java.util.List;

/**
 * @author Yernur Bakash
 */

public interface TextDao {
    Long insert(Text text);

    void update(Text text);

    void delete(Long id);

    Text get(String type, String code);

    Text get(Long id);

    List<Text> getAll();

    List<Text> getConstantsByType(List<String> types);

}

package kz.bsbnb.usci.util.dao;

import kz.bsbnb.usci.model.util.Config;

import java.util.List;

/**
 * @author Jandos Iskakov
 */

public interface ConfigDao {

    Long insert(Config config);

    void update(Config config);

    void delete(Long id);

    Config get(String module, String code);

    Config get(Long id);

    List<Config> getList();

}

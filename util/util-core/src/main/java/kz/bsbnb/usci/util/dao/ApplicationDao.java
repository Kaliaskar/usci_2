package kz.bsbnb.usci.util.dao;

import kz.bsbnb.usci.model.util.Application;

import java.util.List;

/**
 * @author Jandos Iskakov
 */

public interface ApplicationDao {

    List<Application> getAppList();

}

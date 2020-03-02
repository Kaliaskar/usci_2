package kz.bsbnb.usci.util.repository;

import kz.bsbnb.usci.model.util.Text;

public interface TextRepository {
    Text getConfig(String type, String code);

    Text getConfig(Long id);

}

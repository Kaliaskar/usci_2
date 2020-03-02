package kz.bsbnb.usci.core.dao;

import kz.bsbnb.usci.model.adm.Position;

import java.util.List;

/**
 * @author Jandos Iskakov
 */

public interface PositionDao {

    List<Position> getUserPosList();

    List<Position> getUserPosListByProductId(Long userid, Long productId);

    Position getUserPosById(Long id);

    void addUserPosition(Long userId, Long productId, List<Long> positionIds);

    void delUserPosition(Long userId, Long productId, List<Long> positionIds);

}

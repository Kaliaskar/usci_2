package kz.bsbnb.usci.core.service;

import kz.bsbnb.usci.model.adm.Position;

import java.util.List;

/**
 * @author Jandos Iskakov
 */

public interface PositionService {

    List<Position> getPositionList();

    List<Position> getUserPosListByProductId(Long userid, Long productId);

    Position getPositionById(Long id);

    void addUserPosition(Long userId, Long productId, List<Long> positionIds);

    void delUserPosition(Long userId, Long productId, List<Long> positionIds);

}

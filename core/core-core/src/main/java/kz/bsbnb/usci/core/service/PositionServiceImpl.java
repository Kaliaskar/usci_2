package kz.bsbnb.usci.core.service;

import kz.bsbnb.usci.core.dao.PositionDao;
import kz.bsbnb.usci.model.adm.Position;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author Jandos Iskakov
 */

@Service
public class PositionServiceImpl implements PositionService {
    private final PositionDao positionDao;

    public PositionServiceImpl(PositionDao positionDao) {
        this.positionDao = positionDao;
    }

    @Override
    public List<Position> getPositionList() {
        return positionDao.getUserPosList();
    }

    @Override
    public List<Position> getUserPosListByProductId(Long userid, Long productId) {
        return positionDao.getUserPosListByProductId(userid, productId);
    }

    @Override
    public Position getPositionById(Long id) {
        return positionDao.getUserPosById(id);
    }

    @Override
    public void addUserPosition(Long userId, Long productId, List<Long> positionIds) {
        positionDao.addUserPosition(userId, productId, positionIds);
    }

    @Override
    public void delUserPosition(Long userId, Long productId, List<Long> positionIds) {
        positionDao.delUserPosition(userId, productId, positionIds);
    }

}

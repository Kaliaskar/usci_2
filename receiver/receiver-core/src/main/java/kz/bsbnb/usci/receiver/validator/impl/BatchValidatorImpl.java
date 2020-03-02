package kz.bsbnb.usci.receiver.validator.impl;

import kz.bsbnb.usci.model.respondent.Respondent;
import kz.bsbnb.usci.receiver.model.Batch;
import kz.bsbnb.usci.receiver.model.exception.ReceiverException;
import kz.bsbnb.usci.receiver.reader.ManifestReader;
import kz.bsbnb.usci.receiver.validator.BatchValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * @author Baurzhan Makhambetov
 * @author Yernur Bakash
 */

@Service
public class BatchValidatorImpl implements BatchValidator {
    private static final Logger logger = LoggerFactory.getLogger(BatchValidatorImpl.class);

    private final ManifestReader manifestReader;

    public BatchValidatorImpl(ManifestReader manifestReader) {
        this.manifestReader = manifestReader;
    }

    @Override
    public void validateUserRespondent(Batch batch, List<Respondent> userRespondents) {
        if (userRespondents.size() > 0) {
            Optional<Respondent> optRespondent = manifestReader.getRespondent(batch, userRespondents);
            batch.setRespondent(optRespondent.orElseThrow(() -> new ReceiverException("Несоответствие респондента пользователю портала")));
        } else
            throw new ReceiverException("Пользователь не имеет доступа к респондентам: " + batch.getUserId());
    }

}

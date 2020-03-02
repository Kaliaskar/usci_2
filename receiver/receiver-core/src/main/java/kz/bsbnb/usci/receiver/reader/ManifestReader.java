package kz.bsbnb.usci.receiver.reader;

import kz.bsbnb.usci.model.respondent.Respondent;
import kz.bsbnb.usci.receiver.model.Batch;

import java.util.List;
import java.util.Optional;

/**
 * @author Aibek Bukabayev
 * @author Maksat Nussipzhan
 * @author Artur Tkachenko
 * @author Kanat Tulbassiev
 * @author Baurzhan Makhambetov
 */

public interface ManifestReader {

    void read(Batch batch);

    Optional<Respondent> getRespondent(Batch batch, List<Respondent> respondents);

}

package kz.bsbnb.usci.model.util;

/**
 * @author Jandos Iskakov
 */

public interface DtoConverter<E, D> {

    D convertToDto(E e);

    void updateFromDto(E e, D dto);

    default void createFromDto(E e, D dto) {
        this.updateFromDto(e, dto);
    }

}

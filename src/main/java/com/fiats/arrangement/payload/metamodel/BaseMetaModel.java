package com.fiats.arrangement.payload.metamodel;

import javax.persistence.metamodel.SingularAttribute;
import java.io.Serializable;

public interface BaseMetaModel<T1, T2> extends Serializable {

    static final long serialVersionUID = 1L;

    SingularAttribute<T1, T2> metamodel();

    T2 data();

}

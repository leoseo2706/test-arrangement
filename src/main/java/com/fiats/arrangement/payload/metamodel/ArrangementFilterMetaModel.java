package com.fiats.arrangement.payload.metamodel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class ArrangementFilterMetaModel implements Serializable {

    private static final long serialVersionUID = 1L;

    private ArrangementStatusMetaModel arrStatus;

    private List<ArrangementOperationMetaModel> arrOperations;

}
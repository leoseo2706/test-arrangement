package com.fiats.arrangement.payload.metamodel;

import com.fiats.arrangement.jpa.entity.ArrangementOperation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.metamodel.SingularAttribute;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArrangementOperationMetaModel implements BaseMetaModel<ArrangementOperation, Integer> {

    private SingularAttribute<ArrangementOperation, Integer> metamodel;

    private Integer data;

    @Override
    public SingularAttribute<ArrangementOperation, Integer> metamodel() {
        return metamodel;
    }

    @Override
    public Integer data() {
        return data;
    }
}
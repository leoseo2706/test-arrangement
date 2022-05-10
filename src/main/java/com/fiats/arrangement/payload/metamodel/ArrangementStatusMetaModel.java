package com.fiats.arrangement.payload.metamodel;

import com.fiats.arrangement.jpa.entity.Arrangement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.metamodel.SingularAttribute;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArrangementStatusMetaModel implements BaseMetaModel<Arrangement, Integer> {

    private SingularAttribute<Arrangement, Integer> metamodel;

    private Integer data;

    @Override
    public SingularAttribute<Arrangement, Integer> metamodel() {
        return metamodel;
    }

    @Override
    public Integer data() {
        return data;
    }
}

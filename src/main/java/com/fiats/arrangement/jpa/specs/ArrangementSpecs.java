package com.fiats.arrangement.jpa.specs;

import com.fiats.arrangement.constant.ArrangementRoleEnum;
import com.fiats.arrangement.jpa.entity.*;
import com.fiats.arrangement.payload.filter.ArrangementFilter;
import com.fiats.arrangement.payload.filter.ArrangementFilterStatusEnum;
import com.fiats.arrangement.payload.metamodel.ArrangementFilterMetaModel;
import com.fiats.arrangement.payload.metamodel.ArrangementOperationMetaModel;
import com.fiats.arrangement.payload.metamodel.ArrangementStatusMetaModel;
import com.fiats.tmgjpa.specification.LongIDIdentityBaseSpecs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.persistence.criteria.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Component
@Slf4j
public class ArrangementSpecs extends LongIDIdentityBaseSpecs<Arrangement, Arrangement_> {

    public Predicate hasTradingDateGreaterOrEquals(Root<Arrangement> root, CriteriaBuilder builder,
                                                   Timestamp date) {
        return builder.greaterThanOrEqualTo(root.get(Arrangement_.tradingDate), date);

    }

    public Predicate hasTradingDateLessOrEquals(Root<Arrangement> root, CriteriaBuilder builder,
                                                Timestamp date) {
        return builder.lessThanOrEqualTo(root.get(Arrangement_.tradingDate), date);

    }

    public Predicate hasTypeIn(Root<Arrangement> root, List<Integer> types) {
        return root.get(Arrangement_.type).in(types);
    }

    public Predicate hasCustomerIdIn(Join<Arrangement, ArrangementParty> join, List<Long> cusIds) {
        return join.get(ArrangementParty_.customerId).in(cusIds);
    }

    public Predicate hasRoleEqualsTo(Join<Arrangement, ArrangementParty> join,
                                     CriteriaBuilder builder, String role) {
        return builder.equal(join.get(ArrangementParty_.role), role);
    }

    public Predicate hasCustomerIdAndRoleIn(Root<Arrangement> root,
                                            CriteriaBuilder builder, List<Long> cusIds, String role) {
        Join<Arrangement, ArrangementParty> join = root.join(Arrangement_.parties);
        return builder.and(hasCustomerIdIn(join, cusIds),
                hasRoleEqualsTo(join, builder, role));
    }


    public Predicate hasFilterStatusEqualsTo(Root<Arrangement> root, Join<Arrangement, ArrangementOperation> operationJoin,
                                             CriteriaBuilder builder, int status) {

        ArrangementFilterMetaModel metaModel = ArrangementFilterStatusEnum.lookForMetaModel(status);

        List<Predicate> predicates = new ArrayList<>();
        if (metaModel.getArrStatus() != null) {
            ArrangementStatusMetaModel statusMeta = metaModel.getArrStatus();
            predicates.add(builder.equal(root.get(statusMeta.getMetamodel()), statusMeta.getData()));
        }

        if (!CollectionUtils.isEmpty(metaModel.getArrOperations())) {
            List<ArrangementOperationMetaModel> operationMetaModels = metaModel.getArrOperations();
            operationMetaModels.forEach(m ->
                    predicates.add(builder.equal(operationJoin.get(m.getMetamodel()),
                            m.getData())));
        }

        return concatenatePredicate(predicates, builder);

    }

    public Predicate hasFilterStatusIn(Root<Arrangement> root, CriteriaBuilder builder,
                                       List<Integer> statues) {

        Join<Arrangement, ArrangementOperation> join = root.join(Arrangement_.operations);
        AtomicReference<Predicate> finalPredicate = new AtomicReference<>(builder.disjunction());
        statues.forEach(status -> {
            finalPredicate.set(builder.or(finalPredicate.get(), hasFilterStatusEqualsTo(root, join, builder, status)));
        });

        return concatenatePredicate(new ArrayList<Predicate>() {{
            add(finalPredicate.get());
        }}, builder);
    }

    public Specification<Arrangement> buildBrokerSpec(ArrangementFilter filter) {

        return (root, query, builder) -> {

            query.distinct(true);

            if (isNotCountQuery(query)) {
                root.fetch(Arrangement_.operations, JoinType.INNER);
                root.fetch(Arrangement_.prices, JoinType.LEFT);
                root.fetch(Arrangement_.parties, JoinType.LEFT);
            }

            return filterBrokerArrangement(filter, root, builder);
        };
    }

    public Specification<Arrangement> buildCustomerSpec(ArrangementFilter filter) {

        return (root, query, builder) -> {

            query.distinct(true);

            if (isNotCountQuery(query)) {
                root.fetch(Arrangement_.operations, JoinType.INNER);
                root.fetch(Arrangement_.prices, JoinType.LEFT);
            }

            return filterArrangement(filter, root, builder);

        };
    }

    private Predicate filterArrangement(ArrangementFilter filter, Root<Arrangement> root,
                                        CriteriaBuilder builder) {

        List<Predicate> predicates = new ArrayList<>();

        if (!CollectionUtils.isEmpty(filter.getIds())) {
            predicates.add(hasIdIn(root, filter.getIds()));
        }

        if (!CollectionUtils.isEmpty(filter.getCustomerIds())) {
            predicates.add(hasCustomerIdAndRoleIn(root, builder, filter.getCustomerIds(),
                    ArrangementRoleEnum.OWNER.toString()));
        }

        if (filter.getStartDate() != null) {
            predicates.add(hasTradingDateGreaterOrEquals(root, builder, filter.getStartDate()));
        }

        if (filter.getEndDate() != null) {
            predicates.add(hasTradingDateLessOrEquals(root, builder, filter.getEndDate()));
        }

        if (!CollectionUtils.isEmpty(filter.getTypes())) {
            predicates.add(hasTypeIn(root, filter.getTypes()));
        }

        if (!CollectionUtils.isEmpty(filter.getFilterStatuses())) {
            predicates.add(hasFilterStatusIn(root, builder, filter.getFilterStatuses()));
        }

        return concatenatePredicate(predicates, builder);
    }

    private Predicate filterBrokerArrangement(ArrangementFilter filter, Root<Arrangement> root,
                                        CriteriaBuilder builder) {

        List<Predicate> predicates = new ArrayList<>();

        if (CollectionUtils.isEmpty(filter.getIds())) {
            filter.setIds(new ArrayList<>());
        }

        predicates.add(hasIdIn(root, filter.getIds()));

        if (!CollectionUtils.isEmpty(filter.getCustomerIds())) {
            predicates.add(hasCustomerIdAndRoleIn(root, builder, filter.getCustomerIds(),
                    ArrangementRoleEnum.OWNER.toString()));
        }

        if (filter.getStartDate() != null) {
            predicates.add(hasTradingDateGreaterOrEquals(root, builder, filter.getStartDate()));
        }

        if (filter.getEndDate() != null) {
            predicates.add(hasTradingDateLessOrEquals(root, builder, filter.getEndDate()));
        }

        if (!CollectionUtils.isEmpty(filter.getTypes())) {
            predicates.add(hasTypeIn(root, filter.getTypes()));
        }

        if (!CollectionUtils.isEmpty(filter.getFilterStatuses())) {
            predicates.add(hasFilterStatusIn(root, builder, filter.getFilterStatuses()));
        }

        return concatenatePredicate(predicates, builder);
    }

}

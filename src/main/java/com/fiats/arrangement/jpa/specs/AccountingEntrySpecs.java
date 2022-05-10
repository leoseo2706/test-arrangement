package com.fiats.arrangement.jpa.specs;

import com.fiats.arrangement.jpa.entity.AccountingEntry;
import com.fiats.arrangement.jpa.entity.AccountingEntry_;
import com.fiats.arrangement.jpa.entity.Arrangement;
import com.fiats.arrangement.jpa.entity.Arrangement_;
import com.fiats.arrangement.payload.filter.AccountingEntryFilter;
import com.fiats.tmgcoreutils.constant.Constant;
import com.fiats.tmgjpa.specification.LongIDIdentityBaseSpecs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.persistence.criteria.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class AccountingEntrySpecs extends LongIDIdentityBaseSpecs<AccountingEntry, AccountingEntry_> {

    public Predicate hasEffectiveDateGreaterOrEquals(Root<AccountingEntry> root, CriteriaBuilder builder,
                                                     Timestamp date) {
        return builder.greaterThanOrEqualTo(root.get(AccountingEntry_.entryEffectiveDate), date);

    }

    public Predicate hasEffectiveDateLessOrEquals(Root<AccountingEntry> root, CriteriaBuilder builder,
                                                  Timestamp date) {
        return builder.lessThanOrEqualTo(root.get(AccountingEntry_.entryEffectiveDate), date);
    }

    public Predicate hasStatusIn(Root<AccountingEntry> root, List<String> statuses) {
        return root.get(AccountingEntry_.status).in(statuses);
    }

    public Predicate hasTypeEquals(Root<AccountingEntry> root, CriteriaBuilder builder,
                                   Integer type) {
        return builder.equal(root.get(AccountingEntry_.entryType), type);
    }

    public Predicate hasArrangementCodeLike(Join<AccountingEntry, Arrangement> join, CriteriaBuilder builder,
                                            String code) {
        // builder.like(join.get(Arrangement_.code).as(String.class), "%" + code + "%") // cast to number to string for like
        // builder.like(builder.function("TO_CHAR", String.class, join.get("dbColumn")), contains(String.valueOf(code)))
        return builder.like(builder.lower(join.get(Arrangement_.code)), "%" + code.toLowerCase() + "%");
    }

    public Predicate hasDescriptionLikeIgnoreCase(Root<AccountingEntry> root, CriteriaBuilder builder, String description) {
        return builder.like(builder.lower(root.get(AccountingEntry_.entryDescription)), "%" + description.toLowerCase() + "%");
    }

    public Specification<AccountingEntry> buildSpec(AccountingEntryFilter filter) {

        return (root, query, builder) -> {

            query.distinct(Constant.ACTIVE); // not necessary since AccountingEntry-Arrangement is 1-1

            if (isNotCountQuery(query)) {
                root.fetch(AccountingEntry_.arrangement, JoinType.LEFT);
            }

            List<Predicate> predicates = new ArrayList<>();

            if (filter.getStartDate() != null) {
                predicates.add(hasEffectiveDateGreaterOrEquals(root, builder, filter.getStartDate()));
            }

            if (filter.getEndDate() != null) {
                predicates.add(hasEffectiveDateLessOrEquals(root, builder, filter.getEndDate()));
            }

            if (filter.getType() != null) {
                predicates.add(hasTypeEquals(root, builder, filter.getType()));
            }

            if (!CollectionUtils.isEmpty(filter.getStatuses())) {
                predicates.add(hasStatusIn(root, filter.getStatuses()));
            }

            if (StringUtils.hasText(filter.getDescription())) {
                predicates.add(hasDescriptionLikeIgnoreCase(root, builder, filter.getDescription()));
            }

            if (StringUtils.hasText(filter.getArrangementCode())) {
                Join<AccountingEntry, Arrangement> join = root.join(AccountingEntry_.arrangement);
                predicates.add(hasArrangementCodeLike(join, builder, filter.getArrangementCode()));
            }


            return concatenatePredicate(predicates, builder);
        };
    }
}

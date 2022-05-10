package com.fiats.arrangement.jpa.repo;

import com.fiats.arrangement.jpa.entity.ArrangementRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ArrangementRelationRepo extends JpaRepository<ArrangementRelation, Long> {

    @Query(value = "from ArrangementRelation ar " +
            "left join fetch ar.arrangement arr " +
            "left join fetch ar.relatedArrangement related " +
            "left join fetch related.parties relatedP " +
            "left join fetch related.prices relatedPri " +
            "left join fetch related.operations relatedOp " +
            "where ar.arrangement.id in :arrangementIds and ar.type = 1")
    List<ArrangementRelation> findByArrangementIdInFetchAll(Collection<Long> arrangementIds);

    List<ArrangementRelation> findByRelatedArrangementIdAndType(Long arrangementId, Integer type);

    List<ArrangementRelation> findByRelatedArrangementIdInAndType(Collection<Long> arrangementId, Integer type);

    List<ArrangementRelation> findByArrangementIdInAndType(Collection<Long> arrangementId, Integer type);

    @Query(value = "from ArrangementRelation ar " +
            "left join fetch ar.arrangement a " +
            "left join fetch a.operations aop " +
            "left join fetch aop.arrangement a1 " +
            "where ar.relatedArrangement.id in :relatedArrangementIds and ar.type = 1")
    List<ArrangementRelation> findByRelatedArrangementIDFetchArrangementOperation(Collection<Long> relatedArrangementIds);

    @Query(value = "from ArrangementRelation ar " +
            "left join fetch ar.relatedArrangement a " +
            "left join fetch a.operations aop " +
            "left join fetch a.prices apr " +
            "left join fetch a.parties apa " +
            "where ar.arrangement.id in :arrangementIds and ar.type = 1")
    List<ArrangementRelation> findByArrangementIDFetchRelatedArrangementOperation(Collection<Long> arrangementIds);

    @Query(value = "from ArrangementRelation ar " +
            "inner join fetch ar.relatedArrangement a " +
            "where ar.arrangement.id in :arrangementIds and ar.type = 2")
    List<ArrangementRelation> findByArrangementIDFetchRelatedArrangement(Collection<Long> arrangementIds);


}
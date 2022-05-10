package com.fiats.arrangement.jpa.repo;

import com.fiats.arrangement.jpa.entity.ArrangementOperation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ArrangementOperationRepo extends JpaRepository<ArrangementOperation, Long> {

    @Query(value = "from ArrangementOperation ao " +
            "left join fetch ao.arrangement a " +
            "where ao.arrangement.id in :arrangementId")
    List<ArrangementOperation> findByArrangementIdInFetchArrangements(Collection<Long> arrangementId);

}
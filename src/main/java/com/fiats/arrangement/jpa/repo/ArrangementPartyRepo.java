package com.fiats.arrangement.jpa.repo;

import com.fiats.arrangement.jpa.entity.ArrangementParty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArrangementPartyRepo extends JpaRepository<ArrangementParty, Long> {

    @Query(nativeQuery = true, value = "SELECT DISTINCT ARRANGEMENT_ID " +
            "FROM ARRANGEMENT_PARTY " +
            "WHERE ROLE = :ownerRole " +
            "  AND ARRANGEMENT_ID IN (select ARRANGEMENT_ID " +
            "                         from ARRANGEMENT_PARTY " +
            "                         where CUSTOMER_ID = :brokerId " +
            "                           AND ROLE = :brokerRole)")
    List<Long> findArrangementIdsForBrokerReference(Long brokerId, String ownerRole, String brokerRole);

    @Query(value = "from ArrangementParty ap inner join fetch ap.arrangement a " +
            "where ap.customerId = :customerId and ap.role = :role")
    List<ArrangementParty> findByCustomerIdAndAndRole(Long customerId, String role);

    Optional<ArrangementParty> findFirstByArrangementIdAndRole(Long arrangementId, String role);
}
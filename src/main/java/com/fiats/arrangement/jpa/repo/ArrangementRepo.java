package com.fiats.arrangement.jpa.repo;

import com.fiats.arrangement.jpa.entity.Arrangement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ArrangementRepo extends JpaRepository<Arrangement, Long>, JpaSpecificationExecutor<Arrangement> {

    @Query(value = "select distinct a from Arrangement a " +
            "left join fetch a.parties apa " +
            "where a.id = :arrangementId")
    Optional<Arrangement> findByIdFetchParty(Long arrangementId);

    @Query(value = "select distinct a from Arrangement a " +
            "left join fetch a.parties apa " +
            "left join fetch a.prices apr " +
            "where a.id = :arrangementId")
    Optional<Arrangement> findByIdFetchPartyPricing(Long arrangementId);

    @Query(value = "select distinct a from Arrangement a " +
            "left join fetch a.operations ao" +
            "left join fetch a.parties apa " +
            "left join fetch a.prices apr " +
            "where a.id = :arrangementId")
    Optional<Arrangement> findByIdFetchOperationPartyPricing(Long arrangementId);

    @Query(value = "select distinct a from Arrangement a " +
            "left join fetch a.operations ao" +
            "left join fetch a.parties apa " +
            "left join fetch a.prices apr " +
            "left join fetch a.originRelations aor " +
            "where a.id = :arrangementId")
    Optional<Arrangement> findByIdFetchOperationPartyPricingRelations(Long arrangementId);

    @Query(value = "from Arrangement a " +
            "inner join a.parties pa on (pa.role = 'OWNER') " +
            "inner join a.relatedArrs ra " +
            "inner join ra.arrangement ma on (ma.type = 3 and ma.status = 1) " +
            "inner join ma.operations moa on (ra.id = moa.arrangementRelationId and moa.deliveryStatus = 1 and moa.paymentStatus = 1) " +
            "where a.type = 1 and a.status = 1 " +
            "and NOT EXISTS (select ra1.relatedArrangement.id from ArrangementRelation ra1 inner join ra1.arrangement a1 where ra1.relatedArrangement.id = a.id and a1.status = 1 and a1.type in (2,4)) " +
            "and pa.customerId = :customerId " +
            "and (:vanillaCode is null or a.productVanillaCode = :vanillaCode) " +
            "and (:derivativeCode is null or a.productDerivativeCode = :derivativeCode) ")
    List<Arrangement> findAssetAvailableByCustomerId(Long customerId, String vanillaCode, String derivativeCode);

    @Query(value = "from Arrangement a " +
            "inner join a.parties pa on (pa.role = 'OWNER') " +
            "inner join a.relatedArrs ra " +
            "inner join ra.arrangement ma on (ma.type = 3 and ma.status = 1) " +
            "inner join ma.operations moa on (ra.id = moa.arrangementRelationId and moa.deliveryStatus = 1 and moa.paymentStatus = 1) " +
            "where a.type = 1 and a.status = 1 " +
            "and NOT EXISTS (select ra1.relatedArrangement.id from ArrangementRelation ra1 " +
            "inner join ra1.arrangement a1 inner join a1.operations oa1 " +
            "where ra1.relatedArrangement.id = a.id and a1.status = 1 and a1.type = 2 and oa1.deliveryStatus = 1 and oa1.paymentStatus = 1 ) " +
            "and pa.customerId = :customerId " +
            "and (:vanillaCode is null or a.productVanillaCode = :vanillaCode) " +
            "and (:derivativeCode is null or a.productDerivativeCode = :derivativeCode) ")
    List<Arrangement> findAssetByCustomerId(Long customerId, String vanillaCode, String derivativeCode);

    @Query(value = "SELECT TRADING_CODE_SEQUENCE.nextval FROM dual", nativeQuery = true)
    BigDecimal getNextTradingCodeVal();

    @Query(value = "select distinct a from Arrangement a " +
            "left join fetch a.operations ao " +
            "left join fetch a.parties apa " +
            "left join fetch a.prices apr " +
            "left join fetch a.relatedArrs ara " +
            "where a.id = :arrangementId ")
    Optional<Arrangement> findByIdFetchAllRelation(Long arrangementId);

    @Query("select distinct a from Arrangement a " +
            "inner join fetch a.prices " +
            "inner join fetch a.parties ap " +
            "inner join fetch a.operations " +
            "where upper(a.code) in :uppercaseCodes ")
    List<Arrangement> findByUppercaseCodesInFetchPricingOperations(Collection<String> uppercaseCodes);

    @Query("select distinct a from Arrangement a " +
            "left join fetch a.parties " +
            "where a.code = :code") // code is unique
    Optional<Arrangement> findByCodeFetchParty(String code);

    @Query("select distinct a from Arrangement a " +
            "left join fetch a.parties apa " +
            "left join fetch a.operations ao " +
            "left join fetch a.prices apr " +
            "where a.id = :id")
    Optional<Arrangement> findByIDFetchPartyOperationPricing(Long id);


}
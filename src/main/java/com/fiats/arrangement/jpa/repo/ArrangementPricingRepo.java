package com.fiats.arrangement.jpa.repo;

import com.fiats.arrangement.jpa.entity.ArrangementPricing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArrangementPricingRepo extends JpaRepository<ArrangementPricing, Long> {

}
package com.fiats.arrangement.jpa.repo;

import com.fiats.arrangement.jpa.entity.SnapshotContractDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SnapshotContractDetailRepo extends JpaRepository<SnapshotContractDetail, Long> {

}
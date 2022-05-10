package com.fiats.arrangement.jpa.repo;

import com.fiats.arrangement.jpa.entity.SnapshotContract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SnapshotContractRepo extends JpaRepository<SnapshotContract, Long> {

}
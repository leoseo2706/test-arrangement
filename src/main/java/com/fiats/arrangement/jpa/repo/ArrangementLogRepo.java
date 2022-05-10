package com.fiats.arrangement.jpa.repo;

import com.fiats.arrangement.jpa.entity.ArrangementLog;
import jdk.nashorn.internal.runtime.options.Option;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArrangementLogRepo extends JpaRepository<ArrangementLog, Long> {

    Optional<List<ArrangementLog>> findAllByArrangementIdOrderByCreatedDateDesc(Long arrangementId);

    Optional<List<ArrangementLog>> findAllByArrangementCodeOrderByCreatedDateDesc(String arrangementLog);
}

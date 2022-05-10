package com.fiats.arrangement.redis.repo;

import com.fiats.arrangement.redis.entity.ArrangementException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.QueryByExampleExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ArrangementExceptionRepo extends JpaRepository<ArrangementException, String>, QueryByExampleExecutor<ArrangementException> {
}

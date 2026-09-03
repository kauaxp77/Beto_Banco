package com.betobanco.contests.repository;

import com.betobanco.contests.entity.ContestCareer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ContestCareerRepository extends JpaRepository<ContestCareer, ContestCareer.Chave> {

    List<ContestCareer> findByContestId(UUID contestId);

    @Modifying
    @Query("DELETE FROM ContestCareer cc WHERE cc.contestId = :contestId")
    void apagarDoContest(@Param("contestId") UUID contestId);
}

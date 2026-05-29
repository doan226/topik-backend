package com.topik.topikai.repository;

import com.topik.topikai.entity.ExamSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExamSubmissionRepository extends JpaRepository<ExamSubmission, Long> {
}

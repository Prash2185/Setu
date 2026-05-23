package com.setu.cloudbilling;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserPlanRepository extends JpaRepository<UserPlan, Long> {
    UserPlan findByUsername(String username);
}
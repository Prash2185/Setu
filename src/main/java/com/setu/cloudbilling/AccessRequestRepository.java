package com.setu.cloudbilling;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface AccessRequestRepository extends JpaRepository<AccessRequest, Long> {
    List<AccessRequest> findByOwnerUsernameAndStatus(String owner, String status);
    AccessRequest findByShareIdAndRequesterUsername(String shareId, String requester);
}
package com.resergrass.repository;

import com.resergrass.domain.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long> {
    Optional<Client> findByUserEmail(String email);
    List<Client> findAllByOrderByUserFullNameAsc();
}

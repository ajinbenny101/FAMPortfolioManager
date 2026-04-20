package com.training.FAMPortfolioManager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.training.FAMPortfolioManager.model.Portfolio;

// Data access for Portfolio entities.
// JpaRepository provides findById, findAll, save, delete, and other standard CRUD operations.
public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {
}
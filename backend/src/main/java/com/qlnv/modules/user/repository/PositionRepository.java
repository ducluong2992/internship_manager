package com.qlnv.modules.user.repository;

import com.qlnv.modules.user.entity.Position;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PositionRepository extends JpaRepository<Position, Integer> {
    Optional<Position> findByName(String name);
    List<Position> findAllByOrderByNameAsc();
    List<Position> findByIsManagerTrueOrderByNameAsc();
}

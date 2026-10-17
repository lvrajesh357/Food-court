package com.currypoint.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.currypoint.entity.SpecialItems;

@Repository
public interface SpecialItemsRepository extends JpaRepository<SpecialItems, Long>{

	List<SpecialItems> findByCategory(String category);

}

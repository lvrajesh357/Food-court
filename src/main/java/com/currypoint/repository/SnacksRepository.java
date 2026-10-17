package com.currypoint.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.currypoint.entity.Snacks;

@Repository
public interface SnacksRepository extends JpaRepository<Snacks, Long>{


	 List<Snacks> findByCategoryIgnoreCase(String category);
	 List<Snacks> findByCategory(String category);


}

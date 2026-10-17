package com.currypoint.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.currypoint.entity.MenuItems;

@Repository
public interface MenuItemsRepository extends JpaRepository<MenuItems , Long>{

	 List<MenuItems> findByCategoryIgnoreCase(String category);
	 List<MenuItems> findByCategory(String category);

}

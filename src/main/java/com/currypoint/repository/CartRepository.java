package com.currypoint.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.currypoint.entity.Cart;

import jakarta.servlet.http.HttpSession;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long>{

	

	Cart findByName(String name);



}

package com.currypoint.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.currypoint.entity.CustomerRegister;

@Repository
public interface Register extends JpaRepository<CustomerRegister, Long>{

	CustomerRegister findByName(String name);

	CustomerRegister findFirstByNameOrderBySnoDesc(String name);

	CustomerRegister findFirstByEmailOrderBySnoDesc(String email);

}

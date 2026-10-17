package com.currypoint.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.currypoint.entity.Orders;

@Repository
public interface OrdersRepository extends JpaRepository<Orders, Long>{

	List<Orders> findByOrderGroupId(String orderGroupId);

	List<Orders> findByPaymentVerifiedFalseOrderByOrderTimeDesc();

	List<Orders> findByPaymentVerifiedTrueOrderByOrderTimeDesc();

}

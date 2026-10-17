package com.currypoint.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name="register")
public class CustomerRegister {
	
	@Id
	@GeneratedValue(strategy= GenerationType.IDENTITY)
	private long sno;
	
	private String name;
	private String password;
	private String address;
	private String email;

	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	
	public CustomerRegister() {}

	public CustomerRegister(String name,String password,String address) {
		this.name=name;
		this.password=password;
		this.address=address;
	}
	
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
}

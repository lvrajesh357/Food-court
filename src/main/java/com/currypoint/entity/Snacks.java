package com.currypoint.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name="snacks")
public class Snacks {

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private long sno;
	
	 private String name;
	    private String category; 
	    private double price;
	    
	    
		public String getName() {
			return name;
		}
		
		public void setName(String name) {
			this.name = name;
		}
		public String getCategory() {
			return category;
		}
		public void setCategory(String category) {
			this.category = category;
		}
		
		public double getPrice() {
			return price;
		}
		public void setPrice(double price) {
			this.price = price;
		}
	    
	    
		public Snacks() {}
		
		public Snacks(String name,String category,double price) {
			this.name=name;
			this.category=category;
			this.price=price;
		}
}

package com.currypoint.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name="menuitems")
public class MenuItems {
	
	// this both veg and non-veg curry items same entity

	@Id
	@GeneratedValue(strategy= GenerationType.IDENTITY)
	private long sno;
	
	 private String name;
	    private String category; 
	    private int quantity;
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
		
		public int getQuantity() {
			return quantity;
		}
		public void setQuantity(int quantity) {
			this.quantity=quantity;
		}
		
		public double getPrice() {
			return price;
		}
		public void setPrice(double price) {
			this.price = price;
		}
		
		
		MenuItems(){}
		
		MenuItems(String name,String category,double price,int quantity){
			this.name=name;
			this.category=category;
			this.price=price;
			this.quantity=quantity;
		}
}

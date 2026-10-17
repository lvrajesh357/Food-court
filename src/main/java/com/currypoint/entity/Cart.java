package com.currypoint.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name="cart")
public class Cart {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long sno;
	
	   private String name;
	    private int quantity;
	    private Double price;
	    private String category;
	    
	    public long getSno() {
	    	return sno;
	    }
	    public void setSno(long sno) {
	    	this.sno=sno;
	    }
	    
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		
		public int getQuantity() {
			return quantity;
		}
		public void setQuantity(int quantity) {
			this.quantity = quantity;
		}
		
		public double getPrice() {
			return price;
		}
		public void setPrice(Double price) {
			this.price = price;
		}
		
		public String getCategory() {
			return category;
		}
		public void setCategory(String category) {
			this.category=category;
		}

		public Cart(){}
		
		public Cart(long sno,String name,int quantity,Double price,String category){
			this.sno=sno;
			this.name=name;
			this.quantity=quantity;
		    this.price=price;
		    this.category=category;
		}
		   public Double getTotal() {
		        return quantity * price;
		    }
		
		public void setUserId(String userId) {
			
			
		}
	
}

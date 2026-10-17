package com.currypoint.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name="Special_items")
public class SpecialItems {
	
	// Both veg and not veg in same table
	
	@Id
	@GeneratedValue (strategy = GenerationType.IDENTITY)
	private long sno;

	
	 private String name;
//	    private String imageUrl;
	    private int quantity;
	    private String category;
	    private double price;
	    
		public long getSno() {
			return sno;
		}
		public void setSno(long sno) {
			this.sno = sno;
		}
		
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		
//		public String getImageUrl() {
//			return imageUrl;
//		}
//		public void setImageUrl(String imageUrl) {
//			this.imageUrl = imageUrl;
//		}
		
		public SpecialItems() {}
		
		public SpecialItems(long sno, String name, String imageUrl, int price,String category,int quantity) {
//			super();
			this.sno = sno;
			this.name = name;
//			this.imageUrl = imageUrl;
			this.price = price;
			this.setCategory(category);
			this.quantity=quantity;
		}
		
		public double getPrice() {
			return price;
		}
		public void setPrice(double price) {
			this.price = price;
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
}

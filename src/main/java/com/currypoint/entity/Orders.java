package com.currypoint.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name="orders")
public class Orders {
	
	@Id
	@GeneratedValue(strategy= GenerationType.IDENTITY)
	private long sno;

	public long getSno() {
		return sno;
	}
	public void setSno(long sno) {
		this.sno = sno;
	}
	
	   private String itemName;
	    private int quantity;
	    private double price;

	    // Groups every item row belonging to one checkout together, and carries
	    // the info needed to actually verify the payment happened.
	    private String orderGroupId;
	    private String customerName;
	    private String paymentMode;
	    private String paymentReference; // UTR for UPI, last-4 for cards, "COD" for cash
	    private boolean paymentVerified; // true immediately for COD; false until you confirm UPI/card orders
	    private LocalDateTime orderTime;

	    public Orders() {}

	    public Orders(String itemName, int quantity, double price) {
	        this.setItemName(itemName);
	        this.setQuantity(quantity);
	        this.setPrice(price);
	    }

	
		public String getItemName() {
			return itemName;
		}
	public void setItemName(String itemName) {
			this.itemName = itemName;
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
		public void setPrice(double price) {
			this.price = price;
		}

		public String getOrderGroupId() {
			return orderGroupId;
		}
		public void setOrderGroupId(String orderGroupId) {
			this.orderGroupId = orderGroupId;
		}

		public String getCustomerName() {
			return customerName;
		}
		public void setCustomerName(String customerName) {
			this.customerName = customerName;
		}

		public String getPaymentMode() {
			return paymentMode;
		}
		public void setPaymentMode(String paymentMode) {
			this.paymentMode = paymentMode;
		}

		public String getPaymentReference() {
			return paymentReference;
		}
		public void setPaymentReference(String paymentReference) {
			this.paymentReference = paymentReference;
		}

		public boolean isPaymentVerified() {
			return paymentVerified;
		}
		public void setPaymentVerified(boolean paymentVerified) {
			this.paymentVerified = paymentVerified;
		}

		public LocalDateTime getOrderTime() {
			return orderTime;
		}
		public void setOrderTime(LocalDateTime orderTime) {
			this.orderTime = orderTime;
		}
	    

}

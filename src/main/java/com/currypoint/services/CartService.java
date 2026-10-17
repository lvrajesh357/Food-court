package com.currypoint.services;

import java.util.List;
import com.currypoint.entity.Cart;

import jakarta.servlet.http.HttpSession;

public interface CartService {

    List<Cart> getAllCartItems();

    void addItemToCart(Cart item);

    void removeItemFromCart(Long itemId);

    void updateItemQuantity(Long itemId, int quantity);

    double getTotalAmount();

//    void applyDiscount(double discountAmount);

//    void clearCart();

    List<Cart> getCartFromSession(HttpSession session);

    double calculateCartTotal(HttpSession session);
    
    void removeItemFromSessionCart(HttpSession session, int sno);

	void addItemToSessionCart(HttpSession session, String name, int quantity, String category, double price);

	void removeItemByNameFromSessionCart(HttpSession session, String name);

	void clearCart(HttpSession session);

}

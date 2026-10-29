package com.currypoint.services;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.cache.spi.support.AbstractReadWriteAccess.Item;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.currypoint.entity.Cart;
import com.currypoint.repository.CartRepository;

import jakarta.servlet.http.HttpSession;

@Service
public class CartServiceImp implements CartService {
	
	 private static final String CART_SESSION_KEY = "cart";

    @Autowired
    private CartRepository cartrep;

    private double discount = 0.0;

    @Override
    public List<Cart> getAllCartItems() {
        return cartrep.findAll();
    }

    @Override
    public void addItemToCart(Cart item) {
        cartrep.save(item);
    }

    @Override
    public void removeItemFromCart(Long itemId) {
        cartrep.deleteById(itemId);
    }


    @Override
    public void updateItemQuantity(Long itemId, int quantity) {
        Cart item = cartrep.findById(itemId).orElse(null);
        if (item != null) {
            item.setQuantity(quantity);
            cartrep.save(item);
        }
    }

    @Override
    public double getTotalAmount() {
        List<Cart> items = cartrep.findAll();
        double total = 0.0;
        for (Cart item : items) {
            total += item.getPrice() * item.getQuantity();
        }

        // Apply discount based on total
        if (total >= 2000) {
            discount = total * 0.30;
        } else if (total >= 1500) {
            discount = total * 0.15;
        } else if (total >= 1000) {
            discount = total * 0.10;
        } else {
            discount = 0.0;
        }

        return total - discount;
    }

    public double getDiscount() {
        return discount;
    }


//    @Override
//    public void applyDiscount(double discountAmount) {
//        this.discount = discountAmount;
//    }

//    @Override
//    public void clearCart() {
//        cartrep.deleteAll();
//        discount = 0.0;
//    }

    // Simple in-memory counter so session-only cart rows still get a stable id
    // for the "remove" links, without ever touching the shared DB table.
    private long nextSessionCartId(HttpSession session) {
        Long counter = (Long) session.getAttribute("cartIdCounter");
        long next = (counter == null ? 0L : counter) + 1;
        session.setAttribute("cartIdCounter", next);
        return next;
    }

    @Override
    public void addItemToSessionCart(HttpSession session, String name, int quantity, String category, double price) {
        List<Cart> cartItems = (List<Cart>) session.getAttribute(CART_SESSION_KEY);

        if (cartItems == null) {
            cartItems = new ArrayList<>();
        }

        boolean itemExists = false;

        for (Cart item : cartItems) {
            if (item.getName().equals(name)) {
                // Update quantity in the session cart only. The cart is per-browser-session
                // state, not a persisted entity, so it must never be written to the shared
                // Cart table (that table has no link to a specific user/session and would
                // otherwise mix items from every visitor together).
                item.setQuantity(item.getQuantity() + quantity);
                itemExists = true;
                break;
            }
        }

        if (!itemExists) {
            Cart newItem = new Cart();
            newItem.setSno(nextSessionCartId(session));
            newItem.setName(name);
            newItem.setQuantity(quantity);
            newItem.setCategory(category);
            newItem.setPrice(price);

            cartItems.add(newItem);
        }

        // Update session
        session.setAttribute(CART_SESSION_KEY, cartItems);
    }



    @Override
    public void removeItemFromSessionCart(HttpSession session, int sno) {
        List<Cart> cartItems = (List<Cart>) session.getAttribute(CART_SESSION_KEY);
        if (cartItems != null) {
            cartItems.removeIf(item -> item.getSno() == sno);
            session.setAttribute(CART_SESSION_KEY, cartItems);
        }
    }

    @Override
    public void removeItemByNameFromSessionCart(HttpSession session, String name) {
        List<Cart> cartItems = (List<Cart>) session.getAttribute(CART_SESSION_KEY);
        if (cartItems != null && name != null) {
            cartItems.removeIf(item -> name.equals(item.getName()));
            session.setAttribute(CART_SESSION_KEY, cartItems);
        }
    }




    @Override
    public List<Cart> getCartFromSession(HttpSession session) {
        List<Cart> cartItems =
                (List<Cart>) session.getAttribute(CART_SESSION_KEY);

        return cartItems != null ? cartItems : new ArrayList<>();
    }
    
    

    @Override
    public double calculateCartTotal(HttpSession session) {
        List<Cart> cartItems = getCartFromSession(session);

        double originalTotal = 0.0;

        for (Cart item : cartItems) {
            if (item.getQuantity() > 0 && item.getPrice() >= 0) {
                originalTotal += item.getPrice() * item.getQuantity();
            }
        }

        double discountAmount = 0.0;

        if (originalTotal >= 2000) {
            discountAmount = originalTotal * 0.30;
        } else if (originalTotal >= 1500) {
            discountAmount = originalTotal * 0.15;
        } else if (originalTotal >= 1000) {
            discountAmount = originalTotal * 0.10;
        }

        session.setAttribute("originalAmount", originalTotal);
        session.setAttribute("discountAmount", discountAmount);

        return originalTotal;
    }

	

	@Override
	public void clearCart(HttpSession session) {
	    session.removeAttribute(CART_SESSION_KEY);
	    session.removeAttribute("cartIdCounter");
	    session.removeAttribute("originalAmount");
	    session.removeAttribute("discountAmount");
	}


//	@Override
//	public double calculateCartTotal(HttpSession session) {
//	    List<Cart> cartItems = getCartFromSession(session);
//	    double total = 0.0;
//
//	    for (Cart item : cartItems) {
//	        total += item.getPrice() * item.getQuantity();
//	    }
//
//	    session.setAttribute("originalAmount", total);
//	    return total; 
//	}



	

	

	


   



}

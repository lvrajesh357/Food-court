package com.currypoint.services;

import org.springframework.stereotype.Service;

/**
 * NOT connected to any real payment gateway or bank - this project does not
 * integrate with Razorpay, Stripe, PayU, or any UPI/card processing network.
 *
 * Real verification now happens in MyController.confirmPayment():
 *  - Card payments: Luhn checksum + expiry date validation (catches typos/
 *    obviously fake numbers, but does NOT confirm funds or a real charge).
 *  - UPI payments (PhonePe/GPay/Paytm): the customer can optionally enter the
 *    Transaction ID / UTR their UPI app shows after paying - not required to
 *    complete the order, but if provided it's included in the order details
 *    so you (the owner) can cross-check it against your bank/UPI statement.
 *  - Cash on Delivery: no verification needed, payment happens at the door.
 *
 * To accept real online payments with automatic confirmation, you would
 * need to integrate an actual payment gateway (e.g. Razorpay, Cashfree,
 * PayU) which creates a real order via their API, redirects the customer to
 * their secure checkout, and confirms payment through a signed webhook -
 * that is a genuinely different, larger integration than what exists here.
 */
@Service
public class PaymentServcieImp implements PaymentService {

	@Override
	public boolean process(String paymentMode, double amount) {
		// Kept only for interface compatibility - MyController no longer
		// calls this, since per-method verification now happens directly
		// in confirmPayment() where the actual card/UPI details are available.
		return true;
	}
}

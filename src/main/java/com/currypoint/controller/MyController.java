package com.currypoint.controller;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.time.LocalDateTime;

import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.currypoint.entity.Cart;
import com.currypoint.entity.CustomerRegister;
import com.currypoint.entity.MenuItems;
import com.currypoint.entity.Orders;
import com.currypoint.entity.Snacks;
import com.currypoint.entity.SpecialItems;
import com.currypoint.repository.CartRepository;
import com.currypoint.repository.MenuItemsRepository;
import com.currypoint.repository.OrdersRepository;
import com.currypoint.repository.Register;
import com.currypoint.repository.SnacksRepository;
import com.currypoint.repository.SpecialItemsRepository;
import com.currypoint.services.CartService;
import com.currypoint.services.CartServiceImp;
import com.currypoint.services.EmailService;

import jakarta.servlet.http.HttpSession;

@Controller
public class MyController {

	@Autowired
	private EmailService emailService;

	@Autowired
	private com.currypoint.services.OtpService otpService;

    @Autowired
    private JavaMailSender mailSender;


	@Autowired
	private Register rep;

	@Autowired
	private SpecialItemsRepository specialrep;

	@Autowired
	private MenuItemsRepository menurep;

	@Autowired
	private OrdersRepository orderRepository;

	@Autowired
	private CartServiceImp cartimp;

//	@Autowired
//	private SnacksImp snacksimp;

	@Autowired
	private SnacksRepository snacksrep;

	@Autowired
	private CartRepository cartrep;
	
	@Autowired
	private CartService cartService;

	private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	@Value("${app.owner-email}")
	private String ownerEmail;

	@Value("${app.owner-upi}")
	private String ownerUpi;

	@Value("${app.owner-access-code}")
	private String ownerAccessCode;

	@GetMapping("/")
	public String root() {
		return "redirect:/login";
	}

	@GetMapping("/register")
	public String registerapp(HttpSession session) {
		session.removeAttribute("pendingRegName");
		session.removeAttribute("pendingRegEmail");
		return "register";
	}

	@PostMapping("/register")
	public String reg(@RequestParam String name, @RequestParam String pwd, Model model) {

		CustomerRegister customer = rep.findFirstByNameOrderBySnoDesc(name);

		if (customer != null) {
			model.addAttribute("msg", "User already exists. Please login.");
			return "login"; // Redirect to login page
		}

		CustomerRegister user = new CustomerRegister();
		user.setName(name);
		user.setPassword(passwordEncoder.encode(pwd));
		rep.save(user);

		model.addAttribute("msg", "Registration successful! Please login.");

		return "login";
	}

	@PostMapping("/register/send-otp")
	public String registerSendOtp(@RequestParam String name, @RequestParam String email, Model model,
			HttpSession session) {

		if (email == null || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
			model.addAttribute("msg", "Please enter a valid email address.");
			return "register";
		}

		if (rep.findFirstByEmailOrderBySnoDesc(email) != null) {
			model.addAttribute("msg", "This email is already registered. Please login instead.");
			return "register";
		}

		otpService.generateAndSend(email);

		session.setAttribute("pendingRegName", name);
		session.setAttribute("pendingRegEmail", email);

		model.addAttribute("otpSent", true);
		model.addAttribute("name", name);
		model.addAttribute("email", email);
		model.addAttribute("msg", "OTP sent to " + email + ". Please check your inbox.");
		return "register";
	}

	@PostMapping("/register/verify-otp")
	public String registerVerifyOtp(@RequestParam String otp, Model model, HttpSession session) {
		String name = (String) session.getAttribute("pendingRegName");
		String email = (String) session.getAttribute("pendingRegEmail");

		if (name == null || email == null) {
			model.addAttribute("msg", "Session expired. Please start registration again.");
			return "register";
		}

		if (!otpService.verify(email, otp)) {
			model.addAttribute("otpSent", true);
			model.addAttribute("name", name);
			model.addAttribute("email", email);
			model.addAttribute("msg", "Incorrect or expired OTP. Please try again.");
			return "register";
		}

		CustomerRegister user = new CustomerRegister();
		user.setName(name);
		user.setEmail(email);
		rep.save(user);

		session.removeAttribute("pendingRegName");
		session.removeAttribute("pendingRegEmail");

		model.addAttribute("msg", "Registration successful! Please login.");
		return "login";
	}

	@GetMapping("/login")
	public String loginapp(HttpSession session) {
		session.removeAttribute("pendingLoginEmail");
		return "login";
	}

	@PostMapping("/login")
	public String login(@RequestParam String name, @RequestParam String pwd, Model model, HttpSession session) {
		CustomerRegister customer = rep.findFirstByNameOrderBySnoDesc(name);
		if (customer != null && customer.getPassword() != null && passwordEncoder.matches(pwd, customer.getPassword())) {
			session.setAttribute("username", customer.getName()); // ✅ Store in session
			return "redirect:/home"; // Proper redirect so the browser URL actually becomes /home
		} else {
			model.addAttribute("msg", "Please register first, then login.");
			return "login";
		}
	}

	@PostMapping("/login/send-otp")
	public String loginSendOtp(@RequestParam String email, Model model, HttpSession session) {
		CustomerRegister customer = rep.findFirstByEmailOrderBySnoDesc(email);
		if (customer == null) {
			model.addAttribute("msg", "This email isn't registered yet. Please register first.");
			return "login";
		}

		otpService.generateAndSend(email);
		session.setAttribute("pendingLoginEmail", email);

		model.addAttribute("otpSent", true);
		model.addAttribute("email", email);
		model.addAttribute("msg", "OTP sent to " + email + ". Please check your inbox.");
		return "login";
	}

	@PostMapping("/login/verify-otp")
	public String loginVerifyOtp(@RequestParam String otp, Model model, HttpSession session) {
		String email = (String) session.getAttribute("pendingLoginEmail");

		if (email == null) {
			model.addAttribute("msg", "Session expired. Please start login again.");
			return "login";
		}

		if (!otpService.verify(email, otp)) {
			model.addAttribute("otpSent", true);
			model.addAttribute("email", email);
			model.addAttribute("msg", "Incorrect or expired OTP. Please try again.");
			return "login";
		}

		CustomerRegister customer = rep.findFirstByEmailOrderBySnoDesc(email);
		if (customer == null) {
			model.addAttribute("msg", "Account not found. Please register again.");
			return "login";
		}

		session.setAttribute("username", customer.getName());
		session.setAttribute("userEmail", customer.getEmail());
		session.removeAttribute("pendingLoginEmail");

		return "redirect:/home"; // Proper redirect so the browser URL actually becomes /home
	}

	@GetMapping("/home")
	public String Homes(HttpSession session, Model model) {
		String name = (String) session.getAttribute("username");
		if (name == null) {
			return "redirect:/login"; // redirect to login if not logged in
		}
		model.addAttribute("username", name);

		int cartItemCount = cartimp.getCartFromSession(session).size();
		model.addAttribute("cartItemCount", cartItemCount);

		return "home";
	}

	@PostMapping("/resend-otp")
	public String resendOtp(HttpSession session, Model model) {
		// Works for whichever flow is currently pending (register or login).
		String regEmail = (String) session.getAttribute("pendingRegEmail");
		String loginEmail = (String) session.getAttribute("pendingLoginEmail");

		if (regEmail != null) {
			otpService.generateAndSend(regEmail);
			model.addAttribute("otpSent", true);
			model.addAttribute("name", session.getAttribute("pendingRegName"));
			model.addAttribute("email", regEmail);
			model.addAttribute("msg", "A new OTP has been sent to " + regEmail + ".");
			return "register";
		}
		if (loginEmail != null) {
			otpService.generateAndSend(loginEmail);
			model.addAttribute("otpSent", true);
			model.addAttribute("email", loginEmail);
			model.addAttribute("msg", "A new OTP has been sent to " + loginEmail + ".");
			return "login";
		}
		model.addAttribute("msg", "Nothing to resend. Please start again.");
		return "login";
	}

	@GetMapping("/veg-currys")
	public String VegCurrys(HttpSession session, Model model) {
		List<MenuItems> vegItems = menurep.findByCategoryIgnoreCase("veg");
		model.addAttribute("menuItems", vegItems);

		List<Orders> orders = orderRepository.findAll();
		model.addAttribute("orders", orders);

		String name = (String) session.getAttribute("username");
		if (name != null) {
			model.addAttribute("username", name);
		}
		return "veg-currys";
	}

	@GetMapping("/non-veg-currys")
	public String NonVegCurrys(HttpSession session, Model model) {
		List<MenuItems> nonvegItems = menurep.findByCategoryIgnoreCase("Non-veg");
		model.addAttribute("menuItems", nonvegItems);

		List<Orders> orders = orderRepository.findAll();
		model.addAttribute("orders", orders);

		String name = (String) session.getAttribute("username");
		if (name != null) {
			model.addAttribute("username", name);
		}
		return "non-veg-currys";
	}

	@GetMapping("/snacks")
	public String Snacksitems(HttpSession session, Model model) {

		List<Snacks> stators = snacksrep.findByCategoryIgnoreCase("snacks");
		model.addAttribute("Snacks", stators);

		List<Orders> orders = orderRepository.findAll();
		model.addAttribute("orders", orders);

		String name = (String) session.getAttribute("username");
		if (name != null) {
			model.addAttribute("username", name);
		}

		return "snacks";
	}

	@GetMapping("/veg-biryanis")
	public String vegBiryani(HttpSession session, Model model) {
		List<SpecialItems> vegbiryani = specialrep.findByCategory("veg-biryani");

		List<Orders> orders = orderRepository.findAll();
		model.addAttribute("orders", orders);

		model.addAttribute("menuItems", vegbiryani);
		String name = (String) session.getAttribute("username");
		if (name != null) {
			model.addAttribute("username", name);
		}
		return "veg-biryani";
	}

	@GetMapping("/non-veg-biryanis")
	public String nonvegbiryanis(HttpSession session, Model model) {
		List<SpecialItems> nonvegbiryani = specialrep.findByCategory("non-veg-biryani");

		List<Orders> orders = orderRepository.findAll();
		model.addAttribute("orders", orders);

		model.addAttribute("menuItems", nonvegbiryani);
		String name = (String) session.getAttribute("username");
		if (name != null) {
			model.addAttribute("username", name);
		}
		return "non-veg-biryani";
	}


@GetMapping("/cart")
public String viewCart(HttpSession session, Model model) {

    List<Cart> cartItems = cartService.getCartFromSession(session);

    double originalTotal = cartService.calculateCartTotal(session);

    Double discount = (Double) session.getAttribute("discountAmount");
    if (discount == null) {
        discount = 0.0;
    }

    double total = originalTotal - discount;

    model.addAttribute("cartItems", cartItems);
    model.addAttribute("orginaltotalAmount", originalTotal);
    model.addAttribute("totalAmount", total);
    model.addAttribute("discount", discount);

    return "cart";
}

@GetMapping("/cart/count")
@ResponseBody
public int getCartCount(HttpSession session) {
    return cartService.getCartFromSession(session).size();
}

	@PostMapping("/cart/add-to-session")
	@ResponseBody
	public String addToCart(@RequestBody Cart request, HttpSession session) {
		System.out.println("Received item: " + request.getName());
		System.out.println("Category: " + request.getCategory()); // ✅ log category
		System.out.println("Price: " + request.getPrice());

		cartimp.addItemToSessionCart(session, request.getName(), request.getQuantity(), request.getCategory(),
				request.getPrice());
		return "Item added successfully!";
	}

	@PostMapping("/cart/remove-by-name")
	@ResponseBody
	public String removeFromCartByName(@RequestBody Cart request, HttpSession session) {
		cartimp.removeItemByNameFromSessionCart(session, request.getName());
		return "Item removed successfully!";
	}

	@GetMapping("/cart/place-order")
	public String placeOrder(HttpSession session, @RequestParam String paymentMode, Model model) {
		List<Cart> cart = cartimp.getCartFromSession(session);
		double total = cartimp.calculateCartTotal(session);

		if (cart == null || cart.isEmpty()) {
			model.addAttribute("message", "Cart is empty!");
			return "cart";
		}

		// Store order in DB if needed
		session.removeAttribute("cart");

		model.addAttribute("message", "Order placed successfully! Total ₹" + total);
		return "orders";
	}

	@PostMapping("/orders/send-otp")
	@ResponseBody
	public ResponseEntity<String> sendCheckoutOtp(
	        @RequestParam String email,
	        HttpSession session) {

	    if (email == null ||
	            !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
	        return ResponseEntity.badRequest()
	                .body("Enter a valid email address.");
	    }

	    email = email.trim().toLowerCase();

	    // Limit sending to one OTP request per 60 seconds per session.
	    Long lastSent = (Long) session.getAttribute("checkoutOtpLastSent");
	    long now = System.currentTimeMillis();

	    if (lastSent != null && now - lastSent < 60_000) {
	        return ResponseEntity.status(429)
	                .body("Please wait before requesting another OTP.");
	    }

	    try {
	        otpService.generateAndSend(email);

	        session.setAttribute("pendingOrderEmail", email);
	        session.setAttribute("checkoutOtpLastSent", now);
	        session.removeAttribute("verifiedOrderEmail");

	        return ResponseEntity.ok(
	                "OTP requested. Check your inbox and spam folder.");
	    } catch (Exception e) {
	        return ResponseEntity.internalServerError()
	                .body("Unable to send OTP. Please try again.");
	    }
	}

	@PostMapping("/orders/verify-otp")
	@ResponseBody
	public ResponseEntity<String> verifyCheckoutOtp(
	        @RequestParam String email,
	        @RequestParam String otp,
	        HttpSession session) {

	    String pendingEmail =
	            (String) session.getAttribute("pendingOrderEmail");

	    if (pendingEmail == null ||
	            !pendingEmail.equalsIgnoreCase(email.trim())) {
	        return ResponseEntity.badRequest()
	                .body("Request an OTP for this email first.");
	    }

	    if (otp == null || !otp.matches("\\d{6}")) {
	        return ResponseEntity.badRequest()
	                .body("Enter the six-digit OTP.");
	    }

	    if (!otpService.verify(pendingEmail, otp)) {
	        return ResponseEntity.badRequest()
	                .body("Incorrect or expired OTP.");
	    }

	    session.setAttribute("verifiedOrderEmail", pendingEmail);

	    return ResponseEntity.ok(
	            "Email verified successfully. You can place your order.");
	}
	@GetMapping("/cart/remove/{sno}")
	public String removeFromCart(@PathVariable int sno, HttpSession session) {
		cartimp.removeItemFromSessionCart(session, sno);
		return "redirect:/cart";
	}

	@GetMapping("/orders")
	public String OrdersAppn(Model model, @RequestParam(required = false) String name, HttpSession session) {
		if (name == null || name.isEmpty()) {
			name = (String) session.getAttribute("username");
		} else {
			session.setAttribute("username", name);
		}

		if (name == null || name.isEmpty()) {
			model.addAttribute("error", "No user found. Please log in.");
			return "redirect:/login";
		}

		model.addAttribute("username", name);
		model.addAttribute("ownerUpi", ownerUpi);

		double originalAmount = session.getAttribute("originalAmount") != null
				? (double) session.getAttribute("originalAmount")
				: 0.0;

		double discountAmount = session.getAttribute("discountAmount") != null
				? (double) session.getAttribute("discountAmount")
				: 0.0;

		// Only apply coupon if marked as used and valid
		Boolean couponUsed = session.getAttribute("couponUsed") != null ? (Boolean) session.getAttribute("couponUsed")
				: false;

		double couponDiscount = 0.0;
		if (couponUsed != null && couponUsed) {
			couponDiscount = session.getAttribute("couponDiscount") != null
					? (double) session.getAttribute("couponDiscount")
					: 0.0;
			model.addAttribute("couponMessage", "Coupon already applied.");
		} else {
			couponDiscount = 0.0;
			session.setAttribute("couponDiscount", 0.0); // Ensure zero if not applied
		}

		double finalAmount = Math.max(0, originalAmount - discountAmount - couponDiscount);

		model.addAttribute("totalAmount", originalAmount);
		model.addAttribute("discount", discountAmount);
		model.addAttribute("coupon", couponDiscount);
		model.addAttribute("couponUsed", couponUsed);
		model.addAttribute("finaltotalAmount", finalAmount);

		return "orders";
	}

	@PostMapping("/applyCoupon")
	public String applyCoupon(
	        @RequestParam String coupon,
	        HttpSession session,
	        RedirectAttributes redirectAttributes) {

	    String username = (String) session.getAttribute("username");

	    if (username == null || username.isBlank()) {
	        return "redirect:/login";
	    }

	    if (coupon == null || !"coupon".equalsIgnoreCase(coupon.trim())) {
	        session.setAttribute("couponDiscount", 0.0);
	        session.setAttribute("couponUsed", false);

	        redirectAttributes.addFlashAttribute(
	                "couponMessage", "Invalid coupon code.");

	        return "redirect:/orders";
	    }

	    // Calculate the actual cart total on the server.
	    double originalAmount = cartService.calculateCartTotal(session);

	    double regularDiscount = (Double) session.getAttribute("discountAmount");

	    double amountAfterRegularDiscount =
	            Math.max(0, originalAmount - regularDiscount);

	    if (amountAfterRegularDiscount <= 0) {
	        redirectAttributes.addFlashAttribute(
	                "couponMessage", "There is no payable amount for this coupon.");

	        return "redirect:/orders";
	    }

	    // Prevent the coupon from being applied a second time.
	    Boolean couponUsed = (Boolean) session.getAttribute("couponUsed");

	    if (Boolean.TRUE.equals(couponUsed)) {
	        redirectAttributes.addFlashAttribute(
	                "couponMessage", "Coupon already applied.");

	        return "redirect:/orders";
	    }

	    double couponDiscount;

	    if (amountAfterRegularDiscount < 100) {
	        couponDiscount = amountAfterRegularDiscount * 0.02;
	    } else if (amountAfterRegularDiscount < 1000) {
	        couponDiscount = amountAfterRegularDiscount * 0.10;
	    } else if (amountAfterRegularDiscount < 2500) {
	        couponDiscount = amountAfterRegularDiscount * 0.30;
	    } else {
	        couponDiscount = amountAfterRegularDiscount * 0.50;
	    }

	    session.setAttribute("couponDiscount", couponDiscount);
	    session.setAttribute("couponUsed", true);

	    redirectAttributes.addFlashAttribute(
	            "couponMessage",
	            "Coupon applied successfully! Discount: ₹"
	                    + String.format("%.2f", couponDiscount));

	    return "redirect:/orders";
	}
	
	// The list of payment options actually offered on orders.html.
	private static final List<String> SUPPORTED_PAYMENT_MODES = List.of("phonepe", "gpay", "paytm", "anyupi", "debit",
			"credit", "cod", "upi", "card");

	/**
	 * Normalizes an Indian phone/WhatsApp number to E.164 format (+91XXXXXXXXXX),
	 * which is what Twilio requires. Handles the common ways people type it:
	 *   9876543210        -> +919876543210
	 *   09876543210       -> +919876543210   (leading trunk 0 stripped)
	 *   919876543210      -> +919876543210   (already has 91, just missing +)
	 *   +919876543210     -> +919876543210   (already correct, left as-is)
	 *   +14155238886      -> +14155238886    (non-Indian number, left as-is)
	 */
	/**
	 * Server-side card validation - never trust client-side checks alone,
	 * since a request can bypass the browser entirely. Verifies the card
	 * number passes the Luhn checksum (same algorithm every card network
	 * uses) and the expiry date is in MM/YY format and not in the past.
	 * This does NOT verify the card is real, has funds, or belongs to the
	 * person - this project has no payment gateway/bank connection. See the
	 * note in PaymentServcieImp.java.
	 */
	private boolean isValidCard(String cardNumber, String expiry, String cvv) {
		if (cardNumber == null || expiry == null || cvv == null) {
			return false;
		}

		String digits = cardNumber.replaceAll("[^0-9]", "");
		if (digits.length() < 12 || digits.length() > 19) {
			return false;
		}
		int sum = 0;
		boolean doubleDigit = false;
		for (int i = digits.length() - 1; i >= 0; i--) {
			int d = digits.charAt(i) - '0';
			if (doubleDigit) {
				d *= 2;
				if (d > 9) {
					d -= 9;
				}
			}
			sum += d;
			doubleDigit = !doubleDigit;
		}
		if (sum % 10 != 0) {
			return false;
		}

		if (!expiry.trim().matches("(0[1-9]|1[0-2])/?[0-9]{2}")) {
			return false;
		}
		String cleanExpiry = expiry.trim().replace("/", "");
		int month = Integer.parseInt(cleanExpiry.substring(0, 2));
		int year = 2000 + Integer.parseInt(cleanExpiry.substring(2, 4));
		java.time.YearMonth cardExpiry = java.time.YearMonth.of(year, month);
		if (cardExpiry.isBefore(java.time.YearMonth.now())) {
			return false;
		}

		return cvv.trim().matches("[0-9]{3,4}");
	}

	private String normalizeIndianPhone(String raw) {
		if (raw == null || raw.isBlank()) {
			return raw;
		}
		String trimmed = raw.trim();

		// Already has a country code -> leave it alone.
		if (trimmed.startsWith("+")) {
			return trimmed;
		}

		// Strip everything except digits to inspect the number itself.
		String digits = trimmed.replaceAll("[^0-9]", "");

		if (digits.startsWith("0") && digits.length() == 11) {
			digits = digits.substring(1); // drop domestic trunk prefix
		}

		if (digits.length() == 10) {
			return "+91" + digits;
		}
		if (digits.length() == 12 && digits.startsWith("91")) {
			return "+" + digits;
		}

		// Anything else (unexpected length, non-Indian number typed without +,
		// etc.) - just prefix a + so Twilio at least gets a plausible E.164
		// value instead of silently rejecting a bare local number.
		return digits.isEmpty() ? trimmed : "+" + digits;
	}

	@PostMapping("/payment/confirm")
	public String confirmPayment(@RequestParam String paymentMode, @RequestParam double amount,
			@RequestParam String username, @RequestParam(required = false) String email,
			@RequestParam(required = false) String customerNumber,
			@RequestParam(required = false) String addressLine, @RequestParam(required = false) String landmark,
			@RequestParam(required = false) String city, @RequestParam(required = false) String mandal,
			@RequestParam(required = false) String district, @RequestParam(required = false) String state,
			@RequestParam(required = false) String pincode,
			@RequestParam(required = false) String cardNumber, @RequestParam(required = false) String cardName,
			@RequestParam(required = false) String expiry, @RequestParam(required = false) String cvv,
			@RequestParam(required = false) String creditCardNumber, @RequestParam(required = false) String creditCardName,
			@RequestParam(required = false) String creditExpiry, @RequestParam(required = false) String creditCvv,
			@RequestParam(required = false) String upiTransactionId,
			HttpSession session, Model model, RedirectAttributes redirectAttributes) {

		// Twilio requires full country-code (E.164) format like +919876543210.
		// Most people just type the plain 10-digit Indian mobile number, so add
		// +91 automatically when no country code was given. The same number is
		// used for both WhatsApp and SMS - no need to collect it twice.
		customerNumber = normalizeIndianPhone(customerNumber);

		String mode = paymentMode == null ? "" : paymentMode.toLowerCase();

		if (!SUPPORTED_PAYMENT_MODES.contains(mode)) {
			redirectAttributes.addFlashAttribute("error", "Please select a valid payment method.");
			return "redirect:/orders";
		}

		// Per-method verification. This is a real, honest verification layer -
		// NOT a connection to an actual payment gateway/bank, since this project
		// has no such integration. See the note in PaymentServcieImp.java.
		if (mode.equals("debit")) {
			if (!isValidCard(cardNumber, expiry, cvv)) {
				redirectAttributes.addFlashAttribute("error", "Invalid or expired card details. Please check your card number, expiry, and CVV.");
				return "redirect:/orders";
			}
		} else if (mode.equals("credit")) {
			if (!isValidCard(creditCardNumber, creditExpiry, creditCvv)) {
				redirectAttributes.addFlashAttribute("error", "Invalid or expired card details. Please check your card number, expiry, and CVV.");
				return "redirect:/orders";
			}
		} else if (mode.equals("phonepe") || mode.equals("gpay") || mode.equals("paytm") || mode.equals("anyupi")) {
			if (upiTransactionId == null || upiTransactionId.trim().length() < 6) {
				redirectAttributes.addFlashAttribute("error",
						"Please complete the payment in your UPI app and enter the Transaction ID / UTR shown after payment.");
				return "redirect:/orders";
			}
		}
		// "cod" needs no verification - money changes hands at the door.

		// All mode-specific verification above passed (or the redirect above
		// already fired), so the order can proceed.

		// Payment reference detail, so you (the owner) have an audit trail to
		// cross-check against your bank/UPI statement before delivering. COD
		// needs no verification; card/UPI orders start unverified until you
		// confirm them via /owner/orders.
		String paymentReference;
		boolean paymentVerified;
		if (mode.equals("debit") || mode.equals("credit")) {
			String cn = mode.equals("debit") ? cardNumber : creditCardNumber;
			String digits = cn == null ? "" : cn.replaceAll("[^0-9]", "");
			String last4 = digits.length() >= 4 ? digits.substring(digits.length() - 4) : digits;
			paymentReference = "Card ending in " + last4;
			paymentVerified = false;
		} else if (mode.equals("phonepe") || mode.equals("gpay") || mode.equals("paytm") || mode.equals("anyupi")) {
			paymentReference = "UPI Transaction ID: " + upiTransactionId;
			paymentVerified = false;
		} else {
			paymentReference = "Cash on Delivery - collect ₹" + amount + " at the door";
			paymentVerified = true;
		}

		// Never place an order for an empty cart (e.g. a stale/duplicate form
		// submission after items were already removed).
		List<Cart> cartItems = cartimp.getCartFromSession(session);
		if (cartItems == null || cartItems.isEmpty()) {
			redirectAttributes.addFlashAttribute("error", "Your cart is empty. Please add items before placing an order.");
			return "redirect:/orders";
		}

		// Persist the items that were actually ordered, then clear the cart/coupon
		// state so the next visit starts fresh.

String orderGroupId = "FC" + System.currentTimeMillis();
LocalDateTime now = LocalDateTime.now();
StringBuilder itemLines = new StringBuilder();

List<Orders> ordersToSave = new java.util.ArrayList<>();

for (Cart item : cartItems) {
    Orders order = new Orders(
        item.getName(),
        item.getQuantity(),
        item.getPrice()
    );

    order.setOrderGroupId(orderGroupId);
    order.setCustomerName(username);
    order.setPaymentMode(paymentMode);
    order.setPaymentReference(paymentReference);
    order.setPaymentVerified(paymentVerified);
    order.setOrderTime(now);

    ordersToSave.add(order);

    itemLines.append("- ")
        .append(item.getName())
        .append(" x")
        .append(item.getQuantity())
        .append(" (₹")
        .append(item.getPrice())
        .append(")\n");
}

orderRepository.saveAll(ordersToSave);
		session.removeAttribute("cart");
		session.removeAttribute("originalAmount");
		session.removeAttribute("discountAmount");
		session.removeAttribute("couponDiscount");
		session.removeAttribute("couponUsed");

		String fullAddress = java.util.stream.Stream
				.of(addressLine, landmark, city, mandal, district, state, pincode)
				.filter(s -> s != null && !s.isBlank())
				.reduce((a, b) -> a + ", " + b)
				.orElse("");

		String paymentVerification = paymentReference
				+ (paymentVerified ? "" : " — ⏳ Awaiting your manual verification (see /owner/orders)");

		String subject = "Your FoodCourt Order Has Been Placed! 🎉";
		String message = "Hi " + username + ",\n\n" + "Your order has been placed successfully!\n\n" + "Order Summary:\n"
				+ itemLines + "\nTotal Paid: ₹" + amount + "\nPayment Mode: " + paymentMode
				+ "\nPayment Verification: " + paymentVerification
				+ "\nDelivery Address: " + fullAddress + "\nContact Number: " + (customerNumber != null ? customerNumber : "Not provided")
				+ "\n\n" + "Your order will be delivered soon 🚚\n\n" + "Thank you for ordering with FoodCourt!";

		if (email != null && !email.isEmpty()) {
			emailService.sendMail(email, subject, message);
		}

		// Notify the owner by email. Customer number is kept on the order purely
		// as contact info (so you can call them if needed) - no WhatsApp/SMS is
		// sent to it, email is the only active notification channel.
		String customerMobile = (customerNumber != null && !customerNumber.isBlank()) ? customerNumber
				: "Not provided";

		String ownerAlertFull = "🔔 New FoodCourt Order\n\n" + "Order ID: " + orderGroupId + "\nCustomer: " + username
				+ "\nMobile: " + customerMobile
				+ "\n\nItems:\n" + itemLines + "\nTotal Amount: ₹" + amount + "\nPayment Mode: " + paymentMode
				+ "\nPayment Verification: " + paymentVerification
				+ "\n\nDelivery Address:\n" + fullAddress
				+ (paymentVerified ? "\n\nPlease prepare and deliver this order."
						: "\n\n⚠️ Please verify this payment at /owner/orders before delivering.");

		emailService.sendMail(ownerEmail, "New FoodCourt Order - " + username, ownerAlertFull);

		String orderTimeStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
		int etaMinutes = 5 + new java.util.Random().nextInt(11); // random 5-15 minutes

		// Stash order details in the session so the /delivery page (reached via a
		// plain browser redirect after the celebration screen's 10s countdown)
		// can still render them.
		session.setAttribute("username", username);
		session.setAttribute("order_amount", amount);
		session.setAttribute("order_paymentMode", paymentMode);
		session.setAttribute("order_customerNumber", customerNumber);
		session.setAttribute("order_address", fullAddress);
		session.setAttribute("order_time", orderTimeStr);
		session.setAttribute("order_deliveryMessage", message);
		session.setAttribute("order_eta", etaMinutes);

		model.addAttribute("username", username);
		model.addAttribute("amount", amount);
		model.addAttribute("paymentMode", paymentMode);
		model.addAttribute("customerNumber", customerNumber);
		model.addAttribute("address", fullAddress);
		model.addAttribute("orderTime", orderTimeStr);
		model.addAttribute("deliveryMessage", message);
		model.addAttribute("etaMinutes", etaMinutes);
		return "celebration"; // celebration.html -> auto-redirects to /delivery after 10s
	}

	private void sendEmail(String customerEmail, String subject, String message) {
	     try {
	        SimpleMailMessage mailMessage = new SimpleMailMessage();
	        mailMessage.setFrom(ownerEmail);  // Owner mail
	        mailMessage.setTo(customerEmail, ownerEmail); // send to both customer & owner
	        mailMessage.setSubject(subject);
	        mailMessage.setText(message);

	        mailSender.send(mailMessage);
	     } catch (Exception e) {
	        // Don't let a mail failure break the caller.
	     }
	}
	
	 @PostMapping("/send")
	    public String sendMessage(@RequestParam String email) {
	        String subject = "Order Confirmation";
	        String message = "Hello! Your order has been placed successfully.";

	        sendEmail(email, subject, message);
	        return "Mail Sent Successfully!";
	    }
	

	@GetMapping("/payment-sucess")
	public String Paymentsucess() {
		return "payment-sucess";
	}

	@GetMapping("/delivery")
	public String deliveryPage(HttpSession session, Model model) {
		String username = (String) session.getAttribute("username");
		Double amount = (Double) session.getAttribute("order_amount");
		String paymentMode = (String) session.getAttribute("order_paymentMode");
		String customerNumber = (String) session.getAttribute("order_customerNumber");
		String address = (String) session.getAttribute("order_address");
		String orderTime = (String) session.getAttribute("order_time");
		String deliveryMessage = (String) session.getAttribute("order_deliveryMessage");
		Integer etaMinutes = (Integer) session.getAttribute("order_eta");

		model.addAttribute("username", username);
		model.addAttribute("amount", amount != null ? amount : 0.0);
		model.addAttribute("paymentMode", paymentMode);
		model.addAttribute("customerNumber", customerNumber);
		model.addAttribute("address", address);
		model.addAttribute("orderTime", orderTime);
		model.addAttribute("deliveryMessage", deliveryMessage);
		model.addAttribute("etaMinutes", etaMinutes != null ? etaMinutes : 15);

		return "delivery";
	}

	@GetMapping("/owner/orders")
	public String ownerOrders(@RequestParam(required = false) String code, Model model) {
		if (code == null || !code.equals(ownerAccessCode)) {
			model.addAttribute("accessDenied", true);
			return "owner-orders";
		}

		List<Orders> unverified = orderRepository.findByPaymentVerifiedFalseOrderByOrderTimeDesc();
		List<Orders> verified = orderRepository.findByPaymentVerifiedTrueOrderByOrderTimeDesc();

		model.addAttribute("code", code);
		model.addAttribute("unverifiedGroups", groupByOrderId(unverified));
		model.addAttribute("verifiedGroups", groupByOrderId(verified));
		return "owner-orders";
	}

	@PostMapping("/owner/orders/verify")
	public String verifyOrder(@RequestParam String code, @RequestParam String orderGroupId,
			RedirectAttributes redirectAttributes) {
		if (code == null || !code.equals(ownerAccessCode)) {
			return "redirect:/owner/orders";
		}

		List<Orders> items = orderRepository.findByOrderGroupId(orderGroupId);
		for (Orders item : items) {
			item.setPaymentVerified(true);
		}
		orderRepository.saveAll(items);

		redirectAttributes.addFlashAttribute("msg", "Order " + orderGroupId + " marked as verified.");
		return "redirect:/owner/orders?code=" + code;
	}

	/**
	 * Groups flat Orders rows (one row per item) back into one entry per
	 * checkout, keyed by orderGroupId, so the owner page can show "Order
	 * FC123... - 3 items - ₹450" instead of a flat item list.
	 */
	private List<java.util.Map<String, Object>> groupByOrderId(List<Orders> rows) {
		java.util.LinkedHashMap<String, java.util.Map<String, Object>> groups = new java.util.LinkedHashMap<>();
		for (Orders row : rows) {
			String groupId = row.getOrderGroupId() != null ? row.getOrderGroupId() : ("ungrouped-" + row.getSno());
			java.util.Map<String, Object> group = groups.computeIfAbsent(groupId, k -> {
				java.util.Map<String, Object> g = new java.util.HashMap<>();
				g.put("orderGroupId", groupId);
				g.put("customerName", row.getCustomerName());
				g.put("paymentMode", row.getPaymentMode());
				g.put("paymentReference", row.getPaymentReference());
				g.put("orderTime", row.getOrderTime());
				g.put("items", new java.util.ArrayList<Orders>());
				g.put("total", 0.0);
				return g;
			});
			@SuppressWarnings("unchecked")
			List<Orders> items = (List<Orders>) group.get("items");
			items.add(row);
			group.put("total", (double) group.get("total") + row.getPrice() * row.getQuantity());
		}
		return new java.util.ArrayList<>(groups.values());
	}

	
	@GetMapping("/clear")
	public String clearCart(HttpSession session) {
	    cartService.clearCart(session);
	    return "redirect:/cart";
	}
	
	@GetMapping("/logout")
	public String logout(HttpSession session) {
		session.invalidate();
		return "redirect:/login";
	}

}

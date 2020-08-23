package com.MyBookstoreUser.controller;

import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.websocket.server.PathParam;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.MyBookstoreUser.model.Book;
import com.MyBookstoreUser.model.CartItem;
import com.MyBookstoreUser.model.Order;
import com.MyBookstoreUser.model.User;
import com.MyBookstoreUser.model.UserBilling;
import com.MyBookstoreUser.model.UserPayment;
import com.MyBookstoreUser.model.UserShipping;
import com.MyBookstoreUser.model.security.PasswordResetToken;
import com.MyBookstoreUser.model.security.Role;
import com.MyBookstoreUser.model.security.UserRole;
import com.MyBookstoreUser.service.BookService;
import com.MyBookstoreUser.service.CartItemService;
import com.MyBookstoreUser.service.OrderService;
import com.MyBookstoreUser.service.UserPaymentService;
import com.MyBookstoreUser.service.UserService;
import com.MyBookstoreUser.service.UserShippingService;
import com.MyBookstoreUser.service.impl.UserSecurityService;
import com.MyBookstoreUser.utility.INConstants;
import com.MyBookstoreUser.utility.MailConstructor;
import com.MyBookstoreUser.utility.SecurityUtility;


@Controller
public class HomeController {

	@Autowired
	private UserService userService;
	
	@Autowired
	private UserSecurityService userSecurityService;
	
	@Autowired
	private JavaMailSender mailSender;
	
	@Autowired
	private MailConstructor mailConstructor;
	
	@Autowired
	private BookService bookService;
	
	@Autowired
	private UserPaymentService userPaymentService;
	
	@Autowired
	private UserShippingService userShippingService;
	
	@Autowired
	private CartItemService cartItemService;
	
	@Autowired
	private OrderService orderService;
	
	
	@GetMapping("/")
	public String index()
	{
		return "index";
	}
	
	@GetMapping("/login")
	public String login(Model model) 
	{
		model.addAttribute("classActiveLogin", true);
		return "myAccount";
	}
	
	@GetMapping("/about")
	public String about()
	{
		return "about";
	}
	
	@GetMapping("/contact")
	public String contact()
	{
		return "contact";
	}
	
//	@GetMapping("/badRequestPage")
//	public String error()
//	{
//		return "badRequestPage";
//	}
	
	@GetMapping("/newUser")
	public String newUser(Locale locale, @RequestParam("token") String token, Model model) 
	{
		PasswordResetToken passToken = userService.getPasswordResetToken(token);

		if (passToken == null) {
			String message = "Invalid Token.";
			model.addAttribute("message", message);
			return "redirect:/badRequest";
		}

		User user = passToken.getUser();
		String username = user.getUsername();

		UserDetails userDetails = userSecurityService.loadUserByUsername(username);

		Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, userDetails.getPassword(),
				userDetails.getAuthorities());
		
		SecurityContextHolder.getContext().setAuthentication(authentication);
		
		model.addAttribute("user", user);

		model.addAttribute("classActiveEdit", true);
		
		return "myProfile";
	}
	
	
	@PostMapping(value="/newUser")
	public String newUserPost(HttpServletRequest request,@ModelAttribute("email") String userEmail,
								@ModelAttribute("username") String username,Model model) throws Exception
	{
		model.addAttribute("classActiveNewAccount", true);
		model.addAttribute("email", userEmail);
		model.addAttribute("username", username);
		
		if (userService.findByUsername(username) != null) {
			model.addAttribute("usernameExists", true);
			return "myAccount";
		}
		
		if (userService.findByEmail(userEmail) != null) {
			model.addAttribute("emailExists", true);
			return "myAccount";
		}
		
		User user = new User();
		user.setUsername(username);
		user.setEmail(userEmail);
		
		String password = SecurityUtility.randomPassword();
		
		String encryptedPassword = SecurityUtility.passwordEncoder().encode(password);
		user.setPassword(encryptedPassword);
		
		Role role = new Role();
		role.setRoleId(1);
		role.setName("ROLE_USER");
		Set<UserRole> userRoles = new HashSet<>();
		userRoles.add(new UserRole(user, role));
		userService.createUser(user, userRoles);

		String token = UUID.randomUUID().toString();
		userService.createPasswordResetTokenForUser(user, token);
		
		String appUrl = "http://"+request.getServerName()+":"+request.getServerPort()+request.getContextPath();
		
		SimpleMailMessage email = mailConstructor.constructResetTokenEmail(appUrl, request.getLocale(), token, user, password);
		
		mailSender.send(email);
		
		model.addAttribute("emailSent", "true");
		
		return "myAccount";
	}	
	
	@PostMapping("/forgetPassword")
	public String forgetPassword(HttpServletRequest request,@ModelAttribute("email") String email,Model model) 
	{

		model.addAttribute("classActiveForgetPassword", true);
		
		User user = userService.findByEmail(email);
		
		if (user == null) {
			model.addAttribute("emailNotExist", true);
			return "myAccount";
		}
		
		String password = SecurityUtility.randomPassword();
		
		String encryptedPassword = SecurityUtility.passwordEncoder().encode(password);
		user.setPassword(encryptedPassword);
		
		userService.save(user);
		
		String token = UUID.randomUUID().toString();
		userService.createPasswordResetTokenForUser(user, token);
		
		String appUrl = "http://"+request.getServerName()+":"+request.getServerPort()+request.getContextPath();
		
		SimpleMailMessage newEmail = mailConstructor.constructResetTokenEmail(appUrl, request.getLocale(), token, user, password);
		
		mailSender.send(newEmail);
		
		model.addAttribute("forgetPasswordEmailSent", "true");
		
		
		return "myAccount";
	}
	
	
	@RequestMapping("/bookshelf")
	public String bookshelf(Model model, Principal principal) 
	{
		if(principal != null) {
			String username = principal.getName();
			User user = userService.findByUsername(username);
			model.addAttribute("user", user);
		}
		
		List<Book> bookList = bookService.findAll();
		model.addAttribute("bookList", bookList);
		model.addAttribute("activeAll",true);
		
		return "bookshelf";
	}
	
	
	@RequestMapping("/bookDetail")
	public String bookDetail(@PathParam("id") Long id, Model model) 
	{
		model.addAttribute("book", bookService.findOne(id).orElse(null));
		
		List<Integer> qtyList = Arrays.asList(1,2,3,4,5,6,7,8,9,10);
		
		model.addAttribute("qtyList", qtyList);
		model.addAttribute("qty", 1);
		
		return "bookDetail";
	}
	
	@RequestMapping("/myProfile")
	public String myProfile(Model model, Principal principal) 
	{
		User user = userService.findByUsername(principal.getName());
		
		model.addAttribute("user", user);
		model.addAttribute("userPaymentList", user.getUserPaymentList());
		model.addAttribute("userShippingList", user.getUserShippingList());
		model.addAttribute("orderList", user.getOrderList());
		
		UserShipping userShipping = new UserShipping();
		model.addAttribute("userShipping", userShipping);
		
		model.addAttribute("listOfCreditCards", true);
		model.addAttribute("listOfShippingAddresses", true);
		
		List<String> stateList = INConstants.listOfINStatesCode;
		Collections.sort(stateList);
		model.addAttribute("stateList", stateList);
		model.addAttribute("classActiveEdit", true);
		
		return "myProfile";
	}
	
	
	@RequestMapping("/listOfCreditCards")
	public String listOfCreditCards(Model model, Principal principal, HttpServletRequest request)
	{
		User user = userService.findByUsername(principal.getName());
		model.addAttribute("user", user);
		model.addAttribute("userPaymentList", user.getUserPaymentList());
		model.addAttribute("userShippingList", user.getUserShippingList());
		model.addAttribute("orderList", user.getOrderList());
		
		model.addAttribute("listOfCreditCards", true);
		model.addAttribute("classActiveBilling", true);
		model.addAttribute("listOfShippingAddresses", true);
		
		return "myProfile";
	}
	
	@RequestMapping("/addNewCreditCard")
	public String addNewCreditCard(Model model, Principal principal)
	{
		User user = userService.findByUsername(principal.getName());
		model.addAttribute("user", user);
		
		model.addAttribute("addNewCreditCard", true);
		model.addAttribute("classActiveBilling", true);
		model.addAttribute("listOfShippingAddresses", true);
		
		UserBilling userBilling = new UserBilling();
		UserPayment userPayment = new UserPayment();
		
		model.addAttribute("userBilling", userBilling);
		model.addAttribute("userPayment", userPayment);
		
		List<String> stateList = INConstants.listOfINStatesName;
		Collections.sort(stateList);
		model.addAttribute("stateList", stateList);
		model.addAttribute("userPaymentList", user.getUserPaymentList());
		model.addAttribute("userShippingList", user.getUserShippingList());
		model.addAttribute("orderList", user.getOrderList());
		
		return "myProfile";
	}
	

	@RequestMapping(value="/addNewCreditCard", method=RequestMethod.POST)
	public String addNewCreditCard(@ModelAttribute("userPayment") UserPayment userPayment,
									@ModelAttribute("userBilling") UserBilling userBilling,
									Principal principal, Model model)
	{
		
		User user = userService.findByUsername(principal.getName());
		userService.updateUserBilling(userBilling, userPayment, user);
		
		model.addAttribute("user", user);
		model.addAttribute("userPaymentList", user.getUserPaymentList());
		model.addAttribute("userShippingList", user.getUserShippingList());
		model.addAttribute("listOfCreditCards", true);
		model.addAttribute("classActiveBilling", true);
		model.addAttribute("listOfShippingAddresses", true);
		model.addAttribute("orderList", user.getOrderList());
		
		return "myProfile";
	}
	
	
	
	@RequestMapping("/updateCreditCard")
	public String updateCreditCard(@ModelAttribute("id") Long creditCardId, Principal principal, Model model)
	{
		User user = userService.findByUsername(principal.getName());
		UserPayment userPayment = userPaymentService.findById(creditCardId).orElse(null);
		
		if(user.getId() != userPayment.getUser().getId()) 
		{
			return "badRequestPage";
		} 
		else 
		{
			model.addAttribute("user", user);
			UserBilling userBilling = userPayment.getUserBilling();
			model.addAttribute("userPayment", userPayment);
			model.addAttribute("userBilling", userBilling);
			
			List<String> stateList = INConstants.listOfINStatesName;
			Collections.sort(stateList);
			model.addAttribute("stateList", stateList);
			
			model.addAttribute("addNewCreditCard", true);
			model.addAttribute("classActiveBilling", true);
			model.addAttribute("listOfShippingAddresses", true);
			
			model.addAttribute("userPaymentList", user.getUserPaymentList());
			model.addAttribute("userShippingList", user.getUserShippingList());
			model.addAttribute("orderList", user.getOrderList());
			
			return "myProfile";
		}
	}
	
	
	@RequestMapping("/removeCreditCard")
	public String removeCreditCard(@ModelAttribute("id") Long creditCardId, Principal principal, Model model){
		User user = userService.findByUsername(principal.getName());
		UserPayment userPayment = userPaymentService.findById(creditCardId).orElse(null);
		
		if(user.getId() != userPayment.getUser().getId())
		{
			return "badRequestPage";
		}
		else 
		{
			model.addAttribute("user", user);
			userPaymentService.removeById(creditCardId);
			
			model.addAttribute("listOfCreditCards", true);
			model.addAttribute("classActiveBilling", true);
			model.addAttribute("listOfShippingAddresses", true);
			
			model.addAttribute("userPaymentList", user.getUserPaymentList());
			model.addAttribute("userShippingList", user.getUserShippingList());
			model.addAttribute("orderList", user.getOrderList());
			
			return "redirect:/myProfile";
		}
	}
	
	
	@RequestMapping(value="/setDefaultPayment", method=RequestMethod.POST)
	public String setDefaultPayment(@ModelAttribute("defaultUserPaymentId") Long defaultPaymentId, 
									Principal principal, Model model)
	{
		User user = userService.findByUsername(principal.getName());
		userService.setUserDefaultPayment(defaultPaymentId, user);
		
		model.addAttribute("user", user);
		model.addAttribute("listOfCreditCards", true);
		model.addAttribute("classActiveBilling", true);
		model.addAttribute("listOfShippingAddresses", true);
		
		model.addAttribute("userPaymentList", user.getUserPaymentList());
		model.addAttribute("userShippingList", user.getUserShippingList());
		model.addAttribute("orderList", user.getOrderList());
		
		return "redirect:/myProfile";
	}
	
	
	
	
	
	@RequestMapping("/listOfShippingAddresses")
	public String listOfShippingAddresses(Model model, Principal principal, HttpServletRequest request) 
	{
		User user = userService.findByUsername(principal.getName());
		model.addAttribute("user", user);
		model.addAttribute("userPaymentList", user.getUserPaymentList());
		model.addAttribute("userShippingList", user.getUserShippingList());
		model.addAttribute("orderList", user.getOrderList());
		
		model.addAttribute("listOfCreditCards", true);
		model.addAttribute("classActiveShipping", true);
		model.addAttribute("listOfShippingAddresses", true);
		
		return "myProfile";
	}
	
	
	@RequestMapping("/addNewShippingAddress")
	public String addNewShippingAddress(Model model, Principal principal)
	{
		User user = userService.findByUsername(principal.getName());
		model.addAttribute("user", user);
		
		model.addAttribute("addNewShippingAddress", true);
		model.addAttribute("classActiveShipping", true);
		model.addAttribute("listOfCreditCards", true);
		
		UserShipping userShipping = new UserShipping();
		model.addAttribute("userShipping", userShipping);
		
		List<String> stateList = INConstants.listOfINStatesName;
		Collections.sort(stateList);
		model.addAttribute("stateList", stateList);
		model.addAttribute("userPaymentList", user.getUserPaymentList());
		model.addAttribute("userShippingList", user.getUserShippingList());
		model.addAttribute("orderList", user.getOrderList());
		
		return "myProfile";
	}
	
	@RequestMapping(value="/addNewShippingAddress", method=RequestMethod.POST)
	public String addNewShippingAddressPost(@ModelAttribute("userShipping") UserShipping userShipping,
														Principal principal, Model model)
	{
		User user = userService.findByUsername(principal.getName());
		userService.updateUserShipping(userShipping, user);
		
		model.addAttribute("user", user);
		model.addAttribute("userPaymentList", user.getUserPaymentList());
		model.addAttribute("userShippingList", user.getUserShippingList());
		model.addAttribute("listOfShippingAddresses", true);
		model.addAttribute("classActiveShipping", true);
		model.addAttribute("listOfCreditCards", true);
		model.addAttribute("orderList", user.getOrderList());
		
		return "myProfile";
	}
	
	
	@RequestMapping("/updateUserShipping")
	public String updateUserShipping(@ModelAttribute("id") Long shippingAddressId, Principal principal, Model model)
	{
		User user = userService.findByUsername(principal.getName());
		UserShipping userShipping = userShippingService.findById(shippingAddressId).orElse(null);
		
		if(user.getId() != userShipping.getUser().getId()) 
		{
			return "badRequestPage";
		} 
		else
		{
			model.addAttribute("user", user);
			
			model.addAttribute("userShipping", userShipping);
			
			List<String> stateList = INConstants.listOfINStatesName;
			Collections.sort(stateList);
			model.addAttribute("stateList", stateList);
			
			model.addAttribute("addNewShippingAddress", true);
			model.addAttribute("classActiveShipping", true);
			model.addAttribute("listOfCreditCards", true);
			
			model.addAttribute("userPaymentList", user.getUserPaymentList());
			model.addAttribute("userShippingList", user.getUserShippingList());
			model.addAttribute("orderList", user.getOrderList());
			
			return "myProfile";
		}
	}
	
	
	@RequestMapping("/removeUserShipping")
	public String removeUserShipping(@ModelAttribute("id") Long userShippingId, 
										Principal principal, Model model)
	{
		User user = userService.findByUsername(principal.getName());
		UserShipping userShipping = userShippingService.findById(userShippingId).orElse(null);
		
		if(user.getId() != userShipping.getUser().getId()) {
			return "badRequestPage";
		} 
		else 
		{
			model.addAttribute("user", user);
			
			userShippingService.removeById(userShippingId);
			
			model.addAttribute("listOfShippingAddresses", true);
			model.addAttribute("classActiveShipping", true);
			
			model.addAttribute("userPaymentList", user.getUserPaymentList());
			model.addAttribute("userShippingList", user.getUserShippingList());
			model.addAttribute("orderList", user.getOrderList());
			
			return "myProfile";
		}
	}
	
	@RequestMapping(value="/setDefaultShippingAddress", method=RequestMethod.POST)
	public String setDefaultShippingAddress(@ModelAttribute("defaultShippingAddressId") Long defaultShippingId,
												Principal principal, Model model)
	{
		User user = userService.findByUsername(principal.getName());
		userService.setUserDefaultShipping(defaultShippingId, user);
		
		model.addAttribute("user", user);
		model.addAttribute("listOfCreditCards", true);
		model.addAttribute("classActiveShipping", true);
		model.addAttribute("listOfShippingAddresses", true);
		
		model.addAttribute("userPaymentList", user.getUserPaymentList());
		model.addAttribute("userShippingList", user.getUserShippingList());
		model.addAttribute("orderList", user.getOrderList());
		
		return "myProfile";
	}
	
	
	@PostMapping(value="/updateUserInfo")
	public String updateUserInfo(@ModelAttribute("user") User user,
								 @ModelAttribute("newPassword") String newPassword,
								 Model model) throws Exception
	{
		User currentUser = userService.findById(user.getId()).orElse(null);
		
		if(currentUser == null) {
			throw new Exception ("User not found");
		}
		
		/*check email already exists*/
		if (userService.findByEmail(user.getEmail())!=null) {
			if(userService.findByEmail(user.getEmail()).getId() != currentUser.getId()) {
				model.addAttribute("emailExists", true);
				return "myProfile";
			}
		}
		
		/*check username already exists*/
		if (userService.findByUsername(user.getUsername())!=null) {
			if(userService.findByUsername(user.getUsername()).getId() != currentUser.getId()) {
				model.addAttribute("usernameExists", true);
				return "myProfile";
			}
		}
		
		//update password
		if (newPassword != null && !newPassword.isEmpty() && !newPassword.equals(""))
		{
			BCryptPasswordEncoder passwordEncoder = SecurityUtility.passwordEncoder();
			String dbPassword = currentUser.getPassword();
			if(passwordEncoder.matches(user.getPassword(), dbPassword))
			{
				currentUser.setPassword(passwordEncoder.encode(newPassword));
			}
			else
			{
				model.addAttribute("incorrectPassword", true);
				return "myProfile";
			}
		}
		
		currentUser.setFirstName(user.getFirstName());
		currentUser.setLastName(user.getLastName());
//		currentUser.setUsername(user.getUsername());
//		currentUser.setEmail(user.getEmail());
		currentUser.setPhone(user.getPhone());
		
		userService.save(currentUser);
		
		model.addAttribute("updateSuccess", true);
		model.addAttribute("user", currentUser);
		model.addAttribute("classActiveEdit", true);
		
		model.addAttribute("listOfShippingAddresses", true);
		model.addAttribute("listOfCreditCards", true);
		
		UserDetails userDetails = userSecurityService.loadUserByUsername(currentUser.getUsername());

		Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, userDetails.getPassword(),
				userDetails.getAuthorities());
		
		SecurityContextHolder.getContext().setAuthentication(authentication);
		model.addAttribute("orderList", user.getOrderList());
		
		return "redirect:/myProfile";
	}

	
	@RequestMapping("/orderDetail")
	public String orderDetail(@RequestParam("id") Long orderId,
								Principal principal, Model model)
	{
		User user = userService.findByUsername(principal.getName());
		Order order = orderService.findOne(orderId).orElse(null);
		
		if(order.getUser().getId()!=user.getId()) 
		{
			return "badRequestPage";
		} 
		else
		{
			List<CartItem> cartItemList = cartItemService.findByOrder(order);
			model.addAttribute("cartItemList", cartItemList);
			model.addAttribute("user", user);
			model.addAttribute("order", order);
			
			model.addAttribute("userPaymentList", user.getUserPaymentList());
			model.addAttribute("userShippingList", user.getUserShippingList());
			model.addAttribute("orderList", user.getOrderList());
			
			UserShipping userShipping = new UserShipping();
			model.addAttribute("userShipping", userShipping);
			
			List<String> stateList = INConstants.listOfINStatesName;
			Collections.sort(stateList);
			model.addAttribute("stateList", stateList);
			
			model.addAttribute("listOfShippingAddresses", true);
			model.addAttribute("classActiveOrders", true);
			model.addAttribute("listOfCreditCards", true);
			model.addAttribute("displayOrderDetail", true);
			
			return "myProfile";
		}
	}
	
	
}

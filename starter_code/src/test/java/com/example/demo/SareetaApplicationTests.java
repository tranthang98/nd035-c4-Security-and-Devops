package com.example.demo;

import com.auth0.jwt.JWT;
import com.example.demo.model.persistence.Cart;
import com.example.demo.model.persistence.Item;
import com.example.demo.model.persistence.User;
import com.example.demo.model.persistence.UserOrder;
import com.example.demo.model.persistence.repositories.CartRepository;
import com.example.demo.model.persistence.repositories.ItemRepository;
import com.example.demo.model.persistence.repositories.OrderRepository;
import com.example.demo.model.persistence.repositories.UserRepository;
import com.example.demo.model.requests.ModifyCartRequest;
import com.example.demo.security.SecurityConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static com.auth0.jwt.algorithms.Algorithm.HMAC512;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@RunWith(SpringRunner.class)
@SpringBootTest
public class SareetaApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private CartRepository cartRepository;

    @MockBean
    private ItemRepository itemRepository;

    @MockBean
    private OrderRepository orderRepository;

    @Test
    public void contextLoads() {
    }

    private User user;
    private Item pen;
    private Cart cart;
    private UserOrder userOrder;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        user = new User();
        user.setId(1L);
        user.setUsername("testUser");
        user.setPassword("testPassword");

        pen = new Item();
        pen.setId(1L);
        pen.setName("Pen");
        pen.setPrice(BigDecimal.valueOf(2.5));

        cart = new Cart();
        cart.setId(1L);
        cart.setItems(new ArrayList<>());
        user.setCart(cart);

        userOrder = new UserOrder();
        userOrder.setId(1L);
        userOrder.setUser(user);
        userOrder.setItems(Collections.singletonList(pen));
        userOrder.setTotal(BigDecimal.valueOf(2.5));
    }

    // Test for UserController
    @Test
    public void testCreateUser() throws Exception {
        Mockito.when(userRepository.findByUsername("testUser")).thenReturn(null);
        Mockito.when(userRepository.save(Mockito.any(User.class))).thenReturn(user);

        String requestBody = "{\"username\":\"testUser\", \"password\":\"password123\", \"confirmPassword\":\"password123\"}";

        mockMvc.perform(post("/api/user/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        Mockito.verify(userRepository, times(1)).save(Mockito.any(User.class));
    }

    @Test
    public void testFindUserById() throws Exception {
        Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        String token = generateToken(user.getUsername());
        mockMvc.perform(get("/api/user/id/1")
                        .header(SecurityConstants.HEADER_STRING, SecurityConstants.TOKEN_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("testUser"));

        Mockito.verify(userRepository, times(1)).findById(1L);
    }

    @Test
    public void findUserByUserName_userExists() throws Exception {
        Mockito.when(userRepository.findByUsername("testUser")).thenReturn(user);

        String token = generateToken(user.getUsername());
        mockMvc.perform(get("/api/user/testUser")
                        .header(SecurityConstants.HEADER_STRING, SecurityConstants.TOKEN_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testUser"));

        Mockito.verify(userRepository, times(1)).findByUsername("testUser");
    }

    @Test
    public void findUserByUserName_userDoesNotExist() throws Exception {
        Mockito.when(userRepository.findByUsername("nonExistentUser")).thenReturn(null);

        String token = generateToken(user.getUsername());
        mockMvc.perform(get("/api/user/nonExistentUser")
                        .header(SecurityConstants.HEADER_STRING, SecurityConstants.TOKEN_PREFIX + token))
                .andExpect(status().isNotFound());

        Mockito.verify(userRepository, times(1)).findByUsername("nonExistentUser");
    }

    // Test for CartController
    @Test
    public void testAddToCart() throws Exception {
        Mockito.when(userRepository.findByUsername("testUser")).thenReturn(user);
        Mockito.when(itemRepository.findById(1L)).thenReturn(Optional.of(pen));

        String token = generateToken(user.getUsername());
        String requestBody = "{\"username\":\"testUser\", \"itemId\":1, \"quantity\":1}";
        mockMvc.perform(post("/api/cart/addToCart")
                        .header(SecurityConstants.HEADER_STRING, SecurityConstants.TOKEN_PREFIX + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        Mockito.verify(cartRepository, times(1)).save(Mockito.any());
    }

    @Test
    public void removeFromCart_userExistsAndItemExists() throws Exception {
        ModifyCartRequest request = new ModifyCartRequest();
        request.setUsername("testUser");
        request.setItemId(1L);
        request.setQuantity(1);

        Mockito.when(userRepository.findByUsername("testUser")).thenReturn(user);
        Mockito.when(itemRepository.findById(1L)).thenReturn(Optional.of(pen));

        String token = generateToken(user.getUsername());
        mockMvc.perform(post("/api/cart/removeFromCart")
                        .header(SecurityConstants.HEADER_STRING, SecurityConstants.TOKEN_PREFIX + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isOk());

        Mockito.verify(cartRepository, times(1)).save(cart);
    }

    @Test
    public void removeFromCart_userDoesNotExist() throws Exception {
        ModifyCartRequest request = new ModifyCartRequest();
        request.setUsername("nonExistentUser");
        request.setItemId(1L);
        request.setQuantity(1);

        Mockito.when(userRepository.findByUsername("nonExistentUser")).thenReturn(null);

        String token = generateToken(user.getUsername());
        mockMvc.perform(post("/api/cart/removeFromCart")
                        .header(SecurityConstants.HEADER_STRING, SecurityConstants.TOKEN_PREFIX + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isForbidden());

        Mockito.verify(cartRepository, times(0)).save(cart);
    }

    @Test
    public void removeFromCart_itemDoesNotExist() throws Exception {
        ModifyCartRequest request = new ModifyCartRequest();
        request.setUsername("testUser");
        request.setItemId(999L);
        request.setQuantity(1);

        Mockito.when(userRepository.findByUsername("testUser")).thenReturn(user);
        Mockito.when(itemRepository.findById(999L)).thenReturn(Optional.empty());

        String token = generateToken(user.getUsername());
        mockMvc.perform(post("/api/cart/removeFromCart")
                        .header(SecurityConstants.HEADER_STRING, SecurityConstants.TOKEN_PREFIX + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isNotFound());

        Mockito.verify(cartRepository, times(0)).save(cart);
    }

    @Test
    public void removeFromCart_principalNameDoesNotMatch() throws Exception {
        ModifyCartRequest request = new ModifyCartRequest();
        request.setUsername("testUser");
        request.setItemId(1L);
        request.setQuantity(1);

        String token = generateToken("differentUser");
        mockMvc.perform(post("/api/cart/removeFromCart")
                        .header(SecurityConstants.HEADER_STRING, SecurityConstants.TOKEN_PREFIX + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isForbidden());

        Mockito.verify(cartRepository, times(0)).save(cart);
    }

    // Test for ItemController
    @Test
    public void testGetItems() throws Exception {
        Mockito.when(itemRepository.findAll()).thenReturn(Collections.emptyList());

        String token = generateToken(user.getUsername());
        mockMvc.perform(get("/api/item")
                        .header(SecurityConstants.HEADER_STRING, SecurityConstants.TOKEN_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        Mockito.verify(itemRepository, times(1)).findAll();
    }

    @Test
    public void getItemById() throws Exception {
        Mockito.when(itemRepository.findById(1L)).thenReturn(Optional.of(pen));

        String token = generateToken(user.getUsername());
        mockMvc.perform(get("/api/item/1")
                        .header(SecurityConstants.HEADER_STRING, SecurityConstants.TOKEN_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Pen"));

        Mockito.verify(itemRepository, times(1)).findById(1L);
    }

    @Test
    public void getItemById_itemDoesNotExist() throws Exception {
        Mockito.when(itemRepository.findById(1L)).thenReturn(Optional.empty());

        String token = generateToken(user.getUsername());
        mockMvc.perform(get("/api/item/1")
                        .header(SecurityConstants.HEADER_STRING, SecurityConstants.TOKEN_PREFIX + token))
                .andExpect(status().isNotFound());

        Mockito.verify(itemRepository, times(1)).findById(1L);
    }

    @Test
    public void getItemsByName() throws Exception {
        List<Item> items = Collections.singletonList(pen);
        Mockito.when(itemRepository.findByName("Pen")).thenReturn(items);

        String token = generateToken(user.getUsername());
        mockMvc.perform(get("/api/item/name/Pen")
                        .header(SecurityConstants.HEADER_STRING, SecurityConstants.TOKEN_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Pen"));

        Mockito.verify(itemRepository, times(1)).findByName("Pen");
    }

    @Test
    public void getItemsByName_itemsDoNotExist() throws Exception {
        Mockito.when(itemRepository.findByName("nonExistentItem")).thenReturn(Collections.emptyList());

        String token = generateToken(user.getUsername());
        mockMvc.perform(get("/api/item/name/nonExistentItem")
                        .header(SecurityConstants.HEADER_STRING, SecurityConstants.TOKEN_PREFIX + token))
                .andExpect(status().isNotFound());

        Mockito.verify(itemRepository, times(1)).findByName("nonExistentItem");
    }

    // Test for OrderController
    @Test
    public void testSubmitOrder() throws Exception {
        Mockito.when(userRepository.findByUsername("testUser")).thenReturn(user);

        String token = generateToken(user.getUsername());
        mockMvc.perform(post("/api/order/submit/testUser")
                        .header(SecurityConstants.HEADER_STRING, SecurityConstants.TOKEN_PREFIX + token))
                .andExpect(status().isOk());

        Mockito.verify(orderRepository, times(1)).save(Mockito.any());
    }

    @Test
    public void testGetOrdersForUser() throws Exception {
        Mockito.when(userRepository.findByUsername("testUser")).thenReturn(user);
        Mockito.when(orderRepository.findByUser(user)).thenReturn(Collections.emptyList());

        String token = generateToken(user.getUsername());
        mockMvc.perform(get("/api/order/history/testUser")
                        .header(SecurityConstants.HEADER_STRING, SecurityConstants.TOKEN_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        Mockito.verify(orderRepository, times(1)).findByUser(user);
    }

    @Test
    public void getOrdersForUser_userExistsAndPrincipalMatches() throws Exception {
        List<UserOrder> orderList = Collections.singletonList(userOrder);

        Mockito.when(userRepository.findByUsername("testUser")).thenReturn(user);
        Mockito.when(orderRepository.findByUser(user)).thenReturn(orderList);

        String token = generateToken(user.getUsername());
        mockMvc.perform(get("/api/order/history/testUser")
                        .header(SecurityConstants.HEADER_STRING, SecurityConstants.TOKEN_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(userOrder.getId()));

        Mockito.verify(userRepository, times(1)).findByUsername("testUser");
        Mockito.verify(orderRepository, times(1)).findByUser(user);
    }

    @Test
    public void getOrdersForUser_userExistsAndPrincipalDoesNotMatch() throws Exception {
        String token = generateToken("differentUser");
        mockMvc.perform(get("/api/order/history/testUser")
                        .header(SecurityConstants.HEADER_STRING, SecurityConstants.TOKEN_PREFIX + token))
                .andExpect(status().isForbidden());

        Mockito.verify(userRepository, times(0)).findByUsername("testUser");
        Mockito.verify(orderRepository, times(0)).findByUser(user);
    }

    @Test
    public void getOrdersForUser_userDoesNotExist() throws Exception {
        Mockito.when(userRepository.findByUsername("testUser")).thenReturn(null);

        String token = generateToken(user.getUsername());
        mockMvc.perform(get("/api/order/history/testUser")
                        .header(SecurityConstants.HEADER_STRING, SecurityConstants.TOKEN_PREFIX + token))
                .andExpect(status().isNotFound());

        Mockito.verify(userRepository, times(1)).findByUsername("testUser");
        Mockito.verify(orderRepository, times(0)).findByUser(user);
    }

    // Helper method to generate a JWT token for the user
    private String generateToken(String username) {
        return JWT.create()
                .withSubject(username)
                .withExpiresAt(new Date(System.currentTimeMillis() + SecurityConstants.EXPIRATION_TIME))
                .sign(HMAC512(SecurityConstants.SECRET.getBytes()));
    }
}

package com.example.libreria.service;

import com.example.libreria.dto.ReservationRequestDTO;
import com.example.libreria.dto.ReservationResponseDTO;
import com.example.libreria.dto.ReturnBookRequestDTO;
import com.example.libreria.model.Book;
import com.example.libreria.model.Reservation;
import com.example.libreria.model.User;
import com.example.libreria.repository.BookRepository;
import com.example.libreria.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {
    
    @Mock
    private ReservationRepository reservationRepository;
    
    @Mock
    private BookRepository bookRepository;
    
    @Mock
    private BookService bookService;
    
    @Mock
    private UserService userService;
    
    @InjectMocks
    private ReservationService reservationService;
    
    private User testUser;
    private Book testBook;
    private Reservation testReservation;
    
    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("Juan Pérez");
        testUser.setEmail("juan@example.com");
        
        testBook = new Book();
        testBook.setExternalId(258027L);
        testBook.setTitle("The Lord of the Rings");
        testBook.setPrice(new BigDecimal("15.99"));
        testBook.setStockQuantity(10);
        testBook.setAvailableQuantity(5);
        
        testReservation = new Reservation();
        testReservation.setId(1L);
        testReservation.setUser(testUser);
        testReservation.setBook(testBook);
        testReservation.setRentalDays(7);
        testReservation.setStartDate(LocalDate.now());
        testReservation.setExpectedReturnDate(LocalDate.now().plusDays(7));
        testReservation.setDailyRate(new BigDecimal("15.99"));
        testReservation.setTotalFee(new BigDecimal("111.93"));
        testReservation.setStatus(Reservation.ReservationStatus.ACTIVE);
        testReservation.setCreatedAt(LocalDateTime.now());
    }
    
    @Test
    void testCreateReservation_Success() {
        // TOD: Implementar el test de creación de reserva exitosa
        ReservationRequestDTO requestDTO = new ReservationRequestDTO();
        requestDTO.setUserId(1L);
        requestDTO.setBookExternalId(testBook.getExternalId());
        requestDTO.setRentalDays(7);
        requestDTO.setStartDate(LocalDate.now());
        when(userService.getUserEntity(1L)).thenReturn(testUser);
        when(bookRepository.findByExternalId(testBook.getExternalId())).thenReturn(Optional.ofNullable(testBook));
        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> {
                    Reservation r = invocation.getArgument(0);
                    r.setId(1L);
                    return r;
                });
        ReservationResponseDTO responseDTO = reservationService.createReservation(requestDTO);
        assertNotNull(responseDTO);
        assertEquals(1L, responseDTO.getId());
        assertEquals(testBook.getExternalId(), responseDTO.getBookExternalId());
        assertEquals(5, testBook.getAvailableQuantity());
    }
    
    @Test
    void testCreateReservation_BookNotAvailable() {
        // TOD: Implementar el test de creación de reserva cuando el libro no está disponible
        testBook.setAvailableQuantity(Integer.valueOf(0));
        ReservationRequestDTO requestDTO = new ReservationRequestDTO();
        requestDTO.setUserId(1L);
        requestDTO.setBookExternalId(testBook.getExternalId());
        requestDTO.setRentalDays(5);
        requestDTO.setStartDate(LocalDate.now());
        when(userService.getUserEntity(1L)).thenReturn(testUser);
        when(bookRepository.findByExternalId(testBook.getExternalId())).thenReturn(Optional.ofNullable(testBook));
        Exception exception = assertThrows(RuntimeException.class, () -> {
            reservationService.createReservation(requestDTO);
        });
        String expectedMessage = "El libro no está disponible para reserva";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));
    }
    
    @Test
    void testReturnBook_OnTime() {
        // TOD: Implementar el test de devolución de libro en tiempo
        LocalDate returnDate = testReservation.getExpectedReturnDate();
        ReturnBookRequestDTO returnRequestDTO = new ReturnBookRequestDTO();
        returnRequestDTO.setReturnDate(returnDate);
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(testReservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ReservationResponseDTO responseDTO = reservationService.returnBook(1L, returnRequestDTO);
        assertNotNull(responseDTO);
        assertEquals(Reservation.ReservationStatus.RETURNED, responseDTO.getStatus());
        assertEquals(returnDate, responseDTO.getActualReturnDate());
    }
    
    @Test
    void testReturnBook_Overdue() {
        LocalDate returnDate = testReservation.getExpectedReturnDate().plusDays(3);
        ReturnBookRequestDTO dto = new ReturnBookRequestDTO();
        dto.setReturnDate(returnDate);
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(testReservation));
        doAnswer(invocation -> {
            testBook.setAvailableQuantity(testBook.getAvailableQuantity() + 1);
            return null;
        }).when(bookService).increaseAvailableQuantity(testBook.getExternalId());
        ReservationResponseDTO response = reservationService.returnBook(1L, dto);
        BigDecimal expectedLateFee =
                testBook.getPrice()
                        .multiply(new BigDecimal("0.15"))
                        .multiply(new BigDecimal(3));
        assertNotNull(response);
        assertEquals(returnDate, response.getActualReturnDate());
        assertEquals(0, expectedLateFee.compareTo(response.getLateFee()));
        assertEquals(Reservation.ReservationStatus.OVERDUE, response.getStatus());
        assertEquals(6, testBook.getAvailableQuantity());
        verify(bookRepository).save(testBook);
    }
    
    @Test
    void testGetReservationById_Success() {
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(testReservation));
        
        ReservationResponseDTO result = reservationService.getReservationById(1L);
        
        assertNotNull(result);
        assertEquals(testReservation.getId(), result.getId());
    }
    
    @Test
    void testGetAllReservations() {
        Reservation reservation2 = new Reservation();
        reservation2.setId(2L);
        
        when(reservationRepository.findAll()).thenReturn(Arrays.asList(testReservation, reservation2));
        
        List<ReservationResponseDTO> result = reservationService.getAllReservations();
        
        assertNotNull(result);
        assertEquals(2, result.size());
    }
    
    @Test
    void testGetReservationsByUserId() {
        when(reservationRepository.findByUserId(1L)).thenReturn(Arrays.asList(testReservation));
        
        List<ReservationResponseDTO> result = reservationService.getReservationsByUserId(1L);
        
        assertNotNull(result);
        assertEquals(1, result.size());
    }
    
    @Test
    void testGetActiveReservations() {
        when(reservationRepository.findByStatus(Reservation.ReservationStatus.ACTIVE))
                .thenReturn(Arrays.asList(testReservation));
        
        List<ReservationResponseDTO> result = reservationService.getActiveReservations();
        
        assertNotNull(result);
        assertEquals(1, result.size());
    }
}


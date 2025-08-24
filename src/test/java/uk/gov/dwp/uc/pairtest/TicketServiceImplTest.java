package uk.gov.dwp.uc.pairtest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TicketServiceImplTest {
    private TicketPaymentService paymentService;
    private SeatReservationService reservationService;
    private TicketServiceImpl ticketService;

    @BeforeEach
    void setUp() {
        paymentService = Mockito.mock(TicketPaymentService.class);
        reservationService = Mockito.mock(SeatReservationService.class);
        ticketService = new TicketServiceImpl(paymentService, reservationService);
    }

    @Test
    void shouldPurchaseValidAdultTickets() throws InvalidPurchaseException {
        TicketTypeRequest adultTickets = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2);

        ticketService.purchaseTickets(1L, adultTickets);

        verify(paymentService).makePayment(1L, 50); // 2 * £25
        verify(reservationService).reserveSeat(1L, 2); // 2 adults = 2 seats
    }

    @Test
    void shouldPurchaseAdultAndChildTickets() throws InvalidPurchaseException {
        TicketTypeRequest adult = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);
        TicketTypeRequest child = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 2);

        ticketService.purchaseTickets(1L, adult, child);

        verify(paymentService).makePayment(1L, 55); // 25 + (2*15)
        verify(reservationService).reserveSeat(1L, 3); // 1 adult + 2 children
    }

    @Test
    void shouldPurchaseAdultChildAndInfantTickets() throws InvalidPurchaseException {
        TicketTypeRequest adult = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);
        TicketTypeRequest child = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 1);
        TicketTypeRequest infant = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1);

        ticketService.purchaseTickets(1L, adult, child, infant);

        verify(paymentService).makePayment(1L, 40); // 25 + 15 + 0
        verify(reservationService).reserveSeat(1L, 2); // 1 adult + 1 child (infant no seat)
    }

    @Test
    void shouldRejectPurchaseChildTicketWithoutAdultTickets() {
        TicketTypeRequest child = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 1);

        InvalidPurchaseException ex = assertThrows(
                InvalidPurchaseException.class,
                () -> ticketService.purchaseTickets(1L, child)
        );

        assertEquals("Child or Infant tickets require at least one Adult ticket", ex.getMessage());
        verifyNoInteractions(paymentService, reservationService);
    }

    @Test
    void shouldRejectPurchaseInfantTicketWithoutAdultTickets() {
        TicketTypeRequest infant = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1);

        InvalidPurchaseException ex = assertThrows(
                InvalidPurchaseException.class,
                () -> ticketService.purchaseTickets(1L,infant)
        );

        assertEquals("Child or Infant tickets require at least one Adult ticket", ex.getMessage());
        verifyNoInteractions(paymentService, reservationService);
    }

    @Test
    void shouldRejectPurchaseWhenExceedingMaxTickets() {
        TicketTypeRequest adult = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 26);

        InvalidPurchaseException ex = assertThrows(
                InvalidPurchaseException.class,
                () -> ticketService.purchaseTickets(1L, adult)
        );

        assertEquals("Cannot purchase more than 25 tickets at once", ex.getMessage());
        verifyNoInteractions(paymentService, reservationService);
    }

    @Test
    void shouldRejectInvalidAccountId() {
        TicketTypeRequest adult = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);

        InvalidPurchaseException ex = assertThrows(
                InvalidPurchaseException.class,
                () -> ticketService.purchaseTickets(0L, adult)
        );

        assertEquals("Invalid account id", ex.getMessage());
        verifyNoInteractions(paymentService, reservationService);
    }


    @Test
    void shouldRejectInvalidAccountIdNull() {
        TicketTypeRequest adult = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);

        InvalidPurchaseException ex = assertThrows(
                InvalidPurchaseException.class,
                () -> ticketService.purchaseTickets(null, adult)
        );

        assertEquals("Invalid account id", ex.getMessage());
        verifyNoInteractions(paymentService, reservationService);
    }

    @Test
    void shouldRejectEmptyTicketRequest() {
        InvalidPurchaseException ex = assertThrows(
                InvalidPurchaseException.class,
                () -> ticketService.purchaseTickets(1L)
        );

        assertEquals("At least one ticket must be requested", ex.getMessage());
        verifyNoInteractions(paymentService, reservationService);
    }

    @Test
    void shouldRejectNullTicketRequest() {

        InvalidPurchaseException ex = assertThrows(
                InvalidPurchaseException.class,
                () -> ticketService.purchaseTickets(1L, null)
        );

        assertEquals("At least one ticket must be requested", ex.getMessage());
        verifyNoInteractions(paymentService, reservationService);
    }

    @Test
    void shouldRejectZeroLengthTicketRequest() {

        TicketTypeRequest ticketTypeRequest[] = new TicketTypeRequest[0];
        InvalidPurchaseException ex = assertThrows(
                InvalidPurchaseException.class,
                () -> ticketService.purchaseTickets(1L,ticketTypeRequest)
        );

        assertEquals("At least one ticket must be requested", ex.getMessage());
        verifyNoInteractions(paymentService, reservationService);
    }
}
package uk.gov.dwp.uc.pairtest;

import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import java.util.Arrays;

public class TicketServiceImpl implements TicketService {
    /**
     * Should only have private methods other than the one below.
     */

    private static final int MAX_TICKETS = 25;
    private static final int INFANT_PRICE = 0;
    private static final int CHILD_PRICE = 15;
    private static final int ADULT_PRICE = 25;

    private final TicketPaymentService paymentService;
    private final SeatReservationService reservationService;

    public TicketServiceImpl(TicketPaymentService paymentService,
                             SeatReservationService reservationService) {
        this.paymentService = paymentService;
        this.reservationService = reservationService;
    }


    @Override
    public void purchaseTickets(Long accountId, TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException {
        validateAccountId(accountId);
        validateTicketRequests(ticketTypeRequests);

        int totalTickets = Arrays.stream(ticketTypeRequests)
                .mapToInt(TicketTypeRequest::getNoOfTickets)
                .sum();

        if (totalTickets > MAX_TICKETS) {
            throw new InvalidPurchaseException("Cannot purchase more than 25 tickets at once");
        }

        int adultTickets = getTicketsByTicketType(ticketTypeRequests, TicketTypeRequest.Type.ADULT);
        int childTickets = getTicketsByTicketType(ticketTypeRequests, TicketTypeRequest.Type.CHILD);
        int infantTickets = getTicketsByTicketType(ticketTypeRequests, TicketTypeRequest.Type.INFANT);

        if ((childTickets > 0 || infantTickets > 0) && adultTickets == 0) {
            throw new InvalidPurchaseException("Child or Infant tickets require at least one Adult ticket");
        }

        int totalAmount = calculateTotalAmount(adultTickets, childTickets, infantTickets);
        int totalSeats = adultTickets + childTickets;

        paymentService.makePayment(accountId, totalAmount);
        reservationService.reserveSeat(accountId, totalSeats);
    }


    private void validateAccountId(Long accountId) throws InvalidPurchaseException {
        if (accountId == null || accountId <= 0) {
            throw new InvalidPurchaseException("Invalid account id");
        }
    }

    private void validateTicketRequests(TicketTypeRequest... requests) throws InvalidPurchaseException {
        if (requests == null || requests.length == 0) {
            throw new InvalidPurchaseException("At least one ticket must be requested");
        }
    }

    private int getTicketsByTicketType(TicketTypeRequest[] requests, TicketTypeRequest.Type type) {
        return Arrays.stream(requests)
                .filter(req -> req.getTicketType() == type)
                .mapToInt(TicketTypeRequest::getNoOfTickets)
                .sum();
    }

    private int calculateTotalAmount(int adults, int children, int infants) {
        return (adults * ADULT_PRICE) + (children * CHILD_PRICE) + (infants * INFANT_PRICE);
    }

}

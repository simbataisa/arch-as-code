package com.techcombank.qe.sut.capability.reservation;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * TST-023 concurrent limit capability's HTTP surface. The JMeter module drives
 * these three endpoints; utilisation is sampled continuously throughout the
 * concurrent run, per invariant I2, never only at start and end.
 */
@RestController
public class ReservationController {

    private final ReservationService reservations;

    public ReservationController(ReservationService reservations) {
        this.reservations = reservations;
    }

    /** POST /reservations {account, amount} -> 201 {reservationId}, or 409 at the limit. */
    @PostMapping("/reservations")
    public ResponseEntity<?> reserve(@RequestBody ReserveRequest request) {
        try {
            long id = reservations.reserve(request.account(), request.amount());
            return ResponseEntity.status(HttpStatus.CREATED).body(new ReserveResponse(id));
        } catch (ReservationService.LimitExceeded e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    /** POST /reservations/{id}/release -> 204, or 409 if it is not held (I4). */
    @PostMapping("/reservations/{id}/release")
    public ResponseEntity<?> release(@PathVariable long id) {
        try {
            reservations.release(id);
            return ResponseEntity.noContent().build();
        } catch (ReservationService.NotReleasable e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    /** GET /reservations/utilisation?account=ACC-000001
     *  -> {utilisation, declaredLimit, windowTimezone} */
    @GetMapping("/reservations/utilisation")
    public UtilisationResponse utilisation(@RequestParam String account) {
        return new UtilisationResponse(
            reservations.utilisation(account),
            reservations.declaredLimit(account),
            reservations.windowTimezone(account));
    }

    public record ReserveRequest(String account, long amount) {}

    public record ReserveResponse(long reservationId) {}

    public record UtilisationResponse(long utilisation, long declaredLimit, String windowTimezone) {}
}

package com.example.hms.careflow;

import com.example.hms.careflow.domain.InventoryItem;
import com.example.hms.careflow.service.WorkflowException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.*;

class InventoryItemTest {
    final LocalDate today = LocalDate.of(2030, 1, 1);
    InventoryItem item(int stock) { return new InventoryItem("Synthetic kit", "SYN-1", "LOT-1", "Store A", "Surgical", today.plusDays(30), stock, 2); }

    @Test void availableIsTotalMinusReserved() { var i=item(10);i.reserve(4,today);assertThat(i.getAvailableStock()).isEqualTo(6);assertThat(i.getTotalStock()).isEqualTo(10); }
    @Test void lastUnitCanBeReserved() { var i=item(1);i.reserve(1,today);assertThat(i.getAvailableStock()).isZero(); }
    @Test void oversellLeavesStockUnchanged() { var i=item(1);assertThatThrownBy(()->i.reserve(2,today)).isInstanceOf(WorkflowException.class);assertThat(i.getReservedStock()).isZero(); }
    @Test void secondReservationCannotTakeLastUnit() { var i=item(1);i.reserve(1,today);assertThatThrownBy(()->i.reserve(1,today)).isInstanceOf(WorkflowException.class); }
    @Test void fulfillmentReducesTotalAndReservedEqually() { var i=item(10);i.reserve(4,today);i.fulfill(4,today);assertThat(i.getTotalStock()).isEqualTo(6);assertThat(i.getReservedStock()).isZero();assertThat(i.getAvailableStock()).isEqualTo(6); }
    @Test void cancellationReleasesWithoutConsumingStock() { var i=item(10);i.reserve(4,today);i.release(4);assertThat(i.getTotalStock()).isEqualTo(10);assertThat(i.getAvailableStock()).isEqualTo(10); }
    @Test void cannotReleaseMoreThanReserved() { var i=item(10);i.reserve(1,today);assertThatThrownBy(()->i.release(2)).isInstanceOf(WorkflowException.class);assertThat(i.getReservedStock()).isEqualTo(1); }
    @Test void cannotFulfillUnreservedStock() { var i=item(10);assertThatThrownBy(()->i.fulfill(1,today)).isInstanceOf(WorkflowException.class);assertThat(i.getTotalStock()).isEqualTo(10); }
    @ParameterizedTest @ValueSource(ints={0,-1,Integer.MIN_VALUE}) void nonpositiveReservationsFail(int q) { assertThatThrownBy(()->item(10).reserve(q,today)).isInstanceOf(WorkflowException.class); }
    @ParameterizedTest @ValueSource(ints={0,-1}) void nonpositiveReleaseFails(int q) { assertThatThrownBy(()->item(10).release(q)).isInstanceOf(WorkflowException.class); }
    @Test void expiredLotsAreBlocked() { assertThatThrownBy(()->item(10).reserve(1,today.plusDays(31))).isInstanceOf(WorkflowException.class); }
    @Test void expiryDateItselfIsBlocked() { assertThatThrownBy(()->item(10).reserve(1,today.plusDays(30))).isInstanceOf(WorkflowException.class); }
    @Test void quarantineBlocksReservation() { var i=item(10);i.adjust(0,true);assertThatThrownBy(()->i.reserve(1,today)).isInstanceOf(WorkflowException.class); }
    @Test void quarantineAfterReservationBlocksDistribution() { var i=item(10);i.reserve(1,today);i.adjust(0,true);assertThatThrownBy(()->i.fulfill(1,today)).isInstanceOf(WorkflowException.class);assertThat(i.getReservedStock()).isEqualTo(1); }
    @Test void quarantineStillAllowsRelease() { var i=item(10);i.reserve(1,today);i.adjust(0,true);i.release(1);assertThat(i.getReservedStock()).isZero(); }
    @Test void adjustmentCannotConsumeReservedStock() { var i=item(10);i.reserve(5,today);assertThatThrownBy(()->i.adjust(-6,false)).isInstanceOf(WorkflowException.class);assertThat(i.getTotalStock()).isEqualTo(10); }
    @Test void negativeStockIsRejected() { assertThatThrownBy(()->item(-1)).isInstanceOf(WorkflowException.class); }
    @Test void overflowIsRejected() { var i=item(Integer.MAX_VALUE);assertThatThrownBy(()->i.adjust(1,false)).isInstanceOf(WorkflowException.class);assertThat(i.getTotalStock()).isEqualTo(Integer.MAX_VALUE); }
    @Test void replenishmentPreservesReservations() { var i=item(10);i.reserve(3,today);i.adjust(5,false);assertThat(i.getAvailableStock()).isEqualTo(12);assertThat(i.getReservedStock()).isEqualTo(3); }
}

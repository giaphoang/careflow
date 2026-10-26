package com.example.hms.careflow.domain;

import com.example.hms.careflow.service.WorkflowException;
import jakarta.persistence.*;
import org.hibernate.annotations.Check;
import java.time.LocalDate;

@Entity
@Table(name = "cf_inventory", uniqueConstraints = @UniqueConstraint(columnNames = {"udi", "lot"}))
@Check(constraints = "total_stock >= 0 AND reserved_stock >= 0 AND reserved_stock <= total_stock AND reorder_point >= 0")
public class InventoryItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Version private long version;
    @Column(nullable = false, length = 160) private String name;
    @Column(nullable = false, length = 80) private String udi;
    @Column(nullable = false, length = 80) private String lot;
    @Column(nullable = false, length = 100) private String location;
    @Column(nullable = false, length = 80) private String category;
    @Column(nullable = false) private LocalDate expiresOn;
    @Column(nullable = false) private int totalStock;
    @Column(nullable = false) private int reservedStock;
    @Column(nullable = false) private int reorderPoint;
    @Column(nullable = false) private boolean quarantined;

    protected InventoryItem() {}
    public InventoryItem(String name, String udi, String lot, String location, String category,
                         LocalDate expiresOn, int totalStock, int reorderPoint) {
        if (totalStock < 0 || reorderPoint < 0) throw new WorkflowException("Stock and reorder point cannot be negative");
        this.name = name; this.udi = udi; this.lot = lot; this.location = location;
        this.category = category; this.expiresOn = expiresOn; this.totalStock = totalStock; this.reorderPoint = reorderPoint;
    }
    public void ensureUsable(LocalDate today) {
        if (quarantined) throw new WorkflowException("Lot is quarantined; allocation and distribution are blocked");
        if (!expiresOn.isAfter(today)) throw new WorkflowException("Lot has expired; allocation and distribution are blocked");
    }
    public void reserve(int quantity, LocalDate today) {
        positive(quantity); ensureUsable(today);
        if (quantity > getAvailableStock()) throw new WorkflowException("Insufficient available stock");
        reservedStock += quantity;
    }
    public void release(int quantity) {
        positive(quantity);
        if (quantity > reservedStock) throw new WorkflowException("Reservation exceeds reserved stock");
        reservedStock -= quantity;
    }
    public void fulfill(int quantity, LocalDate today) {
        ensureUsable(today); release(quantity); totalStock -= quantity;
    }
    public void adjust(int delta, boolean quarantine) {
        long next = (long) totalStock + delta;
        if (next < reservedStock || next > Integer.MAX_VALUE) throw new WorkflowException("Adjustment would invalidate stock or reservations");
        totalStock = (int) next; quarantined = quarantine;
    }
    private static void positive(int quantity) {
        if (quantity <= 0) throw new WorkflowException("Quantity must be positive");
    }
    public Long getId() { return id; }
    public long getVersion() { return version; }
    public String getName() { return name; }
    public String getUdi() { return udi; }
    public String getLot() { return lot; }
    public String getLocation() { return location; }
    public String getCategory() { return category; }
    public LocalDate getExpiresOn() { return expiresOn; }
    public int getTotalStock() { return totalStock; }
    public int getReservedStock() { return reservedStock; }
    public int getAvailableStock() { return totalStock - reservedStock; }
    public int getReorderPoint() { return reorderPoint; }
    public boolean isQuarantined() { return quarantined; }
}

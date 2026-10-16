package com.lundi_m.spazacred_api.payment.entity;

import com.lundi_m.spazacred_api.auth.entity.SpazaCredUser;
import com.lundi_m.spazacred_api.config.CreatedAtAuditable;
import com.lundi_m.spazacred_api.credit.entity.CreditTransaction;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.Hibernate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "payment_allocations")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentAllocation extends CreatedAtAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_transaction_id", nullable = false)
    private CreditTransaction creditTransaction;

    @Column(name = "amount_allocated", nullable = false)
    private BigDecimal amountAllocated;

    @Column(name = "reversed_at")
    private Instant reversedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reversed_by")
    private SpazaCredUser reversedBy;

    public PaymentAllocation(Payment payment,
                             CreditTransaction creditTransaction,
                             BigDecimal amountAllocated) {
        this.payment = payment;
        this.creditTransaction = creditTransaction;
        this.amountAllocated = amountAllocated;
    }

    public void reverse(SpazaCredUser reversedBy) {
        this.reversedBy = reversedBy;
        this.reversedAt = Instant.now();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || Hibernate.getClass(this) != Hibernate.getClass(object)) return false;

        PaymentAllocation that = (PaymentAllocation) object;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }
}

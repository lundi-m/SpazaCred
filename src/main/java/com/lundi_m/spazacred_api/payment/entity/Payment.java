package com.lundi_m.spazacred_api.payment.entity;

import com.lundi_m.spazacred_api.auth.entity.SpazaCredUser;
import com.lundi_m.spazacred_api.config.CreatedAtAuditable;
import com.lundi_m.spazacred_api.customer.entity.Customer;
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
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends CreatedAtAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reference_number", nullable = false, unique = true, length = 30)
    private String referenceNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by")
    private SpazaCredUser recordedBy;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voided_by")
    private SpazaCredUser voidedBy;

    @Column(name = "voided_at")
    private Instant voidedAt;

    public Payment(String referenceNumber,
                   Customer customer,
                   SpazaCredUser recordedBy,
                   BigDecimal amount,
                   PaymentMethod paymentMethod) {
        this.referenceNumber = referenceNumber;
        this.customer = customer;
        this.recordedBy = recordedBy;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
    }

    public void voidPayment(SpazaCredUser voidedBy) {
        this.status = PaymentStatus.VOIDED;
        this.voidedBy = voidedBy;
        this.voidedAt = Instant.now();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || Hibernate.getClass(this) != Hibernate.getClass(object)) return false;

        Payment payment = (Payment) object;
        return id != null && Objects.equals(id, payment.id);
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }
}

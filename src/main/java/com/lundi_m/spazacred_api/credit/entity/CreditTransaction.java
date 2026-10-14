package com.lundi_m.spazacred_api.credit.entity;

import com.lundi_m.spazacred_api.auth.entity.SpazaCredUser;
import com.lundi_m.spazacred_api.config.CreatedAtAuditable;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.Hibernate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "credit_transactions")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CreditTransaction extends CreatedAtAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reference_number", nullable = false, unique = true,length = 30)
    private String referenceNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_account_id", nullable = false)
    private CreditAccount creditAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private SpazaCredUser createdBy;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column
    private String description;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;

    @Column(name = "voided_at")
    private Instant voidedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voided_by")
    private SpazaCredUser voidedBy;

    public void voidTransaction(SpazaCredUser voidedBy) {
        this.status = TransactionStatus.VOIDED;
        this.voidedBy = voidedBy;
        this.voidedAt = Instant.now();
    }

    public CreditTransaction(String referenceNumber,
                             CreditAccount creditAccount,
                             SpazaCredUser createdBy,
                             BigDecimal amount,
                             String description, LocalDate dueDate) {
        this.referenceNumber = referenceNumber;
        this.creditAccount = creditAccount;
        this.createdBy = createdBy;
        this.amount = amount;
        this.description = description;
        this.dueDate = dueDate;
        this.status = TransactionStatus.ACTIVE;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || Hibernate.getClass(this) != Hibernate.getClass(object)) return false;

        CreditTransaction that = (CreditTransaction) object;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }
}

package ua.kpi.pastushenko.cosmocats.domain.model;

import java.math.BigDecimal;

public final class OrderItem {

    private final long productId;
    private final String productName;
    private final BigDecimal unitPrice;
    private final int quantity;

    public OrderItem(long productId, String productName, BigDecimal unitPrice, int quantity) {
        DomainChecks.positiveId(productId, "productId");
        DomainChecks.name(productName, 3);
        DomainChecks.quantity(quantity);
        this.productId = productId;
        this.productName = productName;
        this.unitPrice = DomainChecks.price(unitPrice);
        this.quantity = quantity;
    }

    public long getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getLineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}

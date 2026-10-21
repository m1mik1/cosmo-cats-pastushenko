package ua.kpi.pastushenko.cosmocats.domain.model;

public final class CartItem {

    private final long productId;
    private final int quantity;

    public CartItem(long productId, int quantity) {
        DomainChecks.positiveId(productId, "productId");
        DomainChecks.quantity(quantity);
        this.productId = productId;
        this.quantity = quantity;
    }

    public long getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }
}

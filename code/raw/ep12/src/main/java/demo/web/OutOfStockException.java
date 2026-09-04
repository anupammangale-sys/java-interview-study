package demo.web;

public class OutOfStockException extends RuntimeException {
    private final String sku;
    public OutOfStockException(String sku) { super("out of stock: " + sku); this.sku = sku; }
    public String getSku() { return sku; }
}

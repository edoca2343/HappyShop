package ci553.happyshop.catalogue.exceptions;

public class ExcessiveOrderQuantityException extends RuntimeException {
    public ExcessiveOrderQuantityException(String message) {
        super(message);
    }
}

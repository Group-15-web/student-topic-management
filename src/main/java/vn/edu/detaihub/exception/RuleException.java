package vn.edu.detaihub.exception;

public class RuleException extends RuntimeException {
  private final int status;

  public RuleException(String message) {
    this(message, 409);
  }

  public RuleException(String message, int status) {
    super(message);
    this.status = status;
  }

  public int getStatus() {
    return status;
  }

  public static void require(boolean condition, String message) {
    if (!condition) throw new RuleException(message);
  }
}

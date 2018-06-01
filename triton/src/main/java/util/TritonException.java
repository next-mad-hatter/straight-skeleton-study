package at.tugraz.igi.util;

@SuppressWarnings("serial")
public class TritonException extends Exception {
  public TritonException() { super(); }
  public TritonException(String message) { super(message); }
  public TritonException(String message, Throwable cause) { super(message, cause); }
  public TritonException(Throwable cause) { super(cause); }
}


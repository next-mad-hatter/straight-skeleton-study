package at.tugraz.igi.util;

import java.io.IOException;

@SuppressWarnings("serial")
public class ParseException extends IOException {
  public ParseException() { super(); }
  public ParseException(String message) { super(message); }
  public ParseException(String message, Throwable cause) { super(message, cause); }
  public ParseException(Throwable cause) { super(cause); }
}


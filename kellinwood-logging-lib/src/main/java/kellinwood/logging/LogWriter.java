package kellinwood.logging;

/**
 * User: ken
 * Date: 1/30/13
 */
public interface LogWriter {

    public void write(String level, String category, String message, Throwable t);
}

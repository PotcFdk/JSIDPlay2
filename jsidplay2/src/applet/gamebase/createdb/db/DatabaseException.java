package applet.gamebase.createdb.db;

/**
 * The DatabaseException is thrown by the data mover whenever
 * an error occurs.
 * 
 * @author Jeff Heaton (http://www.heatonresearch.com)
 *
 */
public class DatabaseException extends Exception
{
  /**
   * Serial id
   */
  private static final long serialVersionUID = 838904293060250128L;

  
  /**
   * Construct an exception based on another exception.
   * 
   * @param e The other exception.
   */
  public DatabaseException(Exception e)
  {
    super(e);
  }
}

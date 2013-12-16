package code.google.pojomvcc.dummy;

/**
* @author Aidan Morgan
*/
public class DummyCacheElement {
  private String value;

  public DummyCacheElement() {
  }

  public void setValue(String s) {
    this.value = s;
  }

  public String getValue() {
    return value;
  }
}

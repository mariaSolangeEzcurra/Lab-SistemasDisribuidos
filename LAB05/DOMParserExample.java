import org.apache.xerces.parsers.DOMParser;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class DOMParserExample {
    public static void main(String[] args) {
        try {
            DOMParser parser = new DOMParser();
            parser.setFeature("http://xml.org/sax/features/validation", true);
            parser.setFeature("http://apache.org/xml/features/validation/dynamic", true);
            parser.setFeature("http://apache.org/xml/features/validation/schema", false);
            parser.parse("Ej01.xml");
            System.out.println("XML válido según la DTD (DOMParser).");
        } catch (SAXParseException e) {
            System.out.println("Error de validación DOM:");
            System.out.println("Línea: " + e.getLineNumber() + ", Columna: " + e.getColumnNumber());
            System.out.println("Mensaje: " + e.getMessage());
        } catch (SAXException | java.io.IOException e) {
            e.printStackTrace();
        }
    }
}


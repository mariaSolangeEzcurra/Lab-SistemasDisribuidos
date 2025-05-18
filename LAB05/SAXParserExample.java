import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;
import org.apache.xerces.parsers.SAXParser;

public class SAXParserExample {
    public static void main(String[] args) {
        try {
            SAXParser parser = new SAXParser();
            parser.setFeature("http://xml.org/sax/features/validation", true);
            parser.setFeature("http://apache.org/xml/features/validation/dynamic", true);
            parser.setFeature("http://apache.org/xml/features/validation/schema", false);
            parser.setErrorHandler(new DefaultHandler() {
                public void error(SAXParseException e) throws SAXException {
                    System.out.println("Error SAX: " + e.getMessage());
                }

                public void fatalError(SAXParseException e) throws SAXException {
                    System.out.println("Error fatal SAX: " + e.getMessage());
                }
            });
            parser.parse("Ej01.xml");
            System.out.println("XML válido según la DTD (SAXParser).");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

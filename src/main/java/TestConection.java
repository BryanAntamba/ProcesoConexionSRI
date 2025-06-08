import java.io.File;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.xml.sax.SAXParseException;
import org.xml.sax.SAXException;

public class TestConection {

    public static boolean validarXML(String xmlPath, String xsdPath) {
        try {
            File schemaFile = new File(xsdPath);
            File xmlFile = new File(xmlPath);

            if (!schemaFile.exists()) {
                System.err.println("❌ El archivo XSD no existe: " + xsdPath);
                return false;
            }

            if (!xmlFile.exists()) {
                System.err.println("❌ El archivo XML no existe: " + xmlPath);
                return false;
            }

            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = factory.newSchema(schemaFile);
            Validator validator = schema.newValidator();

            // Validación
            validator.validate(new StreamSource(xmlFile));
            System.out.println("✅ XML válido contra el XSD.");
            return true;

        } catch (SAXParseException e) {
            System.err.println("❌ Error de validación:");
            System.err.println("Línea: " + e.getLineNumber());
            System.err.println("Columna: " + e.getColumnNumber());
            System.err.println("Mensaje : " + e.getMessage());
        } catch (SAXException e) {
            System.err.println("❌ Error de validación: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("❌ Error general: " + e.getMessage());
        }
        return false;
    }

    public static void main(String[] args) {
        // Rutas relativas (úselas desde el directorio raíz del proyecto)
        String xmlFile = "src/main/resources/XML/Firmados/factura.xml";
        String xsdFile = "src/main/resources/XSD/factura_V2.0.0.xsd";

        validarXML(xmlFile, xsdFile);
    }
}

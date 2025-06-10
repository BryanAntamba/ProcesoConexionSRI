package wssri;

import ec.gob.sri.ws.recepcion.RecepcionComprobantesOfflineService;
import ec.gob.sri.ws.recepcion.RespuestaSolicitud;

import java.io.File;
import java.nio.file.Files;

public class RecepcionComprobantesOffline {

    private static final String URL_PRUEBAS = "https://celcer.sri.gob.ec/comprobantes-electronicos-ws/RecepcionComprobantesOffline?wsdl";

    public static void enviarComprobante(String rutaArchivoFirmado) {
        System.out.println("Método enviarComprobante iniciado.");
        try {
            File xmlFile = new File(rutaArchivoFirmado);
            if (!xmlFile.exists()) {
                System.err.println("El archivo no existe: " + rutaArchivoFirmado);
                return;
            }

            byte[] archivoBytes = Files.readAllBytes(xmlFile.toPath());
            System.out.println("Archivo leído con éxito, tamaño: " + archivoBytes.length + " bytes.");

            RecepcionComprobantesOfflineService service = new RecepcionComprobantesOfflineService();
            ec.gob.sri.ws.recepcion.RecepcionComprobantesOffline port = service.getRecepcionComprobantesOfflinePort();

            RespuestaSolicitud respuesta = port.validarComprobante(archivoBytes);
            System.out.println("Respuesta del SRI recibida.");

            if (respuesta == null || respuesta.getComprobantes() == null || respuesta.getComprobantes().getComprobante().isEmpty()) {
                System.out.println("No se recibieron comprobantes en la respuesta.");
                return;
            }

            respuesta.getComprobantes().getComprobante().forEach(comp -> {
                System.out.println("Clave de Acceso: " + comp.getClaveAcceso());

                if (comp.getMensajes() != null && comp.getMensajes().getMensaje() != null && !comp.getMensajes().getMensaje().isEmpty()) {
                    comp.getMensajes().getMensaje().forEach(mensaje -> {
                        System.out.println("Mensaje: " + mensaje.getMensaje());
                        System.out.println("Información adicional: " + mensaje.getInformacionAdicional());
                    });
                } else {
                    System.out.println("No hay mensajes para este comprobante.");
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error al enviar comprobante al SRI.");
        }
    }
}
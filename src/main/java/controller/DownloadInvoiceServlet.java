package controller;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import util.Conexion;

import java.io.IOException;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Servlet que genera un PDF de la factura con ID = invoiceId.
 * Utiliza iText para construir el documento.
 */
@WebServlet("/downloadInvoice")
public class DownloadInvoiceServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // 1) Leer el parámetro invoiceId
        String invoiceIdParam = request.getParameter("invoiceId");
        if (invoiceIdParam == null) {
            response.sendRedirect("productos");
            return;
        }

        int invoiceId;
        try {
            invoiceId = Integer.parseInt(invoiceIdParam);
        } catch (NumberFormatException e) {
            response.sendRedirect("productos");
            return;
        }

        // 2) Consultar la cabecera de la factura
        String sqlFactura = "SELECT id, fecha, cliente_nombre, cliente_cedula, total FROM facturas WHERE id = ?";
        // 3) Consultar los detalles: nombre, marca, precio_unitario, cantidad, subtotal
        String sqlDetalles =
                "SELECT p.nombre, p.marca, df.precio_unitario, df.cantidad, df.subtotal " +
                        "FROM detalle_factura df " +
                        "JOIN productos p ON df.producto_id = p.id " +
                        "WHERE df.factura_id = ?";

        // Variables para almacenar datos de la consulta
        Date fechaFactura = null;
        String clienteNombre = "";
        String clienteCedula = "";
        double totalFactura = 0.0;

        // Primera conexión: obtener datos de la cabecera
        try (Connection conn = Conexion.getConnection();
             PreparedStatement psFact = conn.prepareStatement(sqlFactura)) {

            psFact.setInt(1, invoiceId);
            try (ResultSet rsFact = psFact.executeQuery()) {
                if (rsFact.next()) {
                    fechaFactura = rsFact.getTimestamp("fecha");
                    clienteNombre = rsFact.getString("cliente_nombre");
                    clienteCedula = rsFact.getString("cliente_cedula");
                    totalFactura = rsFact.getDouble("total");
                } else {
                    // Si no existe la factura, redirigir a productos
                    response.sendRedirect("productos");
                    return;
                }
            }
        } catch (SQLException e) {
            throw new ServletException("Error al leer datos de facturas: " + e.getMessage(), e);
        }

        // 4) Preparar la respuesta HTTP para un PDF
        response.setContentType("application/pdf");
        // Forzar descarga con nombre "factura_<invoiceId>.pdf"
        response.setHeader("Content-Disposition", "attachment; filename=\"factura_" + invoiceId + ".pdf\"");

        // 5) Construir el PDF con iText
        try {
            Document document = new Document(PageSize.A4, 36, 36, 54, 36);
            PdfWriter.getInstance(document, response.getOutputStream());
            document.open();

            // Fuente estándar
            Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Font fontSubtitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font fontNormal = FontFactory.getFont(FontFactory.HELVETICA, 11);

            // --- A) Agregar encabezado de factura ---
            Paragraph titulo = new Paragraph("Mimir Petshop - FACTURA", fontTitulo);
            titulo.setAlignment(Element.ALIGN_CENTER);
            document.add(titulo);

            document.add(Chunk.NEWLINE);

            // Información de cabecera
            PdfPTable tablaCabecera = new PdfPTable(2);
            tablaCabecera.setWidthPercentage(100);
            tablaCabecera.setWidths(new int[]{1, 2});

            // Fila 1: ID factura
            tablaCabecera.addCell(getCell("ID Factura:", PdfPCell.ALIGN_LEFT, fontSubtitulo));
            tablaCabecera.addCell(getCell(String.valueOf(invoiceId), PdfPCell.ALIGN_LEFT, fontNormal));

            // Fila 2: Fecha
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String fechaStr = (fechaFactura != null) ? sdf.format(fechaFactura) : "";
            tablaCabecera.addCell(getCell("Fecha:", PdfPCell.ALIGN_LEFT, fontSubtitulo));
            tablaCabecera.addCell(getCell(fechaStr, PdfPCell.ALIGN_LEFT, fontNormal));

            // Fila 3: Nombre cliente
            tablaCabecera.addCell(getCell("Cliente:", PdfPCell.ALIGN_LEFT, fontSubtitulo));
            tablaCabecera.addCell(getCell(clienteNombre, PdfPCell.ALIGN_LEFT, fontNormal));

            // Fila 4: Cédula
            tablaCabecera.addCell(getCell("Cédula:", PdfPCell.ALIGN_LEFT, fontSubtitulo));
            tablaCabecera.addCell(getCell(clienteCedula, PdfPCell.ALIGN_LEFT, fontNormal));

            document.add(tablaCabecera);
            document.add(Chunk.NEWLINE);

            // --- B) Agregar tabla con detalles ---
            Paragraph subtitulo = new Paragraph("Detalle de Productos", fontSubtitulo);
            subtitulo.setAlignment(Element.ALIGN_LEFT);
            document.add(subtitulo);
            document.add(Chunk.NEWLINE);

            // Columnas: Producto, Marca, Precio Unitario, Cantidad, Subtotal
            PdfPTable tablaDetalles = new PdfPTable(5);
            tablaDetalles.setWidthPercentage(100);
            tablaDetalles.setWidths(new int[]{3, 2, 2, 1, 2});

            // Encabezados de columna
            tablaDetalles.addCell(getCell("Producto", PdfPCell.ALIGN_CENTER, fontSubtitulo));
            tablaDetalles.addCell(getCell("Marca", PdfPCell.ALIGN_CENTER, fontSubtitulo));
            tablaDetalles.addCell(getCell("Precio Unit.", PdfPCell.ALIGN_CENTER, fontSubtitulo));
            tablaDetalles.addCell(getCell("Cant.", PdfPCell.ALIGN_CENTER, fontSubtitulo));
            tablaDetalles.addCell(getCell("Subtotal", PdfPCell.ALIGN_CENTER, fontSubtitulo));

            // Llenar filas desde la BD
            try (Connection conn = Conexion.getConnection();
                 PreparedStatement psDet = conn.prepareStatement(sqlDetalles)) {

                psDet.setInt(1, invoiceId);
                try (ResultSet rsDet = psDet.executeQuery()) {
                    while (rsDet.next()) {
                        String nombreProd = rsDet.getString("nombre");
                        String marcaProd = rsDet.getString("marca");
                        double precioUnit = rsDet.getDouble("precio_unitario");
                        int cantidad = rsDet.getInt("cantidad");
                        double subtotal = rsDet.getDouble("subtotal");

                        tablaDetalles.addCell(getCell(nombreProd, PdfPCell.ALIGN_LEFT, fontNormal));
                        tablaDetalles.addCell(getCell(marcaProd, PdfPCell.ALIGN_LEFT, fontNormal));
                        tablaDetalles.addCell(getCell(String.format("$ %.2f", precioUnit), PdfPCell.ALIGN_RIGHT, fontNormal));
                        tablaDetalles.addCell(getCell(String.valueOf(cantidad), PdfPCell.ALIGN_CENTER, fontNormal));
                        tablaDetalles.addCell(getCell(String.format("$ %.2f", subtotal), PdfPCell.ALIGN_RIGHT, fontNormal));
                    }
                }
            }

            document.add(tablaDetalles);
            document.add(Chunk.NEWLINE);

            // --- C) Agregar total general al final ---
            Paragraph lineaTotal = new Paragraph(String.format("TOTAL GENERAL: $ %.2f", totalFactura), fontSubtitulo);
            lineaTotal.setAlignment(Element.ALIGN_RIGHT);
            document.add(lineaTotal);

            document.close();
        } catch (DocumentException | SQLException e) {
            throw new ServletException("Error al generar el PDF de la factura: " + e.getMessage(), e);
        }
    }

    /**
     * Método auxiliar para crear celdas de una tabla.
     */
    private PdfPCell getCell(String texto, int align, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, font));
        cell.setPadding(5);
        cell.setHorizontalAlignment(align);
        return cell;
    }
}

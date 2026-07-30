package com.example.bancobienestar.service;

import com.example.bancobienestar.Repository.AbonoCreditoRepository;
import com.example.bancobienestar.entity.AbonoCreditoEntity;
import com.example.bancobienestar.entity.MovimientoEntity;
import com.example.bancobienestar.entity.SolicitudCreditoEntity;
import com.example.bancobienestar.entity.UsuarioEntity;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;

import com.lowagie.text.pdf.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Servicio encargado de generar documentos PDF formales
 * para las solicitudes de credito del banco.
 */
@Service
public class PdfGeneratorService {

    private static final Color PRIMARY_DARK = new Color(6, 26, 64);
    private static final Color PRIMARY_MID = new Color(11, 44, 95);
    private static final Color ACCENT_GOLD = new Color(212, 175, 55);
    private static final Color TEXT_DARK = new Color(33, 37, 41);
    private static final Color TEXT_MUTED = new Color(108, 117, 125);
    private static final Color BG_LIGHT = new Color(248, 249, 250);
    private static final Color BORDER_COLOR = new Color(222, 226, 230);

    private static final Font TITLE_FONT = new Font(Font.HELVETICA, 22, Font.BOLD, PRIMARY_DARK);
    private static final Font SUBTITLE_FONT = new Font(Font.HELVETICA, 14, Font.BOLD, PRIMARY_MID);
    private static final Font SECTION_FONT = new Font(Font.HELVETICA, 12, Font.BOLD, PRIMARY_DARK);
    private static final Font LABEL_FONT = new Font(Font.HELVETICA, 10, Font.BOLD, TEXT_MUTED);
    private static final Font VALUE_FONT = new Font(Font.HELVETICA, 11, Font.NORMAL, TEXT_DARK);

    private static final Logger log = LoggerFactory.getLogger(PdfGeneratorService.class);

    private final AbonoCreditoRepository abonoCreditoRepository;

    public PdfGeneratorService(AbonoCreditoRepository abonoCreditoRepository) {
        this.abonoCreditoRepository = abonoCreditoRepository;
    }

    /**
     * Genera el PDF de una solicitud de credito con formato bancario formal.
     *
     * @param solicitud La entidad de solicitud de credito
     * @return Arreglo de bytes con el contenido del PDF
     */
    public byte[] generarPdfSolicitudCredito(SolicitudCreditoEntity solicitud) throws DocumentException, IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        Document document = new Document(PageSize.LETTER, 50, 50, 50, 50);
        PdfWriter writer = PdfWriter.getInstance(document, baos);
        writer.setPageEvent(new PdfPageEventHandler());

        document.open();

        // ================================================================
        // ENCABEZADO: Logo + Banco + Folio
        // ================================================================
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{1, 2});
        headerTable.setSpacingAfter(10);

        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        logoCell.setPadding(5);

        Paragraph bancoNombre = new Paragraph();
        bancoNombre.add(new Phrase("BANCO DE MEXICO", new Font(Font.HELVETICA, 16, Font.BOLD, PRIMARY_DARK)));
        bancoNombre.add(new Phrase("\n", new Font(Font.HELVETICA, 4)));
        bancoNombre.add(new Phrase("Institucion de Banca Multiple", new Font(Font.HELVETICA, 9, Font.NORMAL, TEXT_MUTED)));
        logoCell.addElement(bancoNombre);

        headerTable.addCell(logoCell);

        PdfPCell folioCell = new PdfPCell();
        folioCell.setBorder(Rectangle.NO_BORDER);
        folioCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        folioCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        folioCell.setPadding(5);

        Paragraph folioPara = new Paragraph();
        folioPara.setAlignment(Element.ALIGN_RIGHT);
        folioPara.add(new Phrase("SOLICITUD DE CREDITO\n", SUBTITLE_FONT));
        folioPara.add(new Phrase("Folio: CRD-" + String.format("%06d", solicitud.getId()) + "\n", new Font(Font.HELVETICA, 10, Font.BOLD, PRIMARY_MID)));
        folioPara.add(new Phrase("Emision: " + java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), new Font(Font.HELVETICA, 8, Font.NORMAL, TEXT_MUTED)));
        folioCell.addElement(folioPara);

        headerTable.addCell(folioCell);
        document.add(headerTable);

        // Linea divisoria decorativa
        PdfPTable divider = new PdfPTable(1);
        divider.setWidthPercentage(100);
        PdfPCell dividerCell = new PdfPCell();
        dividerCell.setBorder(Rectangle.NO_BORDER);
        dividerCell.setFixedHeight(3);
        dividerCell.setBackgroundColor(ACCENT_GOLD);
        divider.addCell(dividerCell);
        document.add(divider);

        document.add(Chunk.NEWLINE);
        document.add(Chunk.NEWLINE);

        // ================================================================
        // TITULO DEL DOCUMENTO
        // ================================================================
        Paragraph docTitle = new Paragraph();
        docTitle.setAlignment(Element.ALIGN_CENTER);
        docTitle.add(new Phrase("SOLICITUD FORMAL DE CREDITO\n", TITLE_FONT));
        docTitle.add(new Phrase("Documento de solicitud y autorizacion de financiamiento", new Font(Font.HELVETICA, 11, Font.NORMAL, TEXT_MUTED)));
        document.add(docTitle);

        document.add(Chunk.NEWLINE);

        // ================================================================
        // SECCION 1: DATOS DEL SOLICITANTE
        // ================================================================
        document.add(crearSeccionHeader("DATOS DEL SOLICITANTE"));

        UsuarioEntity usuario = solicitud.getUsuario();

        PdfPTable datosTable = new PdfPTable(2);
        datosTable.setWidthPercentage(100);
        datosTable.setWidths(new float[]{1, 1});
        datosTable.setSpacingBefore(8);
        datosTable.setSpacingAfter(15);

        // Columna izquierda: Informacion personal
        PdfPCell infoCell = new PdfPCell();
        infoCell.setBorder(Rectangle.BOX);
        infoCell.setBorderColor(BORDER_COLOR);
        infoCell.setPadding(12);
        infoCell.setBackgroundColor(BG_LIGHT);

        infoCell.addElement(crearCampo("Nombre completo", usuario.getNombre()));
        infoCell.addElement(crearCampo("Usuario / Cliente", usuario.getUsername()));
        infoCell.addElement(crearCampo("Rol", "CLIENTE"));
        infoCell.addElement(crearCampo("ID de cliente", "USR-" + String.format("%04d", usuario.getId())));

        datosTable.addCell(infoCell);

        // Columna derecha: Foto del solicitante
        PdfPCell fotoCell = new PdfPCell();
        fotoCell.setBorder(Rectangle.BOX);
        fotoCell.setBorderColor(BORDER_COLOR);
        fotoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        fotoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        fotoCell.setPadding(10);

        if (usuario.getFotoPerfil() != null && !usuario.getFotoPerfil().isEmpty()) {
            log.debug("Procesando foto de perfil para usuario '{}' (solicitud CRD-{}). "
                    + "fotoPerfil.length={}, startsWith data:image={}",
                    usuario.getUsername(),
                    String.format("%06d", solicitud.getId()),
                    usuario.getFotoPerfil().length(),
                    usuario.getFotoPerfil().startsWith("data:image"));
            try {
                Image foto = decodificarImagenBase64(usuario.getFotoPerfil(), 140, 160);
                if (foto != null) {
                    log.debug("Foto de perfil decodificada exitosamente para usuario '{}'", usuario.getUsername());
                    fotoCell.addElement(foto);
                } else {
                    throw new RuntimeException("No se pudo decodificar la imagen");
                }
            } catch (Exception e) {
                log.warn("Error al colocar foto de perfil en PDF para usuario '{}': {}",
                        usuario.getUsername(), e.getMessage());
                Paragraph noFoto = new Paragraph();
                noFoto.setAlignment(Element.ALIGN_CENTER);
                noFoto.add(new Phrase("[ SIN FOTO ]\n", new Font(Font.HELVETICA, 12, Font.BOLD, TEXT_MUTED)));
                noFoto.add(new Phrase("Fotografia del solicitante", new Font(Font.HELVETICA, 9, Font.ITALIC, TEXT_MUTED)));
                fotoCell.addElement(noFoto);
            }
        } else {
            log.warn("Usuario '{}' NO tiene foto de perfil (fotoPerfil es null o vacio). "
                    + "Mostrando placeholder en el PDF.",
                    usuario.getUsername());
            Paragraph noFoto = new Paragraph();
            noFoto.setAlignment(Element.ALIGN_CENTER);
            noFoto.add(new Phrase("[ SIN FOTO ]\n", new Font(Font.HELVETICA, 12, Font.BOLD, TEXT_MUTED)));
            noFoto.add(new Phrase("Fotografia del solicitante", new Font(Font.HELVETICA, 9, Font.ITALIC, TEXT_MUTED)));
            fotoCell.addElement(noFoto);
        }

        Paragraph fotoLabel = new Paragraph("FOTO DEL SOLICITANTE", new Font(Font.HELVETICA, 7, Font.BOLD, TEXT_MUTED));
        fotoLabel.setAlignment(Element.ALIGN_CENTER);
        fotoLabel.setSpacingBefore(6);
        fotoCell.addElement(fotoLabel);

        datosTable.addCell(fotoCell);

        document.add(datosTable);

        // ================================================================
        // SECCION 2: INFORMACION DE LA SOLICITUD
        // ================================================================
        document.add(crearSeccionHeader("INFORMACION DE LA SOLICITUD"));

        // Obtener abonos y calcular saldos
        java.util.List<AbonoCreditoEntity> abonos = abonoCreditoRepository
                .findBySolicitudCreditoOrderByFechaDesc(solicitud);
        double totalAbonado = abonos.stream()
                .mapToDouble(AbonoCreditoEntity::getMontoAbonado).sum();
        double saldoPendiente = solicitud.getSaldoPendiente() != null
                ? solicitud.getSaldoPendiente()
                : ("APROBADO".equals(solicitud.getEstado()) ? solicitud.getMontoSolicitado() : 0.0);

        PdfPTable solicitudTable = new PdfPTable(2);
        solicitudTable.setWidthPercentage(100);
        solicitudTable.setWidths(new float[]{1, 1});
        solicitudTable.setSpacingBefore(8);
        solicitudTable.setSpacingAfter(15);

        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.BOX);
        leftCell.setBorderColor(BORDER_COLOR);
        leftCell.setPadding(12);
        leftCell.setBackgroundColor(BG_LIGHT);

        leftCell.addElement(crearCampo("Monto solicitado", "$ " + String.format("%,.2f", solicitud.getMontoSolicitado())));
        leftCell.addElement(crearCampo("Fecha de solicitud", solicitud.getFecha() != null ?
                solicitud.getFecha().format(DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy, HH:mm")) : "—"));

        // Mostrar informacion de saldos solo si el credito fue aprobado o tiene abonos
        if ("APROBADO".equals(solicitud.getEstado()) || "PAGADO".equals(solicitud.getEstado())
                || totalAbonado > 0 || saldoPendiente > 0) {
            leftCell.addElement(crearCampo("Total abonado a la fecha",
                    "$ " + String.format("%,.2f", totalAbonado)));
        }

        solicitudTable.addCell(leftCell);

        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.BOX);
        rightCell.setBorderColor(BORDER_COLOR);
        rightCell.setPadding(12);
        rightCell.setBackgroundColor(BG_LIGHT);

        String estadoTexto;
        switch (solicitud.getEstado() != null ? solicitud.getEstado() : "PENDIENTE") {
            case "APROBADO":
                estadoTexto = "APROBADO";
                break;
            case "PAGADO":
                estadoTexto = "PAGADO";
                break;
            case "RECHAZADO":
                estadoTexto = "RECHAZADO";
                break;
            default:
                estadoTexto = "PENDIENTE";
                break;
        }
        rightCell.addElement(crearCampo("Estado", estadoTexto));
        rightCell.addElement(crearCampo("Folio de solicitud", "CRD-" + String.format("%06d", solicitud.getId())));

        // Mostrar saldo pendiente si aplica
        if ("APROBADO".equals(solicitud.getEstado()) || "PAGADO".equals(solicitud.getEstado())) {
            if (saldoPendiente > 0) {
                rightCell.addElement(crearCampo("Saldo pendiente",
                        "$ " + String.format("%,.2f", saldoPendiente)));
            } else {
                rightCell.addElement(crearCampo("Saldo pendiente",
                        "$ 0.00 (LIQUIDADO)"));
            }
        }

        solicitudTable.addCell(rightCell);

        document.add(solicitudTable);

        // ================================================================
        // SECCION 3: FIRMA DIGITAL
        // ================================================================
        document.add(crearSeccionHeader("FIRMA DIGITAL DE AUTORIZACION"));

        PdfPTable firmaTable = new PdfPTable(1);
        firmaTable.setWidthPercentage(100);
        firmaTable.setSpacingBefore(8);
        firmaTable.setSpacingAfter(20);

        PdfPCell firmaCell = new PdfPCell();
        firmaCell.setBorder(Rectangle.BOX);
        firmaCell.setBorderColor(BORDER_COLOR);
        firmaCell.setPadding(15);
        firmaCell.setBackgroundColor(BG_LIGHT);
        firmaCell.setHorizontalAlignment(Element.ALIGN_CENTER);

        if (solicitud.getFirmaBase64() != null && !solicitud.getFirmaBase64().isEmpty()) {
            try {
                Image firma = decodificarImagenBase64(solicitud.getFirmaBase64(), 300, 80);
                if (firma != null) {
                    firmaCell.addElement(firma);
                } else {
                    throw new RuntimeException("No se pudo decodificar la firma");
                }
            } catch (Exception e) {
                firmaCell.addElement(new Paragraph("[ No se pudo cargar la imagen de la firma ]", new Font(Font.HELVETICA, 9, Font.ITALIC, TEXT_MUTED)));
            }
        } else {
            firmaCell.addElement(new Paragraph("[ Sin firma registrada ]", new Font(Font.HELVETICA, 9, Font.ITALIC, TEXT_MUTED)));
        }

        Paragraph firmaLabelPara = new Paragraph();
        firmaLabelPara.setAlignment(Element.ALIGN_CENTER);
        firmaLabelPara.setSpacingBefore(8);
        firmaLabelPara.add(new Phrase("Firma digital del solicitante", new Font(Font.HELVETICA, 9, Font.NORMAL, TEXT_MUTED)));
        firmaCell.addElement(firmaLabelPara);

        firmaTable.addCell(firmaCell);
        document.add(firmaTable);

        // ================================================================
        // NOTAS LEGALES
        // ================================================================
        PdfPTable legalTable = new PdfPTable(1);
        legalTable.setWidthPercentage(100);
        legalTable.setSpacingBefore(10);

        PdfPCell legalCell = new PdfPCell();
        legalCell.setBorder(Rectangle.NO_BORDER);
        legalCell.setPadding(8);

        Paragraph legalNotice = new Paragraph();
        legalNotice.add(new Phrase("Notas importantes:\n", new Font(Font.HELVETICA, 8, Font.BOLD, PRIMARY_MID)));
        legalNotice.add(new Phrase("- Este documento constituye una solicitud formal de credito y esta sujeto a evaluacion crediticia.\n", new Font(Font.HELVETICA, 7, Font.NORMAL, TEXT_MUTED)));
        legalNotice.add(new Phrase("- La firma digital aqui plasmada tiene validez legal conforme a las disposiciones aplicables.\n", new Font(Font.HELVETICA, 7, Font.NORMAL, TEXT_MUTED)));
        legalNotice.add(new Phrase("- Banco de Mexico se reserva el derecho de aprobar o rechazar la solicitud basandose en su analisis de credito.\n", new Font(Font.HELVETICA, 7, Font.NORMAL, TEXT_MUTED)));
        legalNotice.add(new Phrase("- Este documento fue generado electronicamente el " + java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'a las' HH:mm")) + ".", new Font(Font.HELVETICA, 7, Font.NORMAL, TEXT_MUTED)));

        legalCell.addElement(legalNotice);
        legalTable.addCell(legalCell);
        document.add(legalTable);

        document.close();

        return baos.toByteArray();
    }

    // ================================================================
    // METODOS AUXILIARES
    // ================================================================

    /**
     * Genera un PDF con el estado de cuenta del cliente: informacion basica
     * y tabla de movimientos recientes.
     *
     * @param usuario       El cliente autenticado
     * @param cuentaClabe    La CLABE de su cuenta principal
     * @param saldo          El saldo actual
     * @param movimientos    Lista de movimientos a incluir
     * @return Arreglo de bytes con el contenido del PDF
     */
    public byte[] generarPdfEstadoCuenta(UsuarioEntity usuario, String cuentaClabe,
                                          Double saldo, List<MovimientoEntity> movimientos)
            throws DocumentException, IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        Document document = new Document(PageSize.LETTER, 50, 50, 50, 50);
        PdfWriter writer = PdfWriter.getInstance(document, baos);
        writer.setPageEvent(new PdfPageEventHandler());

        document.open();

        // ================================================================
        // ENCABEZADO: Logo + Banco + Fecha
        // ================================================================
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{1, 2});
        headerTable.setSpacingAfter(10);

        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        logoCell.setPadding(5);

        Paragraph bancoNombre = new Paragraph();
        bancoNombre.add(new Phrase("BANCO DE MEXICO", new Font(Font.HELVETICA, 16, Font.BOLD, PRIMARY_DARK)));
        bancoNombre.add(new Phrase("\n", new Font(Font.HELVETICA, 4)));
        bancoNombre.add(new Phrase("Institucion de Banca Multiple", new Font(Font.HELVETICA, 9, Font.NORMAL, TEXT_MUTED)));
        logoCell.addElement(bancoNombre);

        headerTable.addCell(logoCell);

        PdfPCell fechaCell = new PdfPCell();
        fechaCell.setBorder(Rectangle.NO_BORDER);
        fechaCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        fechaCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        fechaCell.setPadding(5);

        Paragraph fechaPara = new Paragraph();
        fechaPara.setAlignment(Element.ALIGN_RIGHT);
        fechaPara.add(new Phrase("ESTADO DE CUENTA\n", SUBTITLE_FONT));
        fechaPara.add(new Phrase("Generado: " + java.time.LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + "\n",
                new Font(Font.HELVETICA, 9, Font.NORMAL, TEXT_MUTED)));
        fechaPara.add(new Phrase("Documento electronico",
                new Font(Font.HELVETICA, 8, Font.NORMAL, TEXT_MUTED)));
        fechaCell.addElement(fechaPara);

        headerTable.addCell(fechaCell);
        document.add(headerTable);

        // Linea divisoria decorativa
        PdfPTable divider = new PdfPTable(1);
        divider.setWidthPercentage(100);
        PdfPCell dividerCell = new PdfPCell();
        dividerCell.setBorder(Rectangle.NO_BORDER);
        dividerCell.setFixedHeight(3);
        dividerCell.setBackgroundColor(ACCENT_GOLD);
        divider.addCell(dividerCell);
        document.add(divider);

        document.add(Chunk.NEWLINE);

        // ================================================================
        // TITULO DEL DOCUMENTO
        // ================================================================
        Paragraph docTitle = new Paragraph();
        docTitle.setAlignment(Element.ALIGN_CENTER);
        docTitle.add(new Phrase("ESTADO DE CUENTA BANCARIO\n", TITLE_FONT));
        docTitle.add(new Phrase("Detalle de movimientos y saldos",
                new Font(Font.HELVETICA, 11, Font.NORMAL, TEXT_MUTED)));
        document.add(docTitle);

        document.add(Chunk.NEWLINE);

        // ================================================================
        // SECCION 1: DATOS DEL CLIENTE
        // ================================================================
        document.add(crearSeccionHeader("DATOS DEL CLIENTE"));

        PdfPTable datosTable = new PdfPTable(2);
        datosTable.setWidthPercentage(100);
        datosTable.setWidths(new float[]{1, 1});
        datosTable.setSpacingBefore(8);
        datosTable.setSpacingAfter(15);

        PdfPCell infoCell = new PdfPCell();
        infoCell.setBorder(Rectangle.BOX);
        infoCell.setBorderColor(BORDER_COLOR);
        infoCell.setPadding(12);
        infoCell.setBackgroundColor(BG_LIGHT);

        infoCell.addElement(crearCampo("Nombre completo", usuario.getNombre()));
        infoCell.addElement(crearCampo("Usuario", usuario.getUsername()));
        infoCell.addElement(crearCampo("CLABE", cuentaClabe));
        infoCell.addElement(crearCampo("ID de cliente", "USR-" + String.format("%04d", usuario.getId())));

        datosTable.addCell(infoCell);

        PdfPCell saldoCell = new PdfPCell();
        saldoCell.setBorder(Rectangle.BOX);
        saldoCell.setBorderColor(BORDER_COLOR);
        saldoCell.setPadding(12);
        saldoCell.setBackgroundColor(BG_LIGHT);
        saldoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        Paragraph saldoPara = new Paragraph();
        saldoPara.setAlignment(Element.ALIGN_CENTER);
        saldoPara.add(new Phrase("SALDO DISPONIBLE\n", LABEL_FONT));
        saldoPara.add(new Phrase("$ " + String.format("%,.2f", saldo) + " MXN\n",
                new Font(Font.HELVETICA, 22, Font.BOLD, PRIMARY_DARK)));
        saldoPara.add(new Phrase("Al " + java.time.LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                new Font(Font.HELVETICA, 8, Font.NORMAL, TEXT_MUTED)));
        saldoCell.addElement(saldoPara);

        datosTable.addCell(saldoCell);
        document.add(datosTable);

        // ================================================================
        // SECCION 2: TABLA DE MOVIMIENTOS
        // ================================================================
        document.add(crearSeccionHeader("HISTORIAL DE MOVIMIENTOS"));

        PdfPTable movTable = new PdfPTable(4);
        movTable.setWidthPercentage(100);
        movTable.setWidths(new float[]{1.5f, 3, 1.2f, 1.3f});
        movTable.setSpacingBefore(10);
        movTable.setSpacingAfter(15);

        // Encabezados de la tabla
        Font headerFont = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
        Color headerBg = PRIMARY_MID;

        String[] headers = {"FECHA", "DESCRIPCION", "TIPO", "MONTO"};
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
            cell.setBackgroundColor(headerBg);
            cell.setPadding(8);
            cell.setBorderColor(BORDER_COLOR);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            movTable.addCell(cell);
        }

        // Filas de movimientos
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        if (movimientos == null || movimientos.isEmpty()) {
            PdfPCell emptyCell = new PdfPCell(new Phrase("No hay movimientos registrados",
                    new Font(Font.HELVETICA, 10, Font.ITALIC, TEXT_MUTED)));
            emptyCell.setColspan(4);
            emptyCell.setPadding(20);
            emptyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            emptyCell.setBorderColor(BORDER_COLOR);
            movTable.addCell(emptyCell);
        } else {
            for (MovimientoEntity mov : movimientos) {
                // Fecha
                PdfPCell fechaMovCell = new PdfPCell(new Phrase(
                        mov.getFecha() != null ? mov.getFecha().format(dateFmt) : "—",
                        new Font(Font.HELVETICA, 8, Font.NORMAL, TEXT_DARK)));
                fechaMovCell.setPadding(6);
                fechaMovCell.setBorderColor(BORDER_COLOR);
                movTable.addCell(fechaMovCell);

                // Descripcion
                PdfPCell descCell = new PdfPCell(new Phrase(
                        mov.getDescripcion() != null ? mov.getDescripcion() : "—",
                        new Font(Font.HELVETICA, 8, Font.NORMAL, TEXT_DARK)));
                descCell.setPadding(6);
                descCell.setBorderColor(BORDER_COLOR);
                movTable.addCell(descCell);

                // Tipo
                PdfPCell tipoCell = new PdfPCell(new Phrase(
                        mov.getTipo() != null ? mov.getTipo() : "—",
                        new Font(Font.HELVETICA, 8, Font.NORMAL, TEXT_DARK)));
                tipoCell.setPadding(6);
                tipoCell.setBorderColor(BORDER_COLOR);
                tipoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                movTable.addCell(tipoCell);

                // Monto (con signo segun origen)
                boolean esDebito = cuentaClabe.equals(mov.getCuentaOrigen());
                String montoStr = (esDebito ? "-" : "+") + "$" + String.format("%,.2f", mov.getMonto());
                Font montoFont = new Font(Font.HELVETICA, 8, Font.BOLD,
                        esDebito ? new Color(220, 53, 69) : new Color(25, 135, 84));

                PdfPCell montoCell = new PdfPCell(new Phrase(montoStr, montoFont));
                montoCell.setPadding(6);
                montoCell.setBorderColor(BORDER_COLOR);
                montoCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                movTable.addCell(montoCell);
            }
        }

        document.add(movTable);

        // ================================================================
        // SECCION 3: RESUMEN DE MOVIMIENTOS
        // ================================================================
        if (movimientos != null && !movimientos.isEmpty()) {
            double ingresos = movimientos.stream()
                    .filter(m -> !cuentaClabe.equals(m.getCuentaOrigen()))
                    .mapToDouble(MovimientoEntity::getMonto).sum();
            double egresos = movimientos.stream()
                    .filter(m -> cuentaClabe.equals(m.getCuentaOrigen()))
                    .mapToDouble(MovimientoEntity::getMonto).sum();

            document.add(crearSeccionHeader("RESUMEN DEL PERIODO"));

            PdfPTable resumenTable = new PdfPTable(3);
            resumenTable.setWidthPercentage(100);
            resumenTable.setWidths(new float[]{1, 1, 1});
            resumenTable.setSpacingBefore(10);
            resumenTable.setSpacingAfter(20);

            // Ingresos
            PdfPCell ingCell = new PdfPCell();
            ingCell.setBorder(Rectangle.BOX);
            ingCell.setBorderColor(BORDER_COLOR);
            ingCell.setPadding(10);
            ingCell.setBackgroundColor(BG_LIGHT);
            ingCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            Paragraph ingPara = new Paragraph();
            ingPara.add(new Phrase("INGRESOS\n", LABEL_FONT));
            ingPara.add(new Phrase("$ " + String.format("%,.2f", ingresos) + "\n",
                    new Font(Font.HELVETICA, 14, Font.BOLD, new Color(25, 135, 84))));
            ingCell.addElement(ingPara);
            resumenTable.addCell(ingCell);

            // Egresos
            PdfPCell egCell = new PdfPCell();
            egCell.setBorder(Rectangle.BOX);
            egCell.setBorderColor(BORDER_COLOR);
            egCell.setPadding(10);
            egCell.setBackgroundColor(BG_LIGHT);
            egCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            Paragraph egPara = new Paragraph();
            egPara.add(new Phrase("EGRESOS\n", LABEL_FONT));
            egPara.add(new Phrase("$ " + String.format("%,.2f", egresos) + "\n",
                    new Font(Font.HELVETICA, 14, Font.BOLD, new Color(220, 53, 69))));
            egCell.addElement(egPara);
            resumenTable.addCell(egCell);

            // Total movimientos
            PdfPCell totCell = new PdfPCell();
            totCell.setBorder(Rectangle.BOX);
            totCell.setBorderColor(BORDER_COLOR);
            totCell.setPadding(10);
            totCell.setBackgroundColor(BG_LIGHT);
            totCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            Paragraph totPara = new Paragraph();
            totPara.add(new Phrase("TOTAL MOVIMIENTOS\n", LABEL_FONT));
            totPara.add(new Phrase(String.valueOf(movimientos.size()) + "\n",
                    new Font(Font.HELVETICA, 14, Font.BOLD, PRIMARY_DARK)));
            totCell.addElement(totPara);
            resumenTable.addCell(totCell);

            document.add(resumenTable);
        }

        // ================================================================
        // NOTAS LEGALES
        // ================================================================
        PdfPTable legalTable = new PdfPTable(1);
        legalTable.setWidthPercentage(100);
        legalTable.setSpacingBefore(5);

        PdfPCell legalCell = new PdfPCell();
        legalCell.setBorder(Rectangle.NO_BORDER);
        legalCell.setPadding(8);

        Paragraph legalNotice = new Paragraph();
        legalNotice.add(new Phrase("Notas importantes:\n",
                new Font(Font.HELVETICA, 8, Font.BOLD, PRIMARY_MID)));
        legalNotice.add(new Phrase("- Este documento es un estado de cuenta informativo generado electronicamente.\n",
                new Font(Font.HELVETICA, 7, Font.NORMAL, TEXT_MUTED)));
        legalNotice.add(new Phrase("- Los movimientos aqui mostrados reflejan las operaciones registradas en el sistema.\n",
                new Font(Font.HELVETICA, 7, Font.NORMAL, TEXT_MUTED)));
        legalNotice.add(new Phrase("- Banco de Mexico se reserva el derecho de correccion ante cualquier discrepancia.\n",
                new Font(Font.HELVETICA, 7, Font.NORMAL, TEXT_MUTED)));
        legalNotice.add(new Phrase("- Este documento fue generado el "
                + java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'a las' HH:mm"))
                + ".", new Font(Font.HELVETICA, 7, Font.NORMAL, TEXT_MUTED)));

        legalCell.addElement(legalNotice);
        legalTable.addCell(legalCell);
        document.add(legalTable);

        document.close();

        return baos.toByteArray();
    }

    /**
     * Decodifica una cadena Base64 (con o sin prefijo data:image/...) y devuelve
     * un objeto {@link Image} listo para insertar en el PDF.
     * <p>
     * <b>Mejoras respecto a la implementacion anterior:</b>
     * <ul>
     *   <li>Elimina caracteres whitespace de la cadena Base64 que MySQL puede insertar.</li>
     *   <li>Usa {@link Base64#getMimeDecoder()} que tolera saltos de linea.</li>
     *   <li>Escala la imagen al ancho/alto maximo especificado preservando la proporcion.</li>
     *   <li>Retorna {@code null} si no se puede decodificar (sin lanzar excepcion).</li>
     * </ul>
     *
     * @param dataUri  Cadena Base64, opcionalmente con prefijo {@code data:image/...;base64,}
     * @param maxWidth Ancho maximo en puntos PDF al que escalar la imagen
     * @param maxHeight Alto maximo en puntos PDF al que escalar la imagen
     * @return Objeto {@link Image} listo para usarse, o {@code null} si no se pudo decodificar
     */
    private Image decodificarImagenBase64(String dataUri, float maxWidth, float maxHeight) {
        if (dataUri == null || dataUri.isBlank()) {
            log.warn("decodificarImagenBase64 recibio dataUri null o blank");
            return null;
        }

        try {
            // 1. Limpiar la cadena: eliminar whitespace que MySQL pueda haber insertado
            String limpia = dataUri.trim();
            log.debug("dataUri.trim() length={}, startsWith data:image={}",
                    limpia.length(), limpia.startsWith("data:image"));

            // 2. Remover el prefijo data:image/...;base64, si existe
            if (limpia.startsWith("data:image")) {
                int comaIndex = limpia.indexOf(",");
                if (comaIndex >= 0 && comaIndex < limpia.length() - 1) {
                    limpia = limpia.substring(comaIndex + 1);
                    log.debug("Prefijo data:image removido. Nueva longitud={}", limpia.length());
                } else {
                    log.warn("dataUri tiene prefijo data:image pero no se encontro coma valida. "
                            + "comaIndex={}, length={}", comaIndex, limpia.length());
                    return null;
                }
            } else {
                log.debug("dataUri NO tiene prefijo data:image, se trata como base64 puro");
            }

            // 3. Eliminar cualquier caracter de whitespace que pueda quedar
            limpia = limpia.replaceAll("\\s+", "");
            log.debug("Despues de limpiar whitespace: length={}", limpia.length());

            // 4. Verificar que la cadena resultante sea valida
            if (limpia.isEmpty()) {
                log.warn("La cadena base64 quedo vacia despues de limpiar");
                return null;
            }

            // 5. Decodificar usando MimeDecoder (tolerante a saltos de linea)
            byte[] imageBytes = Base64.getMimeDecoder().decode(limpia);
            log.debug("Base64 decodificado exitosamente: {} bytes", imageBytes.length);

            if (imageBytes.length == 0) {
                log.warn("La decodificacion produjo 0 bytes");
                return null;
            }

            // 6. Intentar crear la imagen con OpenPDF (soporta PNG, JPG, GIF, BMP)
            try {
                Image imagen = Image.getInstance(imageBytes);
                imagen.scaleToFit(maxWidth, maxHeight);
                imagen.setAlignment(Element.ALIGN_CENTER);
                log.debug("Imagen OpenPDF creada exitosamente: {}x{} pts",
                        imagen.getScaledWidth(), imagen.getScaledHeight());
                return imagen;
            } catch (Exception e) {
                // 6b. Si OpenPDF no reconoce el formato, detectar el formato real
                String formatoDetectado = detectarFormatoImagen(imageBytes);
                log.warn("OpenPDF no reconoce la imagen. Bytes: {}, formato detectado: {}. "
                        + "Error: {}", imageBytes.length, formatoDetectado, e.getMessage());

                // 6c. Intentar con ImageIO (lee mas formatos) y convertir a PNG
                try {
                    BufferedImage buffered = ImageIO.read(new ByteArrayInputStream(imageBytes));
                    if (buffered == null) {
                        log.warn("ImageIO tampoco pudo leer la imagen (devuelve null). "
                                + "Formato no soportado: {}", formatoDetectado);
                        return null;
                    }

                    // Convertir el BufferedImage a PNG
                    ByteArrayOutputStream pngBaos = new ByteArrayOutputStream();
                    ImageIO.write(buffered, "png", pngBaos);
                    byte[] pngBytes = pngBaos.toByteArray();
                    log.debug("Imagen convertida a PNG via ImageIO: {} -> {} bytes",
                            imageBytes.length, pngBytes.length);

                    // Crear la imagen PNG con OpenPDF
                    Image imagen = Image.getInstance(pngBytes);
                    imagen.scaleToFit(maxWidth, maxHeight);
                    imagen.setAlignment(Element.ALIGN_CENTER);
                    log.debug("Imagen convertida insertada exitosamente: {}x{} pts",
                            imagen.getScaledWidth(), imagen.getScaledHeight());
                    return imagen;
                } catch (Exception e2) {
                    log.warn("ImageIO tampoco pudo convertir la imagen. "
                            + "Formato: {}. Error: {}: {}",
                            formatoDetectado, e2.getClass().getSimpleName(), e2.getMessage());
                    return null;
                }
            }

        } catch (Exception e) {
            String primerosChars = dataUri.length() > 60
                    ? dataUri.substring(0, 60) + "..."
                    : dataUri;
            log.warn("No se pudo decodificar la imagen en el PDF. "
                    + "dataUri (inicio): [{}]. Error: {}: {}",
                    primerosChars, e.getClass().getSimpleName(), e.getMessage());
            return null;
        }
    }

    /**
     * Detecta el formato de una imagen a partir de sus primeros bytes (magic bytes).
     */
    private String detectarFormatoImagen(byte[] bytes) {
        if (bytes == null || bytes.length < 4) {
            return "desconocido (muy pocos bytes)";
        }
        // Magic bytes conocidos
        if (bytes[0] == (byte) 0x89 && bytes[1] == (byte) 0x50 && bytes[2] == (byte) 0x4E && bytes[3] == (byte) 0x47) {
            return "PNG";
        }
        if (bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8 && bytes[2] == (byte) 0xFF) {
            return "JPEG";
        }
        if (bytes[0] == (byte) 0x47 && bytes[1] == (byte) 0x49 && bytes[2] == (byte) 0x46) {
            return "GIF";
        }
        if (bytes[0] == (byte) 0x42 && bytes[1] == (byte) 0x4D) {
            return "BMP";
        }
        // WebP: RIFF....WEBP
        if (bytes[0] == (byte) 0x52 && bytes[1] == (byte) 0x49 && bytes[2] == (byte) 0x46 && bytes[3] == (byte) 0x46) {
            if (bytes.length >= 12 && bytes[8] == (byte) 0x57 && bytes[9] == (byte) 0x45
                    && bytes[10] == (byte) 0x42 && bytes[11] == (byte) 0x50) {
                return "WebP";
            }
        }
        // AVIF / HEIF: ....ftyp....
        if (bytes.length >= 12 && bytes[4] == (byte) 0x66 && bytes[5] == (byte) 0x74
                && bytes[6] == (byte) 0x79 && bytes[7] == (byte) 0x70) {
            String brand = new String(bytes, 8, 4, StandardCharsets.US_ASCII);
            if ("avif".equals(brand) || "avis".equals(brand)) {
                return "AVIF";
            }
            if ("heic".equals(brand) || "heix".equals(brand) || "mif1".equals(brand) || "msf1".equals(brand)) {
                return "HEIF/HEIC";
            }
            return "ISOBMFF/HEIF (" + brand + ")";
        }
        return String.format("desconocido (primeros bytes: %02X %02X %02X %02X)",
                bytes[0], bytes[1], bytes[2], bytes[3]);
    }

    /**
     * Crea el encabezado de una seccion con linea decorativa dorada.
     */
    private PdfPTable crearSeccionHeader(String titulo) {
        PdfPTable sectionTable = new PdfPTable(1);
        sectionTable.setWidthPercentage(100);

        PdfPCell titleCell = new PdfPCell();
        titleCell.setBorder(Rectangle.NO_BORDER);
        titleCell.setPaddingBottom(2);
        Paragraph sectionPara = new Paragraph();
        sectionPara.add(new Phrase(titulo, SECTION_FONT));
        titleCell.addElement(sectionPara);
        sectionTable.addCell(titleCell);

        PdfPCell lineCell = new PdfPCell();
        lineCell.setBorder(Rectangle.NO_BORDER);
        lineCell.setFixedHeight(3);
        lineCell.setBackgroundColor(ACCENT_GOLD);
        sectionTable.addCell(lineCell);

        return sectionTable;
    }

    /**
     * Crea un par label:value para mostrar en el PDF.
     */
    private Paragraph crearCampo(String label, String value) {
        Paragraph p = new Paragraph();
        p.setSpacingBefore(6);
        p.setSpacingAfter(6);
        p.add(new Phrase(label.toUpperCase() + ":\n", LABEL_FONT));
        p.add(new Phrase(value, VALUE_FONT));
        return p;
    }

    // ================================================================
    // EVENT HANDLER PARA ENCABEZADO, PIE DE PAGINA Y MARCA DE AGUA
    // ================================================================

    private static class PdfPageEventHandler extends PdfPageEventHelper {

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();

            // ---- ENCABEZADO (solo una linea fina) ----
            cb.saveState();
            cb.setColorStroke(ACCENT_GOLD);
            cb.setLineWidth(0.5f);
            cb.moveTo(document.leftMargin(), document.top() + 10);
            cb.lineTo(document.right(), document.top() + 10);
            cb.stroke();
            cb.restoreState();

            // ---- PIE DE PAGINA ----
            try {
                cb.saveState();
                cb.beginText();
                cb.setFontAndSize(BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.EMBEDDED), 7);
                cb.setColorFill(TEXT_MUTED);

                String leftText = "Banco de Mexico - Institucion de Banca Multiple";
                cb.showTextAligned(Element.ALIGN_LEFT, leftText,
                        document.leftMargin(), document.bottom() - 10, 0);

                cb.showTextAligned(Element.ALIGN_RIGHT,
                        "Pagina " + writer.getPageNumber(),
                        document.right(), document.bottom() - 10, 0);

                cb.showTextAligned(Element.ALIGN_CENTER,
                        "Documento generado electronicamente",
                        (document.leftMargin() + document.right()) / 2,
                        document.bottom() - 10, 0);

                cb.endText();
                cb.restoreState();
            } catch (Exception e) {
                // Silencioso
            }

            // Linea separadora del pie
            cb.saveState();
            cb.setColorStroke(BORDER_COLOR);
            cb.setLineWidth(0.3f);
            cb.moveTo(document.leftMargin(), document.bottom() - 5);
            cb.lineTo(document.right(), document.bottom() - 5);
            cb.stroke();
            cb.restoreState();
        }

        @Override
        public void onOpenDocument(PdfWriter writer, Document document) {
            // No se requiere accion
        }

        @Override
        public void onStartPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContentUnder();
            cb.saveState();

            try {
                BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
                cb.beginText();
                cb.setFontAndSize(bf, 52);
                cb.setColorFill(new Color(240, 240, 240));

                float x = (document.leftMargin() + document.right()) / 2;
                float y = (document.top() + document.bottom()) / 2;
                cb.showTextAligned(Element.ALIGN_CENTER, "BANCO DE MEXICO", x, y, 45);

                cb.endText();
            } catch (Exception e) {
                // Silencioso
            }

            cb.restoreState();
        }
    }
}

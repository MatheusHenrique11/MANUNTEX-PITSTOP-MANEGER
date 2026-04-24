package com.manutex.pitstop.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.manutex.pitstop.web.dto.ProducaoMecanicoResponse;
import com.manutex.pitstop.web.dto.ServicoMecanicoItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
public class PdfRelatorioService {

    private static final Color COR_CABECALHO   = new Color(30, 64, 95);
    private static final Color COR_SUBHEADER   = new Color(52, 101, 143);
    private static final Color COR_META_BATIDA = new Color(39, 174, 96);
    private static final Color COR_META_FALTA  = new Color(192, 57, 43);
    private static final Color COR_LINHA_PAR   = new Color(245, 247, 250);
    private static final Color COR_BRANCO      = Color.WHITE;

    private static final Font FONTE_TITULO     = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, COR_BRANCO);
    private static final Font FONTE_SUBTITULO  = FontFactory.getFont(FontFactory.HELVETICA, 11, COR_BRANCO);
    private static final Font FONTE_SECAO      = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, COR_SUBHEADER);
    private static final Font FONTE_COL        = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, COR_BRANCO);
    private static final Font FONTE_CELULA     = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);
    private static final Font FONTE_CELULA_B   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.DARK_GRAY);
    private static final Font FONTE_RODAPE     = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);

    public byte[] gerarRelatorioMensal(int mes, int ano, String nomeEmpresa,
                                       List<ProducaoMecanicoResponse> producoes) {
        try (var baos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 40, 40, 50, 50);
            PdfWriter writer = PdfWriter.getInstance(doc, baos);
            writer.setPageEvent(new RodapeHandler(mes, ano, nomeEmpresa));

            doc.open();
            adicionarCabecalho(doc, mes, ano, nomeEmpresa);
            adicionarResumoGeral(doc, producoes, mes, ano);

            for (ProducaoMecanicoResponse p : producoes) {
                adicionarSecaoMecanico(doc, p);
            }

            doc.close();
            log.info("PDF de metas gerado: mes={}, ano={}, mecanicos={}", mes, ano, producoes.size());
            return baos.toByteArray();
        } catch (Exception e) {
            throw new PdfGenerationException("Erro ao gerar relatório PDF: " + e.getMessage(), e);
        }
    }

    private void adicionarCabecalho(Document doc, int mes, int ano, String nomeEmpresa) throws DocumentException {
        PdfPTable header = new PdfPTable(1);
        header.setWidthPercentage(100);

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(COR_CABECALHO);
        cell.setPadding(16);
        cell.setBorder(Rectangle.NO_BORDER);

        Paragraph titulo = new Paragraph("Relatório de Metas — Mecânicos", FONTE_TITULO);
        titulo.setAlignment(Element.ALIGN_CENTER);
        Paragraph sub = new Paragraph(
            nomeEmpresa + "   |   " + nomeMes(mes) + " de " + ano, FONTE_SUBTITULO);
        sub.setAlignment(Element.ALIGN_CENTER);

        cell.addElement(titulo);
        cell.addElement(sub);
        header.addCell(cell);

        doc.add(header);
        doc.add(Chunk.NEWLINE);
    }

    private void adicionarResumoGeral(Document doc, List<ProducaoMecanicoResponse> producoes,
                                      int mes, int ano) throws DocumentException {
        Paragraph tituloSecao = new Paragraph("Resumo Geral — " + nomeMes(mes) + "/" + ano, FONTE_SECAO);
        tituloSecao.setSpacingBefore(8);
        tituloSecao.setSpacingAfter(6);
        doc.add(tituloSecao);

        PdfPTable tabela = new PdfPTable(new float[]{3f, 2f, 2f, 2f, 2f, 1.5f});
        tabela.setWidthPercentage(100);

        String[] colunas = {"Mecânico", "Meta (R$)", "Produzido (R$)", "% Atingido", "Qtd. OS", "Status"};
        for (String col : colunas) {
            adicionarCelulaCabecalho(tabela, col);
        }

        boolean par = false;
        BigDecimal totalMeta = BigDecimal.ZERO;
        BigDecimal totalProduzido = BigDecimal.ZERO;
        int totalOs = 0;
        int bateram = 0;

        for (ProducaoMecanicoResponse p : producoes) {
            Color fundo = par ? COR_LINHA_PAR : COR_BRANCO;
            adicionarCelula(tabela, p.mecanicoNome(), fundo, false);
            adicionarCelula(tabela, formatarMoeda(p.valorMeta()), fundo, false);
            adicionarCelula(tabela, formatarMoeda(p.totalValorProduzido()), fundo, false);
            adicionarCelula(tabela, String.format("%.1f%%", p.percentualAtingido()), fundo, false);
            adicionarCelula(tabela, String.valueOf(p.totalServicos()), fundo, false);

            PdfPCell statusCell = new PdfPCell(new Phrase(
                p.metaBatida() ? "Atingiu" : "Pendente",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8,
                    p.metaBatida() ? COR_META_BATIDA : COR_META_FALTA)
            ));
            statusCell.setBackgroundColor(fundo);
            statusCell.setPadding(5);
            statusCell.setBorderColor(new Color(220, 220, 220));
            tabela.addCell(statusCell);

            totalMeta = totalMeta.add(p.valorMeta() != null ? p.valorMeta() : BigDecimal.ZERO);
            totalProduzido = totalProduzido.add(p.totalValorProduzido() != null ? p.totalValorProduzido() : BigDecimal.ZERO);
            totalOs += p.totalServicos();
            if (p.metaBatida()) bateram++;
            par = !par;
        }

        adicionarCelulaTotais(tabela, "TOTAL", true);
        adicionarCelulaTotais(tabela, formatarMoeda(totalMeta), false);
        adicionarCelulaTotais(tabela, formatarMoeda(totalProduzido), false);
        adicionarCelulaTotais(tabela, bateram + "/" + producoes.size() + " metas", false);
        adicionarCelulaTotais(tabela, String.valueOf(totalOs), false);
        adicionarCelulaTotais(tabela, "", false);

        doc.add(tabela);
        doc.add(Chunk.NEWLINE);
    }

    private void adicionarSecaoMecanico(Document doc, ProducaoMecanicoResponse p) throws DocumentException {
        Color corStatus = p.metaBatida() ? COR_META_BATIDA : COR_META_FALTA;
        String statusLabel = p.metaBatida() ? "META ATINGIDA" : "META NÃO ATINGIDA";

        Paragraph titulo = new Paragraph(
            p.mecanicoNome() + "   [" + statusLabel + "]",
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, corStatus)
        );
        titulo.setSpacingBefore(12);
        titulo.setSpacingAfter(4);
        doc.add(titulo);

        Paragraph resumo = new Paragraph(
            "Meta: " + formatarMoeda(p.valorMeta()) +
            "   |   Produzido: " + formatarMoeda(p.totalValorProduzido()) +
            "   |   " + String.format("%.1f%%", p.percentualAtingido()) + " atingido" +
            "   |   " + p.totalServicos() + " ordens concluídas",
            FONTE_CELULA
        );
        resumo.setSpacingAfter(5);
        doc.add(resumo);

        if (p.servicos() == null || p.servicos().isEmpty()) {
            doc.add(new Paragraph("Nenhum serviço concluído neste período.", FONTE_RODAPE));
            doc.add(Chunk.NEWLINE);
            return;
        }

        PdfPTable tabela = new PdfPTable(new float[]{1.5f, 2f, 3f, 2f, 2f});
        tabela.setWidthPercentage(98);

        String[] colunas = {"Placa", "Veículo", "Descrição", "Valor Final (R$)", "Conclusão"};
        for (String col : colunas) {
            adicionarCelulaCabecalhoSec(tabela, col);
        }

        boolean par = false;
        for (ServicoMecanicoItem s : p.servicos()) {
            Color fundo = par ? COR_LINHA_PAR : COR_BRANCO;
            adicionarCelula(tabela, s.veiculoPlaca(), fundo, false);
            adicionarCelula(tabela, s.veiculoMarca() + " " + s.veiculoModelo(), fundo, false);
            adicionarCelula(tabela, truncar(s.descricao(), 60), fundo, false);
            adicionarCelula(tabela, formatarMoeda(s.valorFinal()), fundo, true);
            adicionarCelula(tabela, s.dataConclusao() != null
                ? s.dataConclusao().toString().substring(0, 10) : "—", fundo, false);
            par = !par;
        }

        doc.add(tabela);
        doc.add(Chunk.NEWLINE);
    }

    private void adicionarCelulaCabecalho(PdfPTable t, String texto) {
        PdfPCell c = new PdfPCell(new Phrase(texto, FONTE_COL));
        c.setBackgroundColor(COR_CABECALHO);
        c.setPadding(6);
        c.setBorder(Rectangle.NO_BORDER);
        t.addCell(c);
    }

    private void adicionarCelulaCabecalhoSec(PdfPTable t, String texto) {
        PdfPCell c = new PdfPCell(new Phrase(texto, FONTE_COL));
        c.setBackgroundColor(COR_SUBHEADER);
        c.setPadding(5);
        c.setBorder(Rectangle.NO_BORDER);
        t.addCell(c);
    }

    private void adicionarCelula(PdfPTable t, String texto, Color fundo, boolean negrito) {
        PdfPCell c = new PdfPCell(new Phrase(texto != null ? texto : "—", negrito ? FONTE_CELULA_B : FONTE_CELULA));
        c.setBackgroundColor(fundo);
        c.setPadding(5);
        c.setBorderColor(new Color(220, 220, 220));
        t.addCell(c);
    }

    private void adicionarCelulaTotais(PdfPTable t, String texto, boolean primeiro) {
        Font fonte = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, COR_CABECALHO);
        PdfPCell c = new PdfPCell(new Phrase(texto, fonte));
        c.setBackgroundColor(new Color(220, 230, 242));
        c.setPadding(6);
        c.setBorderColor(new Color(180, 200, 220));
        t.addCell(c);
    }

    private String formatarMoeda(BigDecimal valor) {
        if (valor == null) return "R$ 0,00";
        return String.format("R$ %,.2f", valor).replace(",", "X").replace(".", ",").replace("X", ".");
    }

    private String nomeMes(int mes) {
        Locale ptBR = Locale.of("pt", "BR");
        String nome = Month.of(mes).getDisplayName(TextStyle.FULL, ptBR);
        return nome.substring(0, 1).toUpperCase() + nome.substring(1);
    }

    private String truncar(String texto, int max) {
        if (texto == null) return "";
        return texto.length() <= max ? texto : texto.substring(0, max - 3) + "...";
    }

    private static class RodapeHandler extends PdfPageEventHelper {
        private final String nomeEmpresa;

        RodapeHandler(int mes, int ano, String nomeEmpresa) {
            this.nomeEmpresa = nomeEmpresa;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            Font fonte = FontFactory.getFont(FontFactory.HELVETICA, 7, Color.GRAY);
            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                new Phrase("Manutex PitStop Manager  |  " + nomeEmpresa +
                           "  |  Gerado automaticamente para uso do RH  |  Página " + writer.getPageNumber(),
                    fonte),
                (document.left() + document.right()) / 2f, document.bottom() - 10, 0);
        }
    }

    public static class PdfGenerationException extends RuntimeException {
        public PdfGenerationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

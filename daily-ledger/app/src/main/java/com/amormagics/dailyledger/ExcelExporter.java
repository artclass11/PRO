package com.amormagics.dailyledger;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class ExcelExporter {
    private static final String MIME =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String FILE_NAME = "Daily_Ledger.xlsx";

    private ExcelExporter() {}

    public static String export(Context context, List<Entry> source) throws Exception {
        if (Build.VERSION.SDK_INT < 29) {
            throw new IllegalStateException("Android 10 or newer is required for automatic Downloads sync.");
        }

        List<Entry> entries = new ArrayList<>(source);
        entries.sort(Comparator.comparing((Entry e) -> e.date).reversed()
                .thenComparingLong(e -> e.id).reversed());

        ContentResolver resolver = context.getContentResolver();
        Uri collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI;

        try (android.database.Cursor cursor = resolver.query(
                collection,
                new String[]{MediaStore.MediaColumns._ID},
                MediaStore.MediaColumns.DISPLAY_NAME + "=?",
                new String[]{FILE_NAME},
                null)) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(0);
                    resolver.delete(Uri.withAppendedPath(collection, Long.toString(id)), null, null);
                }
            }
        }

        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, FILE_NAME);
        values.put(MediaStore.MediaColumns.MIME_TYPE, MIME);
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

        Uri uri = resolver.insert(collection, values);
        if (uri == null) {
            throw new IllegalStateException("Could not create the Excel file in Downloads.");
        }

        try {
            try (OutputStream output = resolver.openOutputStream(uri);
                 ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
                if (output == null) throw new IllegalStateException("Could not open Excel output.");
                writeWorkbook(zip, entries);
            }
        } catch (Exception ex) {
            resolver.delete(uri, null, null);
            throw ex;
        }

        return FILE_NAME;
    }

    private static void writeWorkbook(ZipOutputStream zip, List<Entry> entries) throws Exception {
        add(zip, "[Content_Types].xml", contentTypes());
        add(zip, "_rels/.rels", rootRelationships());
        add(zip, "xl/workbook.xml", workbookXml());
        add(zip, "xl/_rels/workbook.xml.rels", workbookRelationships());
        add(zip, "xl/styles.xml", stylesXml());
        add(zip, "xl/worksheets/sheet1.xml", dailySheet(entries));
        add(zip, "xl/worksheets/sheet2.xml", historySheet(entries));
        add(zip, "xl/worksheets/sheet3.xml", monthlySheet(entries));
    }

    private static void add(ZipOutputStream zip, String path, String content) throws Exception {
        zip.putNextEntry(new ZipEntry(path));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static String contentTypes() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">" +
                "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>" +
                "<Default Extension=\"xml\" ContentType=\"application/xml\"/>" +
                "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>" +
                "<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>" +
                "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>" +
                "<Override PartName=\"/xl/worksheets/sheet2.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>" +
                "<Override PartName=\"/xl/worksheets/sheet3.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>" +
                "</Types>";
    }

    private static String rootRelationships() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>" +
                "</Relationships>";
    }

    private static String workbookXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" " +
                "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">" +
                "<sheets>" +
                "<sheet name=\"आजचा हिशोब\" sheetId=\"1\" r:id=\"rId1\"/>" +
                "<sheet name=\"मागील सर्व नोंदी\" sheetId=\"2\" r:id=\"rId2\"/>" +
                "<sheet name=\"महिनावार अहवाल\" sheetId=\"3\" r:id=\"rId3\"/>" +
                "</sheets><calcPr fullCalcOnLoad=\"1\" forceFullCalc=\"1\"/></workbook>";
    }

    private static String workbookRelationships() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>" +
                "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet2.xml\"/>" +
                "<Relationship Id=\"rId3\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet3.xml\"/>" +
                "<Relationship Id=\"rId4\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>" +
                "</Relationships>";
    }

    private static String stylesXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">" +
                "<fonts count=\"2\"><font><sz val=\"11\"/><name val=\"Aptos\"/></font><font><b/><sz val=\"11\"/><name val=\"Aptos\"/></font></fonts>" +
                "<fills count=\"2\"><fill><patternFill patternType=\"none\"/></fill><fill><patternFill patternType=\"gray125\"/></fill></fills>" +
                "<borders count=\"1\"><border><left/><right/><top/><bottom/><diagonal/></border></borders>" +
                "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>" +
                "<cellXfs count=\"2\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/><xf numFmtId=\"0\" fontId=\"1\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/></cellXfs>" +
                "<cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles>" +
                "</styleSheet>";
    }

    private static String dailySheet(List<Entry> entries) {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().getTime());
        List<Entry> todayEntries = new ArrayList<>();
        for (Entry e : entries) if (today.equals(e.date)) todayEntries.add(e);

        StringBuilder rows = new StringBuilder();
        int row = 1;
        rows.append(row(row, cell("A", "टोमॅटो दैनिक १०० नोंदी हिशोब — Daily Tomato Sales Ledger", 1))); row++;
        double carats = 0, sales = 0;
        for (Entry e : todayEntries) { carats += e.carats; sales += e.amount(); }
        rows.append(row(row, cells(
                cell("A", "आजची तारीख (Date)", 1), cell("B", today, 1),
                cell("C", "एकूण कॅरेट", 1), num("D", carats, 1),
                cell("E", "एकूण विक्री ₹", 1), num("F", sales, 1)))); row += 2;

        rows.append(row(row, cells(
                cell("A", "अ.क्र.", 1), cell("B", "ग्राहकाचे नाव", 1),
                cell("C", "कॅरेट संख्या", 1), cell("D", "दर / Cost ₹", 1),
                cell("E", "एकूण रक्कम ₹", 1)))); row++;
        int i = 1;
        for (Entry e : todayEntries) {
            rows.append(row(row, cells(
                    num("A", i, 0), cell("B", e.customer, 0),
                    num("C", e.carats, 0), num("D", e.rate, 0), num("E", e.amount(), 0)
            )));
            row++; i++;
        }
        return sheetXml(rows.toString(), "A1:E" + Math.max(row, 1),
                "<col min=\"1\" max=\"1\" width=\"8\"/><col min=\"2\" max=\"2\" width=\"30\"/><col min=\"3\" max=\"5\" width=\"17\"/>");
    }

    private static String historySheet(List<Entry> entries) {
        StringBuilder rows = new StringBuilder();
        int row = 1;
        rows.append(row(row, cells(
                cell("A", "दिनांक", 1), cell("B", "अ.क्र.", 1),
                cell("C", "ग्राहकाचे नाव", 1), cell("D", "कॅरेट संख्या", 1),
                cell("E", "दर / Cost ₹", 1), cell("F", "एकूण रक्कम ₹", 1)))); row++;
        Map<String, Integer> serialByDate = new LinkedHashMap<>();
        String last = "";
        int i = 0;
        for (Entry e : entries) {
            if (!e.date.equals(last)) { serialByDate.put(e.date, 1); last = e.date; }
            int serial = serialByDate.get(e.date);
            serialByDate.put(e.date, serial + 1);
            rows.append(row(row, cells(
                    cell("A", e.date, 0), num("B", serial, 0),
                    cell("C", e.customer, 0), num("D", e.carats, 0),
                    num("E", e.rate, 0), num("F", e.amount(), 0)
            ))); row++; i++;
        }
        return sheetXml(rows.toString(), "A1:F" + Math.max(row, 1),
                "<col min=\"1\" max=\"1\" width=\"15\"/><col min=\"2\" max=\"2\" width=\"8\"/><col min=\"3\" max=\"3\" width=\"30\"/><col min=\"4\" max=\"6\" width=\"17\"/>");
    }

    private static String monthlySheet(List<Entry> entries) {
        TreeMap<String, double[]> totals = new TreeMap<>(Collections.reverseOrder());
        for (Entry e : entries) {
            String month = e.date.length() >= 7 ? e.date.substring(0, 7) : e.date;
            double[] pair = totals.computeIfAbsent(month, k -> new double[2]);
            pair[0] += e.carats; pair[1] += e.amount();
        }

        StringBuilder rows = new StringBuilder();
        int row = 1;
        rows.append(row(row, cells(
                cell("A", "महिना (Month)", 1),
                cell("B", "एकूण कॅरेट", 1),
                cell("C", "एकूण विक्री ₹", 1)))); row++;
        for (Map.Entry<String, double[]> m : totals.entrySet()) {
            rows.append(row(row, cells(
                    cell("A", m.getKey(), 0),
                    num("B", m.getValue()[0], 0),
                    num("C", m.getValue()[1], 0)))); row++;
        }
        return sheetXml(rows.toString(), "A1:C" + Math.max(row, 1),
                "<col min=\"1\" max=\"1\" width=\"18\"/><col min=\"2\" max=\"3\" width=\"22\"/>");
    }

    private static String sheetXml(String rows, String dimension, String cols) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">" +
                "<sheetViews><sheetView workbookViewId=\"0\" showGridLines=\"0\"/></sheetViews>" +
                "<dimension ref=\"" + dimension + "\"/>" +
                "<cols>" + cols + "</cols><sheetData>" + rows + "</sheetData></worksheet>";
    }

    private static String row(int row, String cells) {
        return "<row r=\"" + row + "\">" + cells + "</row>";
    }

    private static String cells(String... cells) {
        StringBuilder b = new StringBuilder();
        for (String c : cells) if (c != null) b.append(c);
        return b.toString();
    }

    private static String cell(String col, String value, int style) {
        return "<c r=\"" + col + "\" s=\"" + style + "\" t=\"inlineStr\"><is><t xml:space=\"preserve\">" +
                escape(value == null ? "" : value) + "</t></is></c>";
    }

    private static String num(String col, double value, int style) {
        return "<c r=\"" + col + "\" s=\"" + style + "\"><v>" +
                Double.toString(value) + "</v></c>";
    }

    private static String num(String col, int value, int style) {
        return "<c r=\"" + col + "\" s=\"" + style + "\"><v>" +
                Integer.toString(value) + "</v></c>";
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }
}

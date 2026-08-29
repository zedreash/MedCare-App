package com.medcare.app.utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.FileProvider;

import com.google.android.material.color.MaterialColors;
import com.medcare.app.R;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public final class PdfExporter {
    private PdfExporter() {}

    private static final int PAGE_W = 595;
    private static final int PAGE_H = 842;
    private static final int MARGIN = 40;
    private static final int CONTENT_W = PAGE_W - 2 * MARGIN;
    private static final float KEY_COL_W = 140f;
    private static final int FALLBACK_PRIMARY = 0xFF1565C0;
    private static final int FALLBACK_ON_PRIMARY = 0xFFFFFFFF;
    private static final int COLOR_TEXT = 0xFF1F2937;
    private static final int COLOR_MUTED = 0xFF6B7280;
    private static final int COLOR_WHITE = 0xFFFFFFFF;
    private static final int COLOR_ROW_ALT = 0xFFF8FAFC;
    private static final int COLOR_DIVIDER = 0xFFE5E7EB;

    public static class Table {
        public final String title;
        public final String[] header;
        public final String[][] rows;

        public Table(String title, String[] header, String[][] rows) {
            this.title = title;
            this.header = header;
            this.rows = rows;
        }
    }

    public static File writeReportPdf(Context context, String fileName, String title,
                                      String subtitle, String[] header, String[][] rows) throws Exception {
        PdfWriter w = new PdfWriter(context, fileName);
        w.drawHeader(title, subtitle);
        w.drawTable(header, rows);
        return w.finish();
    }

    public static File writePatientSummaryPdf(Context context, String fileName, String title,
                                              String subtitle, String detailsTitle, String[][] infoPairs,
                                              List<Table> tables) throws Exception {
        PdfWriter w = new PdfWriter(context, fileName);
        w.drawHeader(title, subtitle);
        if (infoPairs != null && infoPairs.length > 0) {
            w.drawSection(detailsTitle);
            for (String[] kv : infoPairs) {
                w.drawKeyValue(kv[0], kv[1]);
            }
        }
        if (tables != null) {
            for (Table t : tables) {
                w.drawSection(t.title);
                w.drawTable(t.header, t.rows);
            }
        }
        return w.finish();
    }

    public static void share(Context context, File file) {
        Uri uri = FileProvider.getUriForFile(context,
                context.getPackageName() + ".fileprovider", file);
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("application/pdf");
        send.putExtra(Intent.EXTRA_STREAM, uri);
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(Intent.createChooser(send, context.getString(R.string.share)));
    }

    private static class PdfWriter {
        private final Context context;
        private final String fileName;
        private final PdfDocument doc;
        private final int primary;
        private final int onPrimary;
        private PdfDocument.Page page;
        private Canvas canvas;
        private float y;

        PdfWriter(Context context, String fileName) {
            this.context = context;
            this.fileName = fileName;
            this.doc = new PdfDocument();
            this.primary = themeColor(context, com.google.android.material.R.attr.colorPrimary, FALLBACK_PRIMARY);
            this.onPrimary = themeColor(context, com.google.android.material.R.attr.colorOnPrimary, FALLBACK_ON_PRIMARY);
            startPage();
        }

        private static int themeColor(Context c, int attr, int fallback) {
            try {
                return MaterialColors.getColor(c, attr, fallback);
            } catch (Exception e) {
                return fallback;
            }
        }

        private void startPage() {
            page = doc.startPage(new PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, 1).create());
            canvas = page.getCanvas();
            y = MARGIN;
        }

        private void ensureSpace(float h) {
            if (y + h > PAGE_H - MARGIN) {
                doc.finishPage(page);
                startPage();
            }
        }

        private Path roundedRect(float left, float top, float right, float bottom,
                                 float tl, float tr, float br, float bl) {
            Path p = new Path();
            float[] radii = {tl, tl, tr, tr, br, br, bl, bl};
            p.addRoundRect(new RectF(left, top, right, bottom), radii, Path.Direction.CW);
            return p;
        }

        private void drawHeader(String title, String subtitle) {
            float bannerH = 66f;
            Path banner = roundedRect(MARGIN, y, MARGIN + CONTENT_W, y + bannerH,
                    14f, 14f, 14f, 14f);
            Paint bannerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            bannerPaint.setColor((primary & 0x00FFFFFF) | 0x1A000000);
            canvas.drawPath(banner, bannerPaint);

            try {
                Drawable d = AppCompatResources.getDrawable(context, R.drawable.ic_app_logo);
                int px = 96;
                Bitmap bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888);
                Canvas c = new Canvas(bmp);
                d.setBounds(0, 0, px, px);
                d.draw(c);
                int sizePt = 36;
                Rect dst = new Rect(MARGIN + 14, (int) (y + (bannerH - sizePt) / 2f),
                        MARGIN + 14 + sizePt, (int) (y + (bannerH - sizePt) / 2f) + sizePt);
                canvas.drawBitmap(bmp, null, dst, null);
            } catch (Exception ignored) {}

            float x = MARGIN + 60;
            Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            titlePaint.setTextSize(19f);
            titlePaint.setFakeBoldText(true);
            titlePaint.setColor(COLOR_TEXT);
            canvas.drawText(title == null ? "" : title, x, y + 27, titlePaint);

            if (subtitle != null && !subtitle.isEmpty()) {
                Paint subPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                subPaint.setTextSize(9.5f);
                subPaint.setColor(COLOR_MUTED);
                canvas.drawText(subtitle, x, y + 44, subPaint);
            }

            y += bannerH + 20;
        }

        private void drawSection(String title) {
            y += 14;
            ensureSpace(32);
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setTextSize(13f);
            p.setFakeBoldText(true);
            p.setColor(primary);
            canvas.drawText(title == null ? "" : title, MARGIN, y, p);
            y += 10;
            Paint accent = new Paint();
            accent.setColor(primary);
            canvas.drawRect(MARGIN, y, MARGIN + 46, y + 1.5f, accent);
            y += 14;
        }

        private void drawKeyValue(String key, String value) {
            if (value == null || value.isEmpty()) return;
            ensureSpace(20);
            Paint keyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            keyPaint.setTextSize(9.5f);
            keyPaint.setFakeBoldText(true);
            keyPaint.setColor(COLOR_MUTED);
            String k = key == null ? "" : key;
            canvas.drawText(k, MARGIN, y, keyPaint);

            Paint valPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            valPaint.setTextSize(10.5f);
            valPaint.setColor(COLOR_TEXT);
            float keyW = keyPaint.measureText(k);
            float valX = Math.max(MARGIN + KEY_COL_W, MARGIN + keyW + 18);
            float maxW = CONTENT_W - (valX - MARGIN);
            if (maxW < 40) maxW = 40;
            for (String line : wrap(value, valPaint, maxW)) {
                ensureSpace(15);
                canvas.drawText(line, valX, y, valPaint);
                y += 15;
            }
            y += 3;
        }

        private void drawTable(String[] header, String[][] rows) {
            if (rows == null || rows.length == 0) return;
            int cols = header != null ? header.length : rows[0].length;
            float colW = CONTENT_W / (float) cols;
            if (header != null) {
                drawTableRow(header, colW, true, false, true, false);
            }
            int n = rows.length;
            for (int i = 0; i < n; i++) {
                drawTableRow(rows[i], colW, false, i % 2 == 0, false, i == n - 1);
            }
            y += 6;
        }

        private void drawTableRow(String[] cells, float colW, boolean headerRow,
                                  boolean altRow, boolean roundTop, boolean roundBottom) {
            Paint cellPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            cellPaint.setTextSize(9.5f);
            cellPaint.setFakeBoldText(headerRow);
            List<List<String>> wrapped = new ArrayList<>();
            int maxLines = 1;
            for (String c : cells) {
                List<String> lines = wrap(c == null ? "" : c, cellPaint, colW - 12);
                wrapped.add(lines);
                maxLines = Math.max(maxLines, lines.size());
            }
            float lineH = 11.5f;
            float padY = 4f;
            float rowH = padY * 2 + maxLines * lineH + 2;

            ensureSpace(rowH + 4);
            Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
            bg.setColor(headerRow ? primary : (altRow ? COLOR_ROW_ALT : COLOR_WHITE));
            Path rowPath = roundedRect(MARGIN, y, MARGIN + CONTENT_W, y + rowH,
                    roundTop ? 8f : 0f, roundTop ? 8f : 0f,
                    roundBottom ? 8f : 0f, roundBottom ? 8f : 0f);
            canvas.drawPath(rowPath, bg);

            cellPaint.setColor(headerRow ? onPrimary : COLOR_TEXT);
            float textY = y + padY + lineH;
            for (int ci = 0; ci < cells.length; ci++) {
                float cx = MARGIN + ci * colW + 7;
                float ty = textY;
                for (String line : wrapped.get(ci)) {
                    canvas.drawText(line, cx, ty, cellPaint);
                    ty += lineH;
                }
            }

            if (!headerRow && !roundBottom) {
                Paint hair = new Paint();
                hair.setColor(COLOR_DIVIDER);
                canvas.drawLine(MARGIN + 1, y + rowH, MARGIN + CONTENT_W - 1, y + rowH, hair);
            }
            y += rowH;
        }

        private List<String> wrap(String text, Paint paint, float maxWidth) {
            List<String> out = new ArrayList<>();
            if (text == null) {
                out.add("");
                return out;
            }
            String[] words = text.split("\\s+");
            StringBuilder line = new StringBuilder();
            for (String word : words) {
                String trial = line.length() == 0 ? word : line.toString() + " " + word;
                if (paint.measureText(trial) > maxWidth && line.length() > 0) {
                    out.add(line.toString());
                    line.setLength(0);
                    line.append(word);
                } else {
                    line.setLength(0);
                    line.append(trial);
                }
            }
            if (line.length() > 0) out.add(line.toString());
            return out;
        }

        private File finish() throws Exception {
            doc.finishPage(page);
            File dir = new File(context.getFilesDir(), "pdf");
            if (!dir.exists()) dir.mkdirs();
            File file = new File(dir, fileName);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                doc.writeTo(fos);
            }
            doc.close();
            return file;
        }
    }
}
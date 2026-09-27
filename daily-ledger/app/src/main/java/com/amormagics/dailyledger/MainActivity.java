package com.amormagics.dailyledger;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends android.app.Activity {
    private static final int MAX_DAILY_ENTRIES = 100;
    private static final int WHITE = Color.WHITE;
    private static final int TEXT = Color.rgb(236, 236, 236);
    private static final int MUTED = Color.rgb(150, 150, 150);
    private static final int LINE = Color.rgb(45, 45, 45);
    private static final int PANEL = Color.rgb(15, 15, 15);
    private static final int FIELD = Color.rgb(24, 24, 24);

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService exporter = Executors.newSingleThreadExecutor();
    private final List<Entry> entries = new ArrayList<>();
    private SharedPreferences prefs;

    private LinearLayout content;
    private TextView totalCarats;
    private TextView totalSales;
    private TextView status;
    private String currentDate;

    private final Runnable delayedExport = new Runnable() {
        @Override public void run() {
            exportExcelAsync();
        }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        Window w = getWindow();
        w.setStatusBarColor(Color.BLACK);
        w.setNavigationBarColor(Color.BLACK);
        if (Build.VERSION.SDK_INT >= 30) {
            w.setDecorFitsSystemWindows(true);
            w.setStatusBarColor(Color.BLACK);
            w.setNavigationBarColor(Color.BLACK);
        }
        prefs = getSharedPreferences("daily_ledger", Context.MODE_PRIVATE);
        currentDate = todayKey();
        load();
        buildApp();
        showToday();
    }

    @Override protected void onResume() {
        super.onResume();
        String now = todayKey();
        if (currentDate != null && !now.equals(currentDate)) {
            currentDate = now;
            showToday();
        }
    }

    @Override protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        exporter.shutdownNow();
        super.onDestroy();
    }

    private void buildApp() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        setContentView(root);

        LinearLayout app = new LinearLayout(this);
        app.setOrientation(LinearLayout.VERTICAL);
        app.setPadding(dp(16), dp(12), dp(16), dp(10));
        root.addView(app, new FrameLayout.LayoutParams(-1, -1));

        if (Build.VERSION.SDK_INT >= 30) {
            root.setOnApplyWindowInsetsListener((v, insets) -> {
                WindowInsetsController c = getWindow().getInsetsController();
                if (c != null) c.setSystemBarsAppearance(0, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
                int top = insets.getInsets(WindowInsets.Type.statusBars()).top;
                int bottom = insets.getInsets(WindowInsets.Type.navigationBars()).bottom;
                app.setPadding(dp(16), dp(12) + top, dp(16), dp(10) + bottom);
                return insets;
            });
        }

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text("दैनिक हिशोब", 25, WHITE, true);
        TextView subtitle = text("Daily Ledger  •  Offline", 12, MUTED, false);
        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);
        titleBox.addView(title);
        titleBox.addView(subtitle, lp(-2, -2, 0, dp(4), 0, 0));
        header.addView(titleBox, new LinearLayout.LayoutParams(0, -2, 1));

        Button export = button("EXCEL", false);
        export.setOnClickListener(v -> exportExcelAsync());
        header.addView(export, lp(-2, dp(42), 0, 0, 0, 0));
        app.addView(header, lp(-1, -2, 0, 0, 0, dp(10)));

        LinearLayout nav = new LinearLayout(this);
        nav.setPadding(dp(4), dp(4), dp(4), dp(4));
        nav.setBackground(bg(PANEL, LINE, 14));
        Button today = navButton("आज");
        Button history = navButton("इतिहास");
        Button report = navButton("रिपोर्ट");
        nav.addView(today, new LinearLayout.LayoutParams(0, dp(42), 1));
        nav.addView(history, new LinearLayout.LayoutParams(0, dp(42), 1));
        nav.addView(report, new LinearLayout.LayoutParams(0, dp(42), 1));
        today.setOnClickListener(v -> showToday());
        history.setOnClickListener(v -> showHistory());
        report.setOnClickListener(v -> showReport());
        app.addView(nav, lp(-1, -2, 0, 0, 0, dp(10)));

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        app.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));
    }

    private void showToday() {
        content.removeAllViews();
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout body = vertical();

        LinearLayout dateCard = card();
        TextView date = text("आजची तारीख  " + currentDate, 14, TEXT, true);
        status = text("ऑटो सेव्ह: तयार", 12, MUTED, false);
        dateCard.addView(date);
        dateCard.addView(status, lp(-1, -2, 0, dp(4), 0, 0));
        body.addView(dateCard, lp(-1, -2, 0, 0, 0, dp(10)));

        LinearLayout totals = new LinearLayout(this);
        LinearLayout c = metric("कॅरेट", "0.00");
        LinearLayout s = metric("एकूण ₹", "0.00");
        totals.addView(c, new LinearLayout.LayoutParams(0, -2, 1));
        totals.addView(s, lp(0, -2, 1, dp(8), 0, 0));
        body.addView(totals, lp(-1, -2, 0, 0, 0, dp(10)));
        totalCarats = (TextView) c.getTag();
        totalSales = (TextView) s.getTag();

        TextView hint = text("ग्राहकाची नोंद करा  •  बदल होताच डेटा सुरक्षितपणे सेव्ह होतो", 12, MUTED, false);
        body.addView(hint, lp(-1, -2, 0, dp(2), 0, dp(8)));

        LinearLayout list = vertical();
        list.setTag("today-list");
        body.addView(list, lp(-1, -2, 0, 0, 0, dp(8)));

        Button add = button("+  नवीन नोंद", true);
        add.setOnClickListener(v -> {
            int count = countForDate(currentDate);
            if (count >= MAX_DAILY_ENTRIES) {
                toast("आजच्या 100 नोंदी पूर्ण झाल्या आहेत.");
                return;
            }
            entries.add(new Entry(System.currentTimeMillis(), currentDate, "", 0, 0));
            saveAndSchedule();
            renderTodayRows(list);
            scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
        });
        body.addView(add, lp(-1, dp(48), 0, 0, 0, dp(8)));

        TextView footer = text("Excel फाइल: Downloads/Daily_Ledger.xlsx", 11, Color.rgb(105,105,105), false);
        footer.setGravity(Gravity.CENTER);
        body.addView(footer, lp(-1, -2, 0, 0, 0, dp(14)));

        scroll.addView(body);
        content.addView(scroll, new LinearLayout.LayoutParams(-1, -1));
        renderTodayRows(list);
        refreshTotals();
    }

    private void renderTodayRows(LinearLayout list) {
        list.removeAllViews();
        List<Entry> today = entriesFor(currentDate);
        int serial = 1;
        if (today.isEmpty()) {
            TextView empty = text("आजची नोंद अजून नाही.\n+ नवीन नोंद दाबा.", 15, MUTED, false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(12), dp(28), dp(12), dp(28));
            list.addView(empty);
            return;
        }
        for (Entry e : today) addEntryCard(list, e, serial++);
    }

    private void addEntryCard(LinearLayout list, Entry e, int serial) {
        LinearLayout card = card();
        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView no = text(String.format(Locale.US, "%02d", serial), 13, MUTED, true);
        no.setGravity(Gravity.CENTER);
        no.setBackground(bg(Color.rgb(28,28,28), LINE, 10));
        top.addView(no, lp(dp(42), dp(40), 0, 0, 0, 0));

        EditText customer = field("ग्राहकाचे नाव", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        customer.setText(e.customer);
        top.addView(customer, lp(0, dp(46), dp(10), 0, 0, 0, 1));

        Button del = button("×", false);
        del.setTextSize(22);
        del.setTextColor(Color.rgb(180,180,180));
        del.setOnClickListener(v -> {
            entries.remove(e);
            saveAndSchedule();
            renderTodayRows(list);
            refreshTotals();
        });
        top.addView(del, lp(dp(46), dp(46), 0, 0, 0, 0));
        card.addView(top);

        LinearLayout numbers = new LinearLayout(this);
        numbers.setGravity(Gravity.CENTER_VERTICAL);

        EditText carats = field("कॅरेट", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        carats.setText(formatInput(e.carats));
        numbers.addView(carats, new LinearLayout.LayoutParams(0, dp(46), 1));

        EditText rate = field("दर ₹", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        rate.setText(formatInput(e.rate));
        numbers.addView(rate, lp(0, dp(46), 1, dp(8), 0, 0));

        TextView amount = text(money(e.amount()), 16, WHITE, true);
        amount.setGravity(Gravity.CENTER);
        amount.setBackground(bg(Color.rgb(10,10,10), LINE, 10));
        numbers.addView(amount, lp(0, dp(46), 1, dp(8), 0, 0));
        card.addView(numbers, lp(-1, -2, 0, dp(8), 0, 0));

        bind(customer, v -> { e.customer = v; saveAndSchedule(); });
        bind(carats, v -> { e.carats = number(v); amount.setText(money(e.amount())); saveAndSchedule(); refreshTotals(); });
        bind(rate, v -> { e.rate = number(v); amount.setText(money(e.amount())); saveAndSchedule(); refreshTotals(); });

        list.addView(card, lp(-1, -2, 0, 0, 0, dp(8)));
    }

    private void showHistory() {
        content.removeAllViews();
        ScrollView scroll = new ScrollView(this);
        LinearLayout body = vertical();
        TextView title = text("मागील सर्व नोंदी", 20, WHITE, true);
        body.addView(title, lp(-1, -2, 0, 0, 0, dp(4)));
        TextView sub = text(entries.size() + " नोंदी  •  फोनवरच सेव्ह", 12, MUTED, false);
        body.addView(sub, lp(-1, -2, 0, 0, 0, dp(10)));

        List<Entry> copy = new ArrayList<>(entries);
        copy.sort(Comparator.comparing((Entry e) -> e.date).reversed().thenComparingLong(e -> e.id).reversed());

        String lastDate = "";
        for (Entry e : copy) {
            if (!e.date.equals(lastDate)) {
                lastDate = e.date;
                LinearLayout head = card();
                TextView d = text(e.date, 14, TEXT, true);
                head.addView(d);
                body.addView(head, lp(-1, -2, 0, 0, 0, dp(8)));
            }
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            TextView name = text(e.customer.isEmpty() ? "नाव नाही" : e.customer, 14, TEXT, false);
            row.addView(name, new LinearLayout.LayoutParams(0, -2, 1));
            TextView details = text(trimNumber(e.carats) + " × " + trimNumber(e.rate) + " = " + money(e.amount()), 13, MUTED, false);
            row.addView(details);
            body.addView(row, lp(-1, -2, 0, dp(10), 0, 0));
        }

        if (copy.isEmpty()) {
            TextView empty = text("इतिहास रिकामा आहे.", 15, MUTED, false);
            empty.setGravity(Gravity.CENTER);
            body.addView(empty, lp(-1, -2, 0, dp(30), 0, 0));
        }

        scroll.addView(body);
        content.addView(scroll, new LinearLayout.LayoutParams(-1, -1));
    }

    private void showReport() {
        content.removeAllViews();
        ScrollView scroll = new ScrollView(this);
        LinearLayout body = vertical();
        body.addView(text("महिनावार अहवाल", 20, WHITE, true), lp(-1, -2, 0, 0, 0, dp(4)));

        TreeMap<String, double[]> totals = new TreeMap<>(Collections.reverseOrder());
        for (Entry e : entries) {
            String month = e.date.length() >= 7 ? e.date.substring(0, 7) : e.date;
            double[] t = totals.computeIfAbsent(month, k -> new double[2]);
            t[0] += e.carats;
            t[1] += e.amount();
        }

        for (Map.Entry<String, double[]> m : totals.entrySet()) {
            LinearLayout card = card();
            TextView month = text(m.getKey(), 16, WHITE, true);
            card.addView(month);
            LinearLayout metrics = new LinearLayout(this);
            TextView c = text("कॅरेट  " + money(m.getValue()[0]), 14, MUTED, false);
            TextView s = text("विक्री ₹  " + money(m.getValue()[1]), 14, TEXT, true);
            metrics.addView(c, new LinearLayout.LayoutParams(0, -2, 1));
            metrics.addView(s, new LinearLayout.LayoutParams(0, -2, 1));
            card.addView(metrics, lp(-1, -2, 0, dp(8), 0, 0));
            body.addView(card, lp(-1, -2, 0, 0, 0, dp(8)));
        }

        if (totals.isEmpty()) {
            TextView empty = text("अहवाल तयार करण्यासाठी नोंदी जोडा.", 15, MUTED, false);
            empty.setGravity(Gravity.CENTER);
            body.addView(empty, lp(-1, -2, 0, dp(30), 0, 0));
        }

        scroll.addView(body);
        content.addView(scroll, new LinearLayout.LayoutParams(-1, -1));
    }

    private void bind(EditText edit, java.util.function.Consumer<String> change) {
        edit.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int before, int count) {
                change.accept(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void saveAndSchedule() {
        save();
        statusText("ऑटो सेव्ह: सुरक्षित");
        handler.removeCallbacks(delayedExport);
        handler.postDelayed(delayedExport, 800);
    }

    private void exportExcelAsync() {
        List<Entry> snapshot = new ArrayList<>(entries);
        statusText("Excel: सेव्ह होत आहे…");
        exporter.execute(() -> {
            try {
                String name = ExcelExporter.export(this, snapshot);
                runOnUiThread(() -> statusText("Excel: " + name + " • अपडेटेड"));
            } catch (Exception ex) {
                runOnUiThread(() -> statusText("Excel: पुन्हा प्रयत्न करा"));
            }
        });
    }

    private void statusText(String s) {
        if (status != null) status.setText(s);
    }

    private void save() {
        JSONArray a = new JSONArray();
        for (Entry e : entries) {
            try {
                JSONObject o = new JSONObject();
                o.put("id", e.id);
                o.put("date", e.date);
                o.put("customer", e.customer);
                o.put("carats", e.carats);
                o.put("rate", e.rate);
                a.put(o);
            } catch (Exception ignored) {}
        }
        prefs.edit().putString("entries", a.toString()).apply();
    }

    private void load() {
        entries.clear();
        try {
            JSONArray a = new JSONArray(prefs.getString("entries", "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                entries.add(new Entry(
                        o.optLong("id", System.currentTimeMillis() + i),
                        o.optString("date", todayKey()),
                        o.optString("customer", ""),
                        o.optDouble("carats", 0),
                        o.optDouble("rate", 0)
                ));
            }
        } catch (Exception ignored) {}
    }

    private void refreshTotals() {
        if (totalCarats == null || totalSales == null) return;
        double c = 0, s = 0;
        for (Entry e : entries) if (currentDate.equals(e.date)) { c += e.carats; s += e.amount(); }
        totalCarats.setText(money(c));
        totalSales.setText(money(s));
    }

    private int countForDate(String date) {
        int n = 0;
        for (Entry e : entries) if (date.equals(e.date)) n++;
        return n;
    }

    private List<Entry> entriesFor(String date) {
        List<Entry> out = new ArrayList<>();
        for (Entry e : entries) if (date.equals(e.date)) out.add(e);
        out.sort(Comparator.comparingLong(e -> e.id));
        return out;
    }

    private static double number(String s) {
        try { return Double.parseDouble(s.trim()); } catch (Exception e) { return 0; }
    }

    private static String todayKey() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().getTime());
    }

    private static String money(double n) {
        return String.format(Locale.US, "%.2f", n);
    }

    private static String trimNumber(double n) {
        if (Math.rint(n) == n) return String.format(Locale.US, "%.0f", n);
        return String.format(Locale.US, "%.2f", n);
    }

    private static String formatInput(double n) {
        if (n == 0) return "";
        return trimNumber(n);
    }

    private LinearLayout vertical() {
        LinearLayout x = new LinearLayout(this);
        x.setOrientation(LinearLayout.VERTICAL);
        return x;
    }

    private LinearLayout card() {
        LinearLayout x = vertical();
        x.setPadding(dp(12), dp(12), dp(12), dp(12));
        x.setBackground(bg(PANEL, LINE, 14));
        return x;
    }

    private LinearLayout metric(String label, String value) {
        LinearLayout x = card();
        TextView l = text(label, 12, MUTED, false);
        TextView v = text(value, 22, WHITE, true);
        x.addView(l);
        x.addView(v, lp(-1, -2, 0, dp(4), 0, 0));
        x.setTag(v);
        return x;
    }

    private Button navButton(String t) {
        Button b = button(t, false);
        b.setTextSize(14);
        b.setPadding(0,0,0,0);
        return b;
    }

    private Button button(String t, boolean filled) {
        Button b = new Button(this);
        b.setText(t);
        b.setAllCaps(false);
        b.setTextSize(14);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setTextColor(filled ? Color.BLACK : TEXT);
        b.setGravity(Gravity.CENTER);
        b.setBackground(bg(filled ? WHITE : Color.TRANSPARENT, filled ? WHITE : LINE, 12));
        return b;
    }

    private EditText field(String hint, int inputType) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setHintTextColor(Color.rgb(95,95,95));
        e.setTextColor(TEXT);
        e.setSingleLine(true);
        e.setTextSize(14);
        e.setInputType(inputType);
        e.setPadding(dp(12), 0, dp(10), 0);
        e.setBackground(bg(FIELD, LINE, 12));
        return e;
    }

    private TextView text(String t, float size, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(t);
        v.setTextSize(size);
        v.setTextColor(color);
        v.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
        return v;
    }

    private GradientDrawable bg(int fill, int stroke, int radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(radius));
        if (stroke != Color.TRANSPARENT) g.setStroke(dp(1), stroke);
        return g;
    }

    private LinearLayout.LayoutParams lp(int w, int h, int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.setMargins(left, top, right, bottom);
        return p;
    }

    private int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }
}

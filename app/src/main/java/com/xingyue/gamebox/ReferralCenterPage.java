package com.xingyue.gamebox;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ReferralCenterPage {
    private static final String BASE = "https://4.wc3.tech";
    private static final int PURPLE = 0xFF32165E;
    private static final int BG = 0xFF100824;
    private static final int WHITE = Color.WHITE;
    private static final int MUTED = 0xFFD0B9F0;

    private static ReferralCenterPage activePage;
    private final Activity activity;
    private final UserAuth auth;
    private final Runnable onBack;
    private LinearLayout content;
    private TextView summaryView;
    private TextView qrView;
    private String qrImageKey = "";

    private ReferralCenterPage(Activity activity, UserAuth auth, Runnable onBack) {
        this.activity = activity;
        this.auth = auth;
        this.onBack = onBack;
    }

    public static void open(Activity activity, UserAuth auth, Runnable onBack) {
        activePage = new ReferralCenterPage(activity, auth, onBack);
        activePage.show();
    }

    public static void onQrPicked(Activity activity, Uri uri) {
        if (activePage != null && activePage.activity == activity && uri != null) {
            activePage.uploadQr(uri);
        }
    }

    private int dp(float value) {
        return (int) (value * activity.getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView text(String value, int size, int color) {
        TextView t = new TextView(activity);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        return t;
    }

    private void show() {
        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);

        content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(18), dp(18), dp(28));
        scroll.addView(content);

        LinearLayout header = new LinearLayout(activity);
        header.setGravity(Gravity.CENTER_VERTICAL);
        content.addView(header);

        TextView back = text("‹ 返回", 17, 0xFFE6C8FF);
        back.setPadding(0, dp(8), dp(18), dp(8));
        header.addView(back);
        back.setOnClickListener(v -> {
            activePage = null;
            onBack.run();
        });

        TextView title = text("推广中心", 24, WHITE);
        title.setTypeface(null, Typeface.BOLD);
        header.addView(title);

        TextView intro = text("邀请好友加入星月游戏盒，获得推广收益。", 14, MUTED);
        LinearLayout.LayoutParams introP = new LinearLayout.LayoutParams(-1, -2);
        introP.topMargin = dp(12);
        content.addView(intro, introP);

        summaryView = text("正在加载推广数据…", 15, WHITE);
        summaryView.setPadding(dp(16), dp(16), dp(16), dp(16));
        summaryView.setBackgroundColor(PURPLE);
        LinearLayout.LayoutParams summaryP = new LinearLayout.LayoutParams(-1, -2);
        summaryP.topMargin = dp(18);
        content.addView(summaryView, summaryP);

        Button share = button("分享我的邀请码");
        share.setOnClickListener(v -> shareInvite());
        
        Button bindInvite = button("输入邀请码");
bindInvite.setOnClickListener(v -> showBindInviteDialog());

        TextView qrTitle = text("微信提现收款码", 18, WHITE);
        qrTitle.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams qrTitleP = new LinearLayout.LayoutParams(-1, -2);
        qrTitleP.topMargin = dp(22);
        content.addView(qrTitle, qrTitleP);

        qrView = text("尚未上传收款码", 14, MUTED);
        LinearLayout.LayoutParams qrViewP = new LinearLayout.LayoutParams(-1, -2);
        qrViewP.topMargin = dp(8);
        content.addView(qrView, qrViewP);

        Button chooseQr = button("选择并上传微信收款码");
        chooseQr.setOnClickListener(v -> chooseQr());

        Button withdraw = button("申请提现");
        withdraw.setOnClickListener(v -> showWithdrawDialog());

        TextView historyTitle = text("提现记录", 18, WHITE);
        historyTitle.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams historyP = new LinearLayout.LayoutParams(-1, -2);
        historyP.topMargin = dp(22);
        content.addView(historyTitle, historyP);

        loadData();
        ((MainActivity) activity).showPageWithBottomNav(scroll, 2);
    }

    private Button button(String label) {
        Button b = new Button(activity);
        b.setText(label);
        b.setTextColor(WHITE);
        b.setAllCaps(false);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF7135B8));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(50));
        p.topMargin = dp(12);
        content.addView(b, p);
        return b;
    }

    private void loadData() {
        new Thread(() -> {
            try {
                JSONObject summary = request("GET", "/referral/v1/summary", null, null);
                JSONArray withdrawals = requestArray("/referral/v1/withdrawals");
                activity.runOnUiThread(() -> {
                    String invite = summary.optString("invite_code", "—");
                    int invited = summary.optInt("invited_count", 0);
                    long available = summary.optLong("available_cents", 0);
                    long debt = summary.optLong("debt_cents", 0);
                    long earned = summary.optLong("total_earned_cents", 0);
                    long withdrawn = summary.optLong("total_withdrawn_cents", 0);
                    int rate = summary.optInt("commission_rate", 50);
                    summaryView.setText(
                        "我的邀请码：" + invite +
                        "\n已邀请人数：" + invited +
                        "\n可提现余额：" + money(available) +
                        "\n累计推广收益：" + money(earned) +
                        "\n累计提现：" + money(withdrawn) +
                        "\n分成比例：" + rate + "%" +
                        (debt > 0 ? "\n待抵扣金额：" + money(debt) : "")
                    );
                    showWithdrawals(withdrawals);
                });
            } catch (Exception e) {
                activity.runOnUiThread(() -> summaryView.setText("推广数据加载失败：" + e.getMessage()));
            }
        }).start();
    }

    private void showWithdrawals(JSONArray rows) {
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.optJSONObject(i);
            if (row == null) continue;
            String status = row.optString("status", "未知");
            String name = row.optString("real_name", "");
            long amount = row.optLong("amount_cents", 0);
            String created = row.optString("created_at", "");
            TextView item = text(
                money(amount) + " · " + statusText(status) +
                (name.isEmpty() ? "" : "\n收款人：" + name) +
                (created.isEmpty() ? "" : "\n申请时间：" + created),
                14, MUTED
            );
            item.setPadding(dp(14), dp(12), dp(14), dp(12));
            item.setBackgroundColor(PURPLE);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
            p.topMargin = dp(8);
            content.addView(item, p);
        }
        if (rows.length() == 0) {
            TextView empty = text("暂无提现记录", 14, MUTED);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
            p.topMargin = dp(8);
            content.addView(empty, p);
        }
    }

    private String statusText(String s) {
        if ("pending".equalsIgnoreCase(s)) return "待审核";
        if ("approved".equalsIgnoreCase(s)) return "已通过";
        if ("paid".equalsIgnoreCase(s)) return "已打款";
        if ("rejected".equalsIgnoreCase(s)) return "已拒绝";
        if ("cancelled".equalsIgnoreCase(s)) return "已取消";
        return s;
    }

    private String money(long cents) {
        return String.format(Locale.CHINA, "¥%.2f", cents / 100.0);
    }

    private void shareInvite() {
        String code = "";
        String value = summaryView.getText().toString();
        int start = value.indexOf("我的邀请码：");
        if (start >= 0) {
            int end = value.indexOf('\n', start);
            code = value.substring(start + 6, end < 0 ? value.length() : end).trim();
        }
        if (code.isEmpty() || "—".equals(code)) {
            toast("邀请码还未加载完成");
            return;
        }
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT,
            "我在使用星月游戏盒，邀请你一起体验！\n我的邀请码：" + code +
            "\n下载地址：" + BASE + "/?invite=" + Uri.encode(code) +
            "\n请复制此链接到浏览器打开，下载并安装 App，首次注册即可自动绑定邀请关系。");
        activity.startActivity(Intent.createChooser(send, "分享邀请码"));
    }

    private void showBindInviteDialog() {
    EditText input = new EditText(activity);
    input.setSingleLine(true);
    input.setHint("请输入对方的邀请码");
    input.setFilters(new android.text.InputFilter[]{
        new android.text.InputFilter.LengthFilter(16)
    });

    new AlertDialog.Builder(activity)
        .setTitle("绑定邀请人")
        .setMessage("请输入邀请人的邀请码。绑定成功后不可更换。")
        .setView(input)
        .setNegativeButton("取消", null)
        .setPositiveButton("确认绑定", (dialog, which) -> {
            String code = input.getText().toString().trim();
            if (code.isEmpty()) {
                toast("请输入邀请码");
                return;
            }
            submitInviteBinding(code);
        })
        .show();
}

private void submitInviteBinding(String code) {
    new Thread(() -> {
        try {
            JSONObject body = new JSONObject();
            body.put("invite_code", code);

            JSONObject result = request(
                "POST",
                "/referral/v1/bind",
                body,
                null
            );

            activity.runOnUiThread(() -> {
                toast(result.optString(
                    "message",
                    "邀请人绑定成功"
                ));
                show();
            });
        } catch (Exception e) {
            activity.runOnUiThread(() ->
                toast("绑定失败：" + e.getMessage())
            );
        }
    }).start();
}

    private void chooseQr() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        activity.startActivityForResult(intent, 7301);
    }

    private void uploadQr(Uri uri) {
        qrView.setText("正在上传收款码…");
        new Thread(() -> {
            try {
                String token = auth.getValidAccessToken();
                String boundary = "----XingYue" + System.currentTimeMillis();
                HttpURLConnection c = (HttpURLConnection) new URL(BASE + "/referral/v1/qr/upload").openConnection();
                c.setRequestMethod("POST");
                c.setDoOutput(true);
                c.setConnectTimeout(20000);
                c.setReadTimeout(30000);
                c.setRequestProperty("Authorization", "Bearer " + token);
                c.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                String mime = activity.getContentResolver().getType(uri);
                if (mime == null || !mime.startsWith("image/")) mime = "image/jpeg";
                String filename = "wechat-qr.jpg";
                OutputStream out = c.getOutputStream();
                out.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"" + filename +
                    "\"\r\nContent-Type: " + mime + "\r\n\r\n").getBytes("UTF-8"));
                InputStream in = activity.getContentResolver().openInputStream(uri);
                if (in == null) throw new Exception("无法读取所选图片");
                byte[] buffer = new byte[8192];
                int n;
                while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
                in.close();
                out.write(("\r\n--" + boundary + "--\r\n").getBytes("UTF-8"));
                out.flush();
                out.close();

                String response = readResponse(c);
                JSONObject result = new JSONObject(response);
                String key = result.optString("qr_image_key", "");
                if (key.isEmpty()) throw new Exception(result.optString("message", "上传失败"));
                qrImageKey = key;
                activity.runOnUiThread(() -> qrView.setText("收款码已上传 ✓"));
            } catch (Exception e) {
                activity.runOnUiThread(() -> qrView.setText("上传失败：" + e.getMessage()));
            }
        }).start();
    }

    private void showWithdrawDialog() {
        LinearLayout form = new LinearLayout(activity);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(8), dp(6), dp(8), dp(2));

        EditText amount = new EditText(activity);
        amount.setSingleLine(true);
        amount.setHint("提现金额（元，最低 1 元）");
        amount.setInputType(8194);
        form.addView(amount);

        EditText realName = new EditText(activity);
        realName.setSingleLine(true);
        realName.setHint("微信收款人真实姓名");
        form.addView(realName);

        EditText note = new EditText(activity);
        note.setSingleLine(true);
        note.setHint("备注（选填）");
        form.addView(note);

        new AlertDialog.Builder(activity)
            .setTitle("申请提现")
            .setMessage("请确认已上传微信收款码，提现申请提交后将进入审核。")
            .setView(form)
            .setNegativeButton("取消", null)
            .setPositiveButton("提交申请", (dialog, which) -> {
                String a = amount.getText().toString().trim();
                String name = realName.getText().toString().trim();
                String memo = note.getText().toString().trim();
                if (a.isEmpty() || name.isEmpty()) {
                    toast("请填写提现金额和真实姓名");
                    return;
                }
                if (qrImageKey.isEmpty()) {
                    toast("请先选择并上传微信收款码");
                    return;
                }
                try {
                    double yuan = Double.parseDouble(a);
                    long cents = Math.round(yuan * 100);
                    if (cents < 100) {
                        toast("最低提现金额为 1 元");
                        return;
                    }
                    submitWithdrawal(cents, name, memo);
                } catch (NumberFormatException e) {
                    toast("提现金额格式不正确");
                }
            }).show();
    }

    private void submitWithdrawal(long cents, String name, String note) {
        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("amount_cents", cents);
                body.put("real_name", name);
                body.put("qr_image_key", qrImageKey);
                body.put("note", note);
                JSONObject result = request("POST", "/referral/v1/withdrawals", body, null);
                activity.runOnUiThread(() -> {
                    toast(result.optString("message", "提现申请已提交"));
                    show();
                });
            } catch (Exception e) {
                activity.runOnUiThread(() -> toast("提交失败：" + e.getMessage()));
            }
        }).start();
    }

    private JSONObject request(String method, String path, JSONObject body, String contentType) throws Exception {
        String token = auth.getValidAccessToken();
        HttpURLConnection c = (HttpURLConnection) new URL(BASE + path).openConnection();
        c.setRequestMethod(method);
        c.setConnectTimeout(15000);
        c.setReadTimeout(20000);
        c.setRequestProperty("Authorization", "Bearer " + token);
        if (body != null) {
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", contentType == null ? "application/json; charset=utf-8" : contentType);
            OutputStream out = c.getOutputStream();
            out.write(body.toString().getBytes("UTF-8"));
            out.close();
        }
        String response = readResponse(c);
        JSONObject result = new JSONObject(response);
        if (c.getResponseCode() >= 400) {
            throw new Exception(result.optString("message", "服务器请求失败"));
        }
        return result;
    }

    private JSONArray requestArray(String path) throws Exception {
        String token = auth.getValidAccessToken();
        HttpURLConnection c = (HttpURLConnection) new URL(BASE + path).openConnection();
        c.setRequestMethod("GET");
        c.setConnectTimeout(15000);
        c.setReadTimeout(20000);
        c.setRequestProperty("Authorization", "Bearer " + token);
        String response = readResponse(c);
        if (response.trim().startsWith("[")) {
            return new JSONArray(response);
        }
        JSONObject obj = new JSONObject(response);
        JSONArray rows = obj.optJSONArray("withdrawals");
        if (rows == null) rows = obj.optJSONArray("items");
        if (rows == null) rows = obj.optJSONArray("data");
        return rows == null ? new JSONArray() : rows;
    }

    private String readResponse(HttpURLConnection c) throws Exception {
        int code = c.getResponseCode();
        InputStream in = code >= 400 ? c.getErrorStream() : c.getInputStream();
        if (in == null) throw new Exception("服务器无响应（HTTP " + code + "）");
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        reader.close();
        if (code >= 400) {
            try {
                JSONObject error = new JSONObject(sb.toString());
                throw new Exception(error.optString("message", "HTTP " + code));
            } catch (org.json.JSONException ignored) {
                throw new Exception("HTTP " + code + "：" + sb);
            }
        }
        return sb.toString();
    }

    private void toast(String message) {
        Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
    }
}

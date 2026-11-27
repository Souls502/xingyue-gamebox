package com.xingyue.gamebox;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class CustomerServicePage {

    private static final String BASE = "https://4.wc3.tech";
    private static final int BG = Color.rgb(20, 15, 39);
    private static final int PURPLE = Color.rgb(126, 87, 194);
    private static final int TEXT = Color.rgb(240, 235, 255);
    private static final int MUTED = Color.rgb(170, 160, 195);

    private final Activity activity;
    private final UserAuth auth;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private Dialog dialog;
    private LinearLayout messageList;
    private ScrollView scrollView;
    private EditText input;
    private TextView statusText;

    private String conversationId = "";
    private boolean busy = false;
    private boolean closed = false;
    private String lastMessageId = "";

    private final Runnable pollTask = new Runnable() {
        @Override
        public void run() {
            if (closed || dialog == null || !dialog.isShowing()) {
                return;
            }
            loadMessages(false);
            handler.postDelayed(this, 5000);
        }
    };

    private CustomerServicePage(Activity activity, UserAuth auth) {
        this.activity = activity;
        this.auth = auth;
    }

    public static void open(Activity activity, UserAuth auth) {
        if (activity == null || auth == null || activity.isFinishing()) {
            return;
        }
        new CustomerServicePage(activity, auth).show();
    }

    private void show() {
        dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(14), dp(16), dp(12));
        root.setBackground(round(BG, 20));

        LinearLayout header = new LinearLayout(activity);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);

        TextView title = text("联系客服", 20, TEXT, true);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(42), 1));

        TextView close = text("✕", 22, MUTED, true);
        close.setGravity(Gravity.CENTER);
        close.setOnClickListener(v -> dialog.dismiss());
        header.addView(close, new LinearLayout.LayoutParams(dp(42), dp(42)));

        root.addView(header);

        statusText = text("正在连接客服…", 12, MUTED, false);
        statusText.setPadding(0, 0, 0, dp(8));
        root.addView(statusText);

        scrollView = new ScrollView(activity);
        scrollView.setFillViewport(true);
        scrollView.setBackground(round(Color.rgb(27, 21, 49), 14));

        messageList = new LinearLayout(activity);
        messageList.setOrientation(LinearLayout.VERTICAL);
        messageList.setPadding(dp(10), dp(12), dp(10), dp(12));

        scrollView.addView(messageList);
        LinearLayout.LayoutParams scrollParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 0, 1);
        scrollParams.topMargin = dp(4);
        scrollParams.bottomMargin = dp(10);
        root.addView(scrollView, scrollParams);

        LinearLayout bottom = new LinearLayout(activity);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        bottom.setGravity(Gravity.CENTER_VERTICAL);

        input = new EditText(activity);
        input.setTextColor(TEXT);
        input.setHintTextColor(MUTED);
        input.setHint("输入消息…");
        input.setTextSize(14);
        input.setMaxLines(4);
        input.setPadding(dp(12), dp(8), dp(12), dp(8));
        input.setBackground(round(Color.rgb(39, 31, 66), 16));

        bottom.addView(input, new LinearLayout.LayoutParams(0, dp(48), 1));

        TextView send = text("发送", 14, Color.WHITE, true);
        send.setGravity(Gravity.CENTER);
        send.setBackground(round(PURPLE, 14));

        LinearLayout.LayoutParams sendParams =
                new LinearLayout.LayoutParams(dp(64), dp(48));
        sendParams.leftMargin = dp(8);
        bottom.addView(send, sendParams);

        send.setOnClickListener(v -> sendMessage());

        root.addView(bottom);

        dialog.setContentView(root);
        dialog.setCancelable(true);
        dialog.setOnDismissListener(d -> {
            closed = true;
            handler.removeCallbacks(pollTask);
        });

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT);
            window.setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        dialog.show();

        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT);
        }

        loadConversation();
    }

    private void loadConversation() {
        new Thread(() -> {
            try {
                JSONObject result = request(
                        "GET", "/support/v1/conversation", null);

                conversationId = result.optString("id", "");

                int unread = result.optInt("unread_count", 0);

                activity.runOnUiThread(() -> {
                    if (closed) return;

                    if (unread > 0) {
                        statusText.setText("客服有 " + unread + " 条未读回复");
                    } else {
                        statusText.setText("在线客服 · 文字消息");
                    }

                    loadMessages(true);
                                        handler.removeCallbacks(pollTask);
                    handler.postDelayed(pollTask, 5000);
                });

            } catch (Exception e) {
                activity.runOnUiThread(() -> {
                    if (!closed) {
                        statusText.setText("连接失败，请检查登录状态后重试");
                        addSystemMessage("暂时无法连接客服，请稍后重新打开。");
                    }
                });
            }
        }).start();
    }

    private void loadMessages(boolean force) {
        if (closed || conversationId.isEmpty()) return;
        if (busy && !force) return;

        busy = true;

        new Thread(() -> {
            try {
                                JSONArray finalMessages = requestArray(
                        "GET", "/support/v1/messages");

                activity.runOnUiThread(() -> {
                    if (closed) return;

                    messageList.removeAllViews();
                    lastMessageId = "";

                    for (int i = 0; i < finalMessages.length(); i++) {
                        JSONObject item = finalMessages.optJSONObject(i);
                        if (item == null) continue;

                        String id = item.optString("id", "");
                        String role = item.optString("sender_role", "user");
                        String content = item.optString("content", "");

                        addMessage(role, content);

                        if (!id.isEmpty()) {
                            lastMessageId = id;
                        }
                    }

                    if (finalMessages.length() == 0) {
                        addSystemMessage("您好，有问题可以在这里留言，客服会尽快回复。");
                    }

                    scrollToBottom();
                    statusText.setText("在线客服 · 文字消息");
                });

            } catch (Exception e) {
                activity.runOnUiThread(() -> {
                    if (!closed) {
                        statusText.setText("消息刷新失败，稍后自动重试");
                    }
                });
            } finally {
                busy = false;
            }
        }).start();
    }

    private void sendMessage() {
        if (busy) {
            Toast.makeText(activity, "正在处理，请稍候", Toast.LENGTH_SHORT).show();
            return;
        }

        String content = input.getText().toString().trim();

        if (content.isEmpty()) {
            Toast.makeText(activity, "请输入消息内容", Toast.LENGTH_SHORT).show();
            return;
        }

        if (content.length() > 2000) {
            Toast.makeText(activity, "消息不能超过 2000 字", Toast.LENGTH_SHORT).show();
            return;
        }

        if (conversationId.isEmpty()) {
            Toast.makeText(activity, "客服连接中，请稍后再发", Toast.LENGTH_SHORT).show();
            return;
        }

        busy = true;
        input.setEnabled(false);

        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("content", content);

                request(
                        "POST",
                        "/support/v1/messages",
                        body);

                activity.runOnUiThread(() -> {
                    if (closed) return;
                    input.setText("");
                    input.setEnabled(true);
                    loadMessages(true);
                });

            } catch (Exception e) {
                activity.runOnUiThread(() -> {
                    if (closed) return;
                    input.setEnabled(true);
                    Toast.makeText(
                            activity,
                            "发送失败，请稍后重试",
                            Toast.LENGTH_SHORT).show();
                });
            } finally {
                busy = false;
            }
        }).start();
    }

    private JSONObject request(
            String method,
            String path,
            JSONObject body) throws Exception {

        String token = auth.getValidAccessToken();

        if (token == null || token.trim().isEmpty()) {
            throw new Exception("登录已失效");
        }

        HttpURLConnection connection = null;

        try {
            URL url = new URL(BASE + path);
            connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod(method);
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            connection.setRequestProperty(
                    "Authorization", "Bearer " + token);
            connection.setRequestProperty(
                    "Content-Type", "application/json; charset=UTF-8");
            connection.setRequestProperty(
                    "Accept", "application/json");

            if (body != null) {
                connection.setDoOutput(true);

                byte[] bytes = body.toString().getBytes("UTF-8");

                try (OutputStream output = connection.getOutputStream()) {
                    output.write(bytes);
                }
            }

            int code = connection.getResponseCode();

            InputStream stream = code >= 200 && code < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream();

            String response = readStream(stream);

            if (code < 200 || code >= 300) {
                throw new Exception("HTTP " + code + ": " + response);
            }

            if (response == null || response.trim().isEmpty()) {
                return new JSONObject();
            }

            return new JSONObject(response);

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

        private JSONArray requestArray(
            String method,
            String path) throws Exception {

        String token = auth.getValidAccessToken();

        if (token == null || token.trim().isEmpty()) {
            throw new Exception("登录已失效");
        }

        HttpURLConnection connection = null;

        try {
            URL url = new URL(BASE + path);
            connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod(method);
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            connection.setRequestProperty(
                    "Authorization", "Bearer " + token);
            connection.setRequestProperty(
                    "Accept", "application/json");

            int code = connection.getResponseCode();

            InputStream stream = code >= 200 && code < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream();

            String response = readStream(stream);

            if (code < 200 || code >= 300) {
                throw new Exception("HTTP " + code + ": " + response);
            }

            return new JSONArray(response);

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        }

    private String readStream(InputStream stream) throws Exception {
        if (stream == null) return "";

        StringBuilder builder = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, "UTF-8"))) {

            String line;

            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }

        return builder.toString();
    }

    private void addMessage(String role, String content) {
        boolean fromUser = "user".equalsIgnoreCase(role);

        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setGravity(fromUser ? Gravity.RIGHT : Gravity.LEFT);
        row.setPadding(0, dp(4), 0, dp(4));

        TextView bubble = text(content, 14, TEXT, false);
        bubble.setPadding(dp(12), dp(9), dp(12), dp(9));
        bubble.setMaxWidth((int) (activity.getResources()
                .getDisplayMetrics().widthPixels * 0.72f));
        bubble.setBackground(round(
                fromUser
                        ? Color.rgb(102, 70, 165)
                        : Color.rgb(49, 40, 76),
                16));

        row.addView(bubble);
        messageList.addView(row);
    }

    private void addSystemMessage(String content) {
        TextView message = text(content, 12, MUTED, false);
        message.setGravity(Gravity.CENTER);
        message.setPadding(dp(8), dp(10), dp(8), dp(10));
        messageList.addView(message);
    }

    private void scrollToBottom() {
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }

    private TextView text(
            String value,
            int size,
            int color,
            boolean bold) {

        TextView view = new TextView(activity);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);

        if (bold) {
            view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }

        return view;
    }

    private GradientDrawable round(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }

    private int dp(float value) {
        return (int) (value * activity.getResources()
                .getDisplayMetrics().density + 0.5f);
    }
                                                 }

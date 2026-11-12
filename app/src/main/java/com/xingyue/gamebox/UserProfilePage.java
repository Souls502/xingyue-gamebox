package com.xingyue.gamebox;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.Button;
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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class UserProfilePage {
    private static final String BASE =
        "https://vavsdizrdsvavioxsmjh.supabase.co";
    private static final String API_KEY =
        "sb_publishable_r1RfRiRkwcDVOoOtO-Vj8w_sQAs2VIk";

    private static final String[] AVATAR_KEYS = {
        "moon_1", "star_1", "rabbit_1",
        "cat_1", "planet_1", "fox_1"
    };
    private static final String[] AVATAR_ICONS = {
        "🌙", "⭐", "🐰", "🐱", "🪐", "🦊"
    };

    private static final int PURPLE = Color.rgb(19, 14, 35);
    private static final int ACCENT = 0xFF7545B5;
    private static final int CARD = 0xFF302546;
    private static final int MUTED = 0xFFB9AED5;

    private final Activity activity;
    private final UserAuth auth;
    private final Runnable onBack;
    private EditText nicknameInput;
    private TextView avatarPreview;
    private TextView[] avatarChoices;
    private String selectedAvatar = AVATAR_KEYS[0];
    private boolean loading = false;

    private UserProfilePage(Activity activity, UserAuth auth,
                            Runnable onBack) {
        this.activity = activity;
        this.auth = auth;
        this.onBack = onBack;
    }

    public static void open(Activity activity, UserAuth auth,
                            Runnable onBack) {
        new UserProfilePage(activity, auth, onBack).show();
    }

    private int dp(float value) {
        return (int) (value *
            activity.getResources().getDisplayMetrics().density + 0.5f);
    }

    private GradientDrawable background(int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    private TextView text(String value, int size, int color) {
        TextView view = new TextView(activity);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        return view;
    }

    private void show() {
        ScrollView scroll = new ScrollView(activity);
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(24), dp(20), dp(30));
        root.setBackgroundColor(PURPLE);
        scroll.addView(root);

        ((MainActivity) activity).showPageWithBottomNav(scroll, 2);

        TextView back = text("‹ 返回游戏盒", 16, 0xFFD5A7FF);
        root.addView(back);
        back.setOnClickListener(v -> onBack.run());

        TextView title = text("✦ 个人资料", 27, Color.WHITE);
        title.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams titleParams =
            new LinearLayout.LayoutParams(-1, -2);
        titleParams.topMargin = dp(22);
        root.addView(title, titleParams);

        TextView email = text("账号邮箱：" + auth.getEmail(), 14, MUTED);
        LinearLayout.LayoutParams emailParams =
            new LinearLayout.LayoutParams(-1, -2);
        emailParams.topMargin = dp(12);
        root.addView(email, emailParams);

        avatarPreview = text("🌙", 48, Color.WHITE);
        avatarPreview.setGravity(Gravity.CENTER);
        avatarPreview.setBackground(background(CARD, 60));
        LinearLayout.LayoutParams previewParams =
            new LinearLayout.LayoutParams(dp(112), dp(112));
        previewParams.gravity = Gravity.CENTER_HORIZONTAL;
        previewParams.topMargin = dp(26);
        root.addView(avatarPreview, previewParams);

        TextView avatarTitle = text("选择头像", 17, Color.WHITE);
        avatarTitle.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams avatarTitleParams =
            new LinearLayout.LayoutParams(-1, -2);
        avatarTitleParams.topMargin = dp(24);
        avatarTitleParams.bottomMargin = dp(12);
        root.addView(avatarTitle, avatarTitleParams);

        LinearLayout avatarRow = new LinearLayout(activity);
        avatarRow.setOrientation(LinearLayout.HORIZONTAL);
        avatarRow.setGravity(Gravity.CENTER);
        avatarChoices = new TextView[AVATAR_KEYS.length];

        for (int i = 0; i < AVATAR_KEYS.length; i++) {
            final int index = i;
            TextView choice = text(AVATAR_ICONS[i], 25, Color.WHITE);
            choice.setGravity(Gravity.CENTER);
            choice.setBackground(background(i == 0 ? ACCENT : CARD, 16));
            LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(0, dp(54), 1);
            params.setMargins(dp(3), 0, dp(3), 0);
            avatarRow.addView(choice, params);
            avatarChoices[i] = choice;
            choice.setOnClickListener(v -> selectAvatar(index));
        }
        root.addView(avatarRow);

        TextView nicknameTitle = text("昵称", 17, Color.WHITE);
        nicknameTitle.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams nicknameParams =
            new LinearLayout.LayoutParams(-1, -2);
        nicknameParams.topMargin = dp(25);
        root.addView(nicknameTitle, nicknameParams);

        nicknameInput = new EditText(activity);
        nicknameInput.setSingleLine(true);
        nicknameInput.setTextColor(Color.WHITE);
        nicknameInput.setHintTextColor(MUTED);
        nicknameInput.setHint("请输入游戏盒昵称");
        nicknameInput.setTextSize(16);
        nicknameInput.setPadding(dp(14), dp(12), dp(14), dp(12));
        nicknameInput.setBackground(background(CARD, 12));
        LinearLayout.LayoutParams inputParams =
            new LinearLayout.LayoutParams(-1, -2);
        inputParams.topMargin = dp(10);
        root.addView(nicknameInput, inputParams);

        Button save = new Button(activity);
        save.setText("保存资料");
        save.setTextColor(Color.WHITE);
        save.setBackground(background(ACCENT, 14));
        LinearLayout.LayoutParams saveParams =
            new LinearLayout.LayoutParams(-1, -2);
        saveParams.topMargin = dp(24);
        root.addView(save, saveParams);
        save.setOnClickListener(v -> saveProfile(save));

        TextView logout = text("退出登录", 15, 0xFFFFA6A6);
        logout.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams logoutParams =
            new LinearLayout.LayoutParams(-1, dp(52));
        logoutParams.topMargin = dp(12);
        root.addView(logout, logoutParams);
        logout.setOnClickListener(v -> confirmLogout());

        loadProfile();
    }

    private void selectAvatar(int index) {
        selectedAvatar = AVATAR_KEYS[index];
        avatarPreview.setText(AVATAR_ICONS[index]);
        for (int i = 0; i < avatarChoices.length; i++) {
            avatarChoices[i].setBackground(background(
                i == index ? ACCENT : CARD, 16));
        }
    }

    private void loadProfile() {
        if (loading) return;
        loading = true;

        new Thread(() -> {
            try {
                String path = "/rest/v1/profiles?id=eq."
                    + auth.getUserId()
                    + "&select=nickname,avatar_key";
                JSONArray rows = new JSONArray(request("GET", path, null));

                activity.runOnUiThread(() -> {
                    if (rows.length() > 0) {
                        JSONObject profile = rows.optJSONObject(0);
                        if (profile != null) {
                            nicknameInput.setText(
                                profile.optString("nickname", ""));
                            String key = profile.optString(
                                "avatar_key", AVATAR_KEYS[0]);
                            for (int i = 0; i < AVATAR_KEYS.length; i++) {
                                if (AVATAR_KEYS[i].equals(key)) {
                                    selectAvatar(i);
                                    break;
                                }
                            }
                        }
                    }
                    loading = false;
                });
            } catch (Exception e) {
                activity.runOnUiThread(() -> {
                    loading = false;
                    Toast.makeText(activity,
                        "读取资料失败：" + e.getMessage(),
                        Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void saveProfile(Button save) {
        String nickname = nicknameInput.getText().toString().trim();
        if (nickname.isEmpty()) {
            nicknameInput.setError("请输入昵称");
            return;
        }
        if (nickname.length() > 20) {
            nicknameInput.setError("昵称最多 20 个字符");
            return;
        }

        save.setEnabled(false);
        save.setText("正在保存...");

        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("id", auth.getUserId());
                body.put("nickname", nickname);
                body.put("avatar_key", selectedAvatar);

                SimpleDateFormat format = new SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
                format.setTimeZone(TimeZone.getTimeZone("UTC"));
                body.put("updated_at", format.format(new Date()));

                request("POST",
                    "/rest/v1/profiles?on_conflict=id",
                    body.toString());

                activity.runOnUiThread(() -> {
                    save.setEnabled(true);
                    save.setText("保存资料");
                    Toast.makeText(activity,
                        "个人资料保存成功！",
                        Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                activity.runOnUiThread(() -> {
                    save.setEnabled(true);
                    save.setText("保存资料");
                    new AlertDialog.Builder(activity)
                        .setTitle("保存失败")
                        .setMessage(e.getMessage())
                        .setPositiveButton("知道了", null)
                        .show();
                });
            }
        }).start();
    }

    private String request(String method, String path, String body)
            throws Exception {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(BASE + path);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("apikey", API_KEY);
            conn.setRequestProperty("Authorization",
                "Bearer " + auth.getAccessToken());
            conn.setRequestProperty("Accept", "application/json");

            if (body != null) {
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type",
                    "application/json; charset=UTF-8");
                conn.setRequestProperty("Prefer",
                    "resolution=merge-duplicates,return=minimal");
                try (OutputStream out = conn.getOutputStream()) {
                    out.write(body.getBytes("UTF-8"));
                }
            }

            int code = conn.getResponseCode();
            InputStream stream = code >= 200 && code < 300
                ? conn.getInputStream() : conn.getErrorStream();

            StringBuilder response = new StringBuilder();
            if (stream != null) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(stream, "UTF-8"))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                }
            }

            if (code < 200 || code >= 300) {
                throw new Exception("服务器返回 " + code + "：" + response);
            }
            return response.toString();
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private void confirmLogout() {
        new AlertDialog.Builder(activity)
            .setTitle("退出登录")
            .setMessage("确定要退出当前账号吗？")
            .setPositiveButton("退出", (dialog, which) -> {
                new Thread(() -> {
                    auth.logout();
                    activity.runOnUiThread(() -> {
                        Toast.makeText(activity, "已退出登录",
                            Toast.LENGTH_SHORT).show();
                        onBack.run();
                    });
                }).start();
            })
            .setNegativeButton("取消", null)
            .show();
    }
}

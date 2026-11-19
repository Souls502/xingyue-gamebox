package com.xingyue.gamebox;

import android.content.Context;
import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
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

    private static final String BASE = "https://4.wc3.tech";

    private static final int BG = 0xFF100824;
    private static final int CARD = 0xFF302044;
    private static final int ACCENT = 0xFF7545B5;
    private static final int MUTED = 0xFFB9AED5;

    private final Activity activity;
    private final UserAuth auth;
    private final Runnable onBack;

    private EditText nicknameInput;
    private boolean loading = false;

    private UserProfilePage(
        Activity activity,
        UserAuth auth,
        Runnable onBack
    ) {
        this.activity = activity;
        this.auth = auth;
        this.onBack = onBack;
    }

    public static void open(
        Activity activity,
        UserAuth auth,
        Runnable onBack
    ) {
        new UserProfilePage(
            activity, auth, onBack
        ).show();
    }

    private int dp(float value) {
        return (int) (
            value * activity.getResources()
                .getDisplayMetrics().density + 0.5f
        );
    }

    private GradientDrawable background(
        int color, int radiusDp
    ) {
        GradientDrawable drawable =
            new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        drawable.setStroke(dp(1), 0xFF7440B5);
        return drawable;
    }

    private GradientDrawable gradient(
        int start, int end, int radiusDp
    ) {
        GradientDrawable drawable =
            new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{start, end}
            );
        drawable.setCornerRadius(dp(radiusDp));
        drawable.setStroke(dp(1), 0xFF8747D5);
        return drawable;
    }

    private TextView text(
        String value, int size, int color
    ) {
        TextView view = new TextView(activity);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        return view;
    }

    private void show() {
        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(true);

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(
            dp(20), dp(24), dp(20), dp(30)
        );
        root.setBackgroundColor(BG);

        scroll.addView(root);

        ((MainActivity) activity)
            .showPageWithBottomNav(scroll, 2);

        TextView back = text(
            "‹ 返回用户中心", 16, 0xFFD5A7FF
        );
        root.addView(back);
        back.setOnClickListener(v -> onBack.run());

        TextView title = text(
            "✦ 个人资料", 27, Color.WHITE
        );
        title.setTypeface(null, Typeface.BOLD);

        LinearLayout.LayoutParams titleParams =
            new LinearLayout.LayoutParams(-1, -2);
        titleParams.topMargin = dp(22);
        root.addView(title, titleParams);

        TextView subtitle = text(
            "管理你的星月游戏盒账号信息",
            13, MUTED
        );
        LinearLayout.LayoutParams subtitleParams =
            new LinearLayout.LayoutParams(-1, -2);
        subtitleParams.topMargin = dp(7);
        root.addView(subtitle, subtitleParams);

        // 默认月亮头像
        LinearLayout avatarCard =
            new LinearLayout(activity);
        avatarCard.setOrientation(
            LinearLayout.VERTICAL
        );
        avatarCard.setGravity(Gravity.CENTER);
        avatarCard.setPadding(
            dp(16), dp(22), dp(16), dp(20)
        );
        avatarCard.setBackground(gradient(
            0xFF421B85, 0xFF211044, 20
        ));

        LinearLayout.LayoutParams avatarCardParams =
            new LinearLayout.LayoutParams(-1, -2);
        avatarCardParams.topMargin = dp(24);
        root.addView(
            avatarCard, avatarCardParams
        );

        MoonAvatar avatar = new MoonAvatar(activity);

        LinearLayout.LayoutParams avatarParams =
            new LinearLayout.LayoutParams(
                dp(112), dp(112)
            );
        avatarCard.addView(avatar, avatarParams);

        TextView avatarName = text(
            "星月默认头像", 16, Color.WHITE
        );
        avatarName.setTypeface(null, Typeface.BOLD);

        LinearLayout.LayoutParams avatarNameParams =
            new LinearLayout.LayoutParams(-2, -2);
        avatarNameParams.topMargin = dp(14);
        avatarCard.addView(
            avatarName, avatarNameParams
        );

        TextView avatarHint = text(
            "专属月亮头像，陪伴你的游戏之旅",
            12, MUTED
        );

        LinearLayout.LayoutParams hintParams =
            new LinearLayout.LayoutParams(-2, -2);
        hintParams.topMargin = dp(6);
        avatarCard.addView(
            avatarHint, hintParams
        );

        // 邮箱信息
        addSectionTitle(root, "账号邮箱");

        TextView email = text(
            auth.getEmail(), 15, 0xFFE5D5FF
        );
        email.setPadding(
            dp(14), dp(15), dp(14), dp(15)
        );
        email.setBackground(background(CARD, 12));

        root.addView(email,
            new LinearLayout.LayoutParams(-1, -2));

        // 昵称
        addSectionTitle(root, "我的昵称");

        nicknameInput = new EditText(activity);
        nicknameInput.setSingleLine(true);
        nicknameInput.setTextColor(Color.WHITE);
        nicknameInput.setHintTextColor(MUTED);
        nicknameInput.setHint("请输入游戏盒昵称");
        nicknameInput.setTextSize(16);
        nicknameInput.setPadding(
            dp(14), dp(12), dp(14), dp(12)
        );
        nicknameInput.setBackground(
            background(CARD, 12)
        );

        root.addView(nicknameInput,
            new LinearLayout.LayoutParams(-1, -2));

        // 保存按钮
        Button save = new Button(activity);
        save.setText("保存资料");
        save.setTextColor(Color.WHITE);
        save.setTextSize(16);
        save.setBackground(gradient(
            0xFF8545D0, 0xFF54238E, 14
        ));

        LinearLayout.LayoutParams saveParams =
            new LinearLayout.LayoutParams(-1, dp(54));
        saveParams.topMargin = dp(25);
        root.addView(save, saveParams);

        save.setOnClickListener(
            v -> saveProfile(save)
        );

        // 返回按钮
        TextView backButton = text(
            "返回用户中心", 15, 0xFFD5A7FF
        );
        backButton.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams backParams =
            new LinearLayout.LayoutParams(-1, dp(52));
        backParams.topMargin = dp(10);
        root.addView(backButton, backParams);

        backButton.setOnClickListener(
            v -> onBack.run()
        );

        loadProfile();
    }

    private void addSectionTitle(
        LinearLayout root, String title
    ) {
        TextView label = text(
            title, 17, Color.WHITE
        );
        label.setTypeface(null, Typeface.BOLD);

        LinearLayout.LayoutParams params =
            new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(24);
        params.bottomMargin = dp(10);

        root.addView(label, params);
    }

    // 从自建服务器读取当前用户昵称
    private void loadProfile() {
        if (loading) return;
        loading = true;

        new Thread(() -> {
            try {
                String path =
                    "/rest/v1/profiles?id=eq."
                    + auth.getUserId()
                    + "&select=nickname";

                JSONArray rows = new JSONArray(
                    request("GET", path, null)
                );

                activity.runOnUiThread(() -> {
                    if (rows.length() > 0) {
                        JSONObject profile =
                            rows.optJSONObject(0);

                        if (profile != null) {
                            nicknameInput.setText(
                                profile.optString(
                                    "nickname", ""
                                )
                            );
                        }
                    }
                    loading = false;
                });

            } catch (Exception e) {
                activity.runOnUiThread(() -> {
                    loading = false;
                    Toast.makeText(
                        activity,
                        "读取资料失败：" + e.getMessage(),
                        Toast.LENGTH_LONG
                    ).show();
                });
            }
        }).start();
    }

    // 保存昵称到自建服务器
    private void saveProfile(Button save) {
        String nickname = nicknameInput
            .getText().toString().trim();

        if (nickname.isEmpty()) {
            nicknameInput.setError("请输入昵称");
            return;
        }

        if (nickname.length() > 20) {
            nicknameInput.setError(
                "昵称最多 20 个字符"
            );
            return;
        }

        save.setEnabled(false);
        save.setText("正在保存...");

        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("nickname", nickname);

                request(
                    "PATCH",
                    "/rest/v1/profiles?id=eq."
                        + auth.getUserId(),
                    body.toString()
                );

                activity.runOnUiThread(() -> {
                    save.setEnabled(true);
                    save.setText("保存资料");

                    Toast.makeText(
                        activity,
                        "个人资料保存成功！",
                        Toast.LENGTH_SHORT
                    ).show();
                });

            } catch (Exception e) {
                activity.runOnUiThread(() -> {
                    save.setEnabled(true);
                    save.setText("保存资料");

                    new AlertDialog.Builder(activity)
                        .setTitle("保存失败")
                        .setMessage(e.getMessage())
                        .setPositiveButton(
                            "知道了", null
                        )
                        .show();
                });
            }
        }).start();
    }
    private String request(String method, String path, String body) throws Exception {
        String token = auth.getValidAccessToken();
        if (token == null || token.isEmpty()) {
            throw new Exception("登录状态已失效，请重新登录");
        }

        URL url = new URL(BASE + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        conn.setRequestProperty("Authorization", "Bearer " + token);
        conn.setRequestProperty("Accept", "application/json");

        if (body != null) {
            conn.setDoOutput(true);
            conn.setRequestProperty(
                    "Content-Type",
                    "application/json; charset=UTF-8"
            );

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes("UTF-8"));
            }
        }

        int code = conn.getResponseCode();
        InputStream input = code >= 200 && code < 300
                ? conn.getInputStream()
                : conn.getErrorStream();

        StringBuilder result = new StringBuilder();

        if (input != null) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(input, "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
            }
        }

        conn.disconnect();

        if (code < 200 || code >= 300) {
            throw new Exception("服务器请求失败（" + code + "）：" + result);
        }

        return result.toString();
    }

    private static class MoonAvatar extends View {

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        public MoonAvatar(Context context) {
            super(context);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            float w = getWidth();
            float h = getHeight();
            float cx = w / 2f;
            float cy = h / 2f;
            float radius = Math.min(w, h) * 0.42f;

            paint.setShader(null);
            paint.setColor(Color.rgb(91, 65, 150));
            canvas.drawCircle(cx, cy, radius, paint);

            paint.setColor(Color.rgb(242, 220, 255));
            canvas.drawCircle(cx - radius * 0.12f,
                    cy - radius * 0.02f,
                    radius * 0.62f,
                    paint);

            paint.setColor(Color.rgb(91, 65, 150));
            canvas.drawCircle(cx + radius * 0.18f,
                    cy - radius * 0.22f,
                    radius * 0.55f,
                    paint);

            paint.setColor(Color.WHITE);
            canvas.drawCircle(cx + radius * 0.42f,
                    cy - radius * 0.35f,
                    radius * 0.055f,
                    paint);

            canvas.drawCircle(cx + radius * 0.62f,
                    cy - radius * 0.05f,
                    radius * 0.035f,
                    paint);
        }
    }
                              }

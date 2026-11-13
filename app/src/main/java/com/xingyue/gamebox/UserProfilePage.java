package com.xingyue.gamebox;

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

    private static final String BASE =
        "https://vavsdizrdsvavioxsmjh.supabase.co";

    private static final String API_KEY =
        "sb_publishable_r1RfRiRkwcDVOoOtO-Vj8w_sQAs2VIk";

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
                body.put("id", auth.getUserId());
                body.put("nickname", nickname);

                SimpleDateFormat format =
                    new SimpleDateFormat(
                        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                        Locale.US
                    );

                format.setTimeZone(
                    TimeZone.getTimeZone("UTC")
                );

                body.put(
                    "updated_at",
                    format.format(new Date())
                );

                request(
                    "POST",
                    "/rest/v1/profiles?on_conflict=id",
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

    private String request(
        String method, String path, String body
    ) throws Exception {

        HttpURLConnection conn = null;

        try {
            URL url = new URL(BASE + path);
            conn = (HttpURLConnection)
                url.openConnection();

            conn.setRequestMethod(method);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);

            conn.setRequestProperty(
                "apikey", API_KEY
            );

            conn.setRequestProperty(
                "Authorization",
                "Bearer " + auth.getValidAccessToken()
            );

            conn.setRequestProperty(
                "Accept", "application/json"
            );

            if (body != null) {
                conn.setDoOutput(true);

                conn.setRequestProperty(
                    "Content-Type",
                    "application/json; charset=UTF-8"
                );

                conn.setRequestProperty(
                    "Prefer",
                    "resolution=merge-duplicates,return=minimal"
                );

                try (OutputStream out =
                    conn.getOutputStream()) {
                    out.write(body.getBytes("UTF-8"));
                }
            }

            int code = conn.getResponseCode();

            InputStream stream =
                code >= 200 && code < 300
                    ? conn.getInputStream()
                    : conn.getErrorStream();

            StringBuilder response =
                new StringBuilder();

            if (stream != null) {
                try (BufferedReader reader =
                    new BufferedReader(
                        new InputStreamReader(
                            stream, "UTF-8"
                        )
                    )) {
                    String line;

                    while ((line = reader.readLine())
                        != null) {
                        response.append(line);
                    }
                }
            }

            if (code < 200 || code >= 300) {
                throw new Exception(
                    "服务器返回 " + code + "："
                    + response
                );
            }

            return response.toString();

        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    // 与用户中心一致的月亮默认头像
    private class MoonAvatar extends View {

        private final Paint paint =
            new Paint(Paint.ANTI_ALIAS_FLAG);

        public MoonAvatar(Activity context) {
            super(context);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            float w = getWidth();
            float h = getHeight();
            float cx = w / 2f;
            float cy = h / 2f;
            float r = Math.min(w, h) * 0.46f;

            // 紫色星空背景
            paint.setShader(new LinearGradient(
                0, 0, w, h,
                0xFF35105F,
                0xFF170A35,
                Shader.TileMode.CLAMP
            ));

            canvas.drawCircle(
                cx, cy, r, paint
            );

            paint.setShader(null);

            // 紫色发光圆环
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(3));
            paint.setColor(0xFFDDAAFF);

            canvas.drawCircle(
                cx, cy, r - dp(2), paint
            );

            paint.setStyle(Paint.Style.FILL);

            // 金色月亮
            paint.setColor(0xFFFFD66E);

            canvas.drawCircle(
                cx - r * 0.05f,
                cy,
                r * 0.53f,
                paint
            );

            // 遮挡月亮，形成弯月
            paint.setColor(0xFF35105F);

            canvas.drawCircle(
                cx + r * 0.23f,
                cy - r * 0.19f,
                r * 0.46f,
                paint
            );

            // 星星
            paint.setColor(0xFFFFE28A);

            drawStar(
                canvas,
                cx + r * 0.43f,
                cy - r * 0.30f,
                r * 0.17f
            );

            paint.setColor(0xFFFFE9B0);

            canvas.drawCircle(
                cx - r * 0.48f,
                cy - r * 0.48f,
                dp(1.5f),
                paint
            );
        }

        private void drawStar(
            Canvas canvas,
            float x,
            float y,
            float size
        ) {
            android.graphics.Path path =
                new android.graphics.Path();

            path.moveTo(x, y - size);
            path.lineTo(
                x + size * 0.25f,
                y - size * 0.25f
            );
            path.lineTo(x + size, y);
            path.lineTo(
                x + size * 0.25f,
                y + size * 0.25f
            );
            path.lineTo(x, y + size);
            path.lineTo(
                x - size * 0.25f,
                y + size * 0.25f
            );
            path.lineTo(x - size, y);
            path.lineTo(
                x - size * 0.25f,
                y - size * 0.25f
            );
            path.close();

            canvas.drawPath(path, paint);
        }
    }
            }

package com.xingyue.gamebox;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UserCenterPage {

    private static final String BASE =
        "https://vavsdizrdsvavioxsmjh.supabase.co";

    private static final String API_KEY =
        "sb_publishable_r1RfRiRkwcDVOoOtO-Vj8w_sQAs2VIk";

    private static final int BG = 0xFF130E23;
    private static final int CARD = 0xFF302546;
    private static final int PURPLE = 0xFF7545B5;
    private static final int MUTED = 0xFFB9AED5;

    private final Activity activity;
    private final UserAuth auth;
    private final Runnable onHome;
    private final Runnable onCategories;

    private TextView avatarView;
    private TextView nicknameView;

    private String nickname = "星月玩家";
    private String avatar = "🌙";

    private UserCenterPage(
        Activity activity,
        UserAuth auth,
        Runnable onHome,
        Runnable onCategories
    ) {
        this.activity = activity;
        this.auth = auth;
        this.onHome = onHome;
        this.onCategories = onCategories;
    }

    public static void open(
        Activity activity,
        UserAuth auth,
        Runnable onHome,
        Runnable onCategories
    ) {
        new UserCenterPage(
            activity, auth, onHome, onCategories
        ).show();
    }

    private int dp(float value) {
        return (int) (
            value * activity.getResources()
                .getDisplayMetrics().density + 0.5f
        );
    }

    private GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
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

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(1);
        root.setPadding(dp(18), dp(24), dp(18), dp(20));
        root.setBackgroundColor(BG);

        scroll.addView(root);

        // 使用主页面的全局底部导航
        ((MainActivity) activity).showPageWithBottomNav(
            scroll, 2
        );

        TextView title = text("✦ 用户中心", 27, Color.WHITE);
        title.setTypeface(null, Typeface.BOLD);
        root.addView(title);

        // 用户信息卡片
        LinearLayout userCard = new LinearLayout(activity);
        userCard.setGravity(Gravity.CENTER_VERTICAL);
        userCard.setPadding(dp(18), dp(20), dp(18), dp(20));
        userCard.setBackground(shape(CARD, 18));

        LinearLayout.LayoutParams userParams =
            new LinearLayout.LayoutParams(-1, -2);
        userParams.topMargin = dp(22);
        root.addView(userCard, userParams);

        avatarView = text(avatar, 42, Color.WHITE);
        avatarView.setGravity(Gravity.CENTER);
        avatarView.setBackground(shape(PURPLE, 50));

        LinearLayout.LayoutParams avatarParams =
            new LinearLayout.LayoutParams(dp(76), dp(76));
        userCard.addView(avatarView, avatarParams);

        LinearLayout userInfo = new LinearLayout(activity);
        userInfo.setOrientation(1);
        userInfo.setPadding(dp(15), 0, 0, 0);

        userCard.addView(userInfo,
            new LinearLayout.LayoutParams(0, -2, 1));

        nicknameView = text(nickname, 20, Color.WHITE);
        nicknameView.setTypeface(null, Typeface.BOLD);
        userInfo.addView(nicknameView);

        TextView email = text(
            auth.getEmail(), 13, MUTED
        );

        LinearLayout.LayoutParams emailParams =
            new LinearLayout.LayoutParams(-1, -2);
        emailParams.topMargin = dp(7);
        userInfo.addView(email, emailParams);

        TextView vip = text("✦ 普通用户", 13, 0xFFD5A7FF);
        LinearLayout.LayoutParams vipParams =
            new LinearLayout.LayoutParams(-1, -2);
        vipParams.topMargin = dp(7);
        userInfo.addView(vip, vipParams);

        // 积分和收藏
        LinearLayout stats = new LinearLayout(activity);
        stats.setOrientation(0);

        LinearLayout.LayoutParams statsParams =
            new LinearLayout.LayoutParams(-1, dp(92));
        statsParams.topMargin = dp(16);
        root.addView(stats, statsParams);

        addStat(stats, "我的积分", "--");
        addStat(stats, "我的收藏", "--");

        // 会员中心
        addMenu(root, "👑 会员中心", "查看会员等级与权益", () -> {
            showMessage("会员中心", "会员功能正在开发中，敬请期待。");
        });

        // 每日签到
        addMenu(root, "📅 每日签到", "每日签到，领取积分", () -> {
            showMessage("每日签到", "签到功能正在开发中，敬请期待。");
        });

        // 我的资料
        addMenu(root, "👤 我的资料", "修改头像和昵称", () -> {
            UserProfilePage.open(
                activity,
                auth,
                () -> show()
            );
        });

        // 消息通知
        addMenu(root, "🔔 消息通知", "查看系统消息与公告", () -> {
            showMessage("消息通知", "消息功能正在开发中，敬请期待。");
        });

        // 退出登录
        TextView logout = text("退出登录", 15, 0xFFFFA6A6);
        logout.setGravity(Gravity.CENTER);
        logout.setBackground(shape(CARD, 14));

        LinearLayout.LayoutParams logoutParams =
            new LinearLayout.LayoutParams(-1, dp(52));
        logoutParams.topMargin = dp(24);
        root.addView(logout, logoutParams);

        logout.setOnClickListener(v -> confirmLogout());

        loadProfile();
    }

    private void addStat(
        LinearLayout parent,
        String name,
        String number
    ) {
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(1);
        card.setGravity(Gravity.CENTER);
        card.setBackground(shape(CARD, 16));

        LinearLayout.LayoutParams params =
            new LinearLayout.LayoutParams(0, -1, 1);
        params.setMargins(dp(4), 0, dp(4), 0);
        parent.addView(card, params);

        TextView value = text(number, 23, Color.WHITE);
        value.setTypeface(null, Typeface.BOLD);
        card.addView(value);

        TextView label = text(name, 13, MUTED);
        LinearLayout.LayoutParams labelParams =
            new LinearLayout.LayoutParams(-2, -2);
        labelParams.topMargin = dp(5);
        card.addView(label, labelParams);
    }

    private void addMenu(
        LinearLayout root,
        String title,
        String subtitle,
        Runnable action
    ) {
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(1);
        card.setPadding(dp(17), dp(14), dp(17), dp(14));
        card.setBackground(shape(CARD, 14));

        LinearLayout.LayoutParams params =
            new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(12);
        root.addView(card, params);

        TextView heading = text(title, 17, Color.WHITE);
        heading.setTypeface(null, Typeface.BOLD);
        card.addView(heading);

        TextView desc = text(subtitle, 12, MUTED);
        LinearLayout.LayoutParams descParams =
            new LinearLayout.LayoutParams(-1, -2);
        descParams.topMargin = dp(5);
        card.addView(desc, descParams);

        card.setOnClickListener(v -> action.run());
    }

    private void showMessage(String title, String message) {
        new AlertDialog.Builder(activity)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("知道了", null)
            .show();
    }

    private void loadProfile() {
        new Thread(() -> {
            HttpURLConnection conn = null;

            try {
                URL url = new URL(
                    BASE + "/rest/v1/profiles?id=eq."
                    + auth.getUserId()
                    + "&select=nickname,avatar_key"
                );

                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(12000);
                conn.setReadTimeout(12000);
                conn.setRequestProperty("apikey", API_KEY);
                conn.setRequestProperty(
                    "Authorization",
                    "Bearer " + auth.getAccessToken()
                );

                int code = conn.getResponseCode();

                if (code != 200) return;

                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                        conn.getInputStream(), "UTF-8"
                    )
                );

                StringBuilder result = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }

                reader.close();

                JSONArray rows = new JSONArray(result.toString());

                if (rows.length() > 0) {
                    JSONObject obj = rows.getJSONObject(0);
                    String newName = obj.optString(
                        "nickname", "星月玩家"
                    );
                    String key = obj.optString(
                        "avatar_key", "moon_1"
                    );

                    String newAvatar = "🌙";

                    if (key.equals("star_1")) newAvatar = "⭐";
                    if (key.equals("rabbit_1")) newAvatar = "🐰";
                    if (key.equals("cat_1")) newAvatar = "🐱";
                    if (key.equals("planet_1")) newAvatar = "🪐";
                    if (key.equals("fox_1")) newAvatar = "🦊";

                    final String finalName = newName;
                    final String finalAvatar = newAvatar;

                    activity.runOnUiThread(() -> {
                        nickname = finalName;
                        avatar = finalAvatar;
                        nicknameView.setText(nickname);
                        avatarView.setText(avatar);
                    });
                }

            } catch (Exception e) {
                // 资料读取失败时保留默认显示
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    private void confirmLogout() {
        new AlertDialog.Builder(activity)
            .setTitle("退出登录")
            .setMessage("确定要退出当前账号吗？")
            .setPositiveButton("退出", (dialog, which) -> {
                new Thread(() -> {
                    auth.logout();
                    activity.runOnUiThread(() -> {
                        Toast.makeText(
                            activity,
                            "已退出登录",
                            Toast.LENGTH_SHORT
                        ).show();
                        onHome.run();
                    });
                }).start();
            })
            .setNegativeButton("取消", null)
            .show();
    }
}

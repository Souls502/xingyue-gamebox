package com.xingyue.gamebox;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
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

    private static final int BG = 0xFF100824;
    private static final int CARD = 0xFF32165E;
    private static final int MUTED = 0xFFD0B9F0;
    private static final int WHITE = Color.WHITE;

    private final Activity activity;
    private final UserAuth auth;
    private final Runnable onHome;
    private final Runnable onCategories;

    private TextView nicknameView;

    private String nickname = "星月玩家";

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

    private GradientDrawable gradient(
        int start, int end, int radius
    ) {
        GradientDrawable d = new GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            new int[]{start, end}
        );
        d.setCornerRadius(dp(radius));
        d.setStroke(dp(1), 0xFF8747D5);
        return d;
    }

    private GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        d.setStroke(dp(1), 0xFF7440B5);
        return d;
    }

    private TextView text(
        String value, int size, int color
    ) {
        TextView t = new TextView(activity);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        return t;
    }

    private void show() {
        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(false);
        scroll.setClipToPadding(false);

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(22), dp(18), dp(24));
        root.setBackgroundColor(BG);

        scroll.addView(root);

        // 使用主页面的全局底部导航
        ((MainActivity) activity).showPageWithBottomNav(
            scroll, 2
        );

        // 顶部品牌区域
        LinearLayout header = new LinearLayout(activity);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(header);

        LinearLayout brand = new LinearLayout(activity);
        brand.setOrientation(LinearLayout.VERTICAL);
        header.addView(
            brand,
            new LinearLayout.LayoutParams(0, -2, 1)
        );

        TextView logo = text("🌙  星月游戏盒", 25, WHITE);
        logo.setTypeface(null, Typeface.BOLD);
        brand.addView(logo);

        TextView slogan = text(
            "发现更多好游戏  ✨", 14, MUTED
        );
        LinearLayout.LayoutParams sloganParams =
            new LinearLayout.LayoutParams(-2, -2);
        sloganParams.topMargin = dp(5);
        brand.addView(slogan, sloganParams);

        // 用户信息大卡片
        LinearLayout userCard = new LinearLayout(activity);
        userCard.setOrientation(LinearLayout.VERTICAL);
        userCard.setPadding(dp(17), dp(18), dp(17), dp(14));
        userCard.setBackground(gradient(
            0xFF421B85, 0xFF211044, 22
        ));

        LinearLayout.LayoutParams userParams =
            new LinearLayout.LayoutParams(-1, -2);
        userParams.topMargin = dp(24);
        root.addView(userCard, userParams);

        LinearLayout userTop = new LinearLayout(activity);
        userTop.setGravity(Gravity.CENTER_VERTICAL);
        userCard.addView(userTop);

        MoonAvatar avatar = new MoonAvatar(activity);
        LinearLayout.LayoutParams avatarParams =
            new LinearLayout.LayoutParams(dp(90), dp(90));
        userTop.addView(avatar, avatarParams);

        LinearLayout userInfo = new LinearLayout(activity);
        userInfo.setOrientation(LinearLayout.VERTICAL);
        userInfo.setPadding(dp(14), 0, 0, 0);
        userTop.addView(
            userInfo,
            new LinearLayout.LayoutParams(0, -2, 1)
        );

        LinearLayout nameRow = new LinearLayout(activity);
        nameRow.setGravity(Gravity.CENTER_VERTICAL);
        userInfo.addView(nameRow);

        nicknameView = text(nickname, 21, WHITE);
        nicknameView.setTypeface(null, Typeface.BOLD);
        nameRow.addView(nicknameView);

        TextView vip = text("  👑普通会员", 13, 0xFFFFD77D);
        nameRow.addView(vip);

        TextView email = text(
            auth.getEmail(), 14, 0xFFE0C9FF
        );
        LinearLayout.LayoutParams emailParams =
            new LinearLayout.LayoutParams(-1, -2);
        emailParams.topMargin = dp(10);
        userInfo.addView(email, emailParams);

        TextView arrow = text("›", 34, 0xFFE6C8FF);
        userTop.addView(arrow);

        View line = new View(activity);
        line.setBackgroundColor(0xFF9D6BDB);
        LinearLayout.LayoutParams lineParams =
            new LinearLayout.LayoutParams(-1, dp(1));
        lineParams.topMargin = dp(17);
        lineParams.bottomMargin = dp(12);
        userCard.addView(line, lineParams);

        LinearLayout stats = new LinearLayout(activity);
        stats.setGravity(Gravity.CENTER);
        userCard.addView(stats);

        // 只保留我的积分
        addStat(stats, "我的积分", "1280");

        // 会员中心横幅
        LinearLayout member = new LinearLayout(activity);
        member.setGravity(Gravity.CENTER_VERTICAL);
        member.setPadding(dp(15), dp(15), dp(12), dp(15));
        member.setBackground(gradient(
            0xFF7B32D5, 0xFF39146F, 18
        ));

        LinearLayout.LayoutParams memberParams =
            new LinearLayout.LayoutParams(-1, dp(118));
        memberParams.topMargin = dp(20);
        root.addView(member, memberParams);

        TextView crown = text("👑", 43, WHITE);
        member.addView(crown);

        LinearLayout memberInfo = new LinearLayout(activity);
        memberInfo.setOrientation(LinearLayout.VERTICAL);
        memberInfo.setPadding(dp(10), 0, 0, 0);
        member.addView(
            memberInfo,
            new LinearLayout.LayoutParams(0, -2, 1)
        );

        TextView memberTitle = text(
            "会员中心", 21, WHITE
        );
        memberTitle.setTypeface(null, Typeface.BOLD);
        memberInfo.addView(memberTitle);

        TextView memberDesc = text(
            "解锁更多特权，畅享专属福利", 12, 0xFFEBD9FF
        );
        LinearLayout.LayoutParams descParams =
            new LinearLayout.LayoutParams(-1, -2);
        descParams.topMargin = dp(7);
        memberInfo.addView(memberDesc, descParams);

        TextView gift = text("🎁", 37, WHITE);
        member.addView(gift);

        TextView viewMember = text(
            "立即查看 ›", 13, 0xFF7134A5
        );
        viewMember.setGravity(Gravity.CENTER);
        viewMember.setPadding(dp(10), dp(9), dp(10), dp(9));
        viewMember.setBackground(shape(0xFFFFD6B0, 24));
        member.addView(viewMember);

        member.setOnClickListener(v ->
            showMessage("会员中心",
                "会员功能正在开发中，敬请期待。")
        );

        // 两列功能区
        LinearLayout row1 = createMenuRow(root);
        addMenu(row1, "📅", "每日签到",
            "签到领积分", () ->
                showMessage("每日签到",
                    "签到功能正在开发中，敬请期待。"));

        addMenu(row1, "👤", "我的资料",
            "编辑个人信息", () ->
                UserProfilePage.open(
                    activity, auth, () -> show()
                ));

        LinearLayout row2 = createMenuRow(root);
        addMenu(row2, "🔔", "消息通知",
            "查看系统消息", () ->
                showMessage("消息通知",
                    "消息功能正在开发中，敬请期待。"));

        // 保留功能区里的我的收藏
        addMenu(row2, "⭐", "我的收藏",
            "管理收藏游戏", () ->
                showMessage("我的收藏",
                    "收藏功能正在开发中，敬请期待。"));

        // 退出登录
        TextView logout = text(
            "⇥  退出登录", 19, 0xFFFF8EBB
        );
        logout.setTypeface(null, Typeface.BOLD);
        logout.setGravity(Gravity.CENTER);
        logout.setBackground(gradient(
            0xFF3D1A70, 0xFF241044, 18
        ));

        LinearLayout.LayoutParams logoutParams =
            new LinearLayout.LayoutParams(-1, dp(66));
        logoutParams.topMargin = dp(22);
        root.addView(logout, logoutParams);

        logout.setOnClickListener(v -> confirmLogout());

        loadProfile();
    }

    private LinearLayout createMenuRow(LinearLayout root) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout.LayoutParams params =
            new LinearLayout.LayoutParams(-1, dp(112));
        params.topMargin = dp(15);
        root.addView(row, params);

        return row;
    }

    private void addMenu(
        LinearLayout row,
        String icon,
        String title,
        String subtitle,
        Runnable action
    ) {
        LinearLayout card = new LinearLayout(activity);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setPadding(dp(12), dp(10), dp(7), dp(10));
        card.setBackground(gradient(
            0xFF4A237F, 0xFF29134D, 17
        ));

        LinearLayout.LayoutParams params =
            new LinearLayout.LayoutParams(0, -1, 1);
        params.setMargins(dp(3), 0, dp(3), 0);
        row.addView(card, params);

        TextView iconView = text(icon, 31, WHITE);
        card.addView(iconView);

        LinearLayout words = new LinearLayout(activity);
        words.setOrientation(LinearLayout.VERTICAL);
        words.setPadding(dp(7), 0, 0, 0);
        card.addView(
            words,
            new LinearLayout.LayoutParams(0, -2, 1)
        );

        TextView heading = text(title, 16, WHITE);
        heading.setTypeface(null, Typeface.BOLD);
        words.addView(heading);

        TextView desc = text(subtitle, 12, MUTED);
        LinearLayout.LayoutParams descParams =
            new LinearLayout.LayoutParams(-1, -2);
        descParams.topMargin = dp(6);
        words.addView(desc, descParams);

        card.setOnClickListener(v -> action.run());
    }

    private void addStat(
        LinearLayout parent,
        String name,
        String number
    ) {
        LinearLayout item = new LinearLayout(activity);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        parent.addView(
            item,
            new LinearLayout.LayoutParams(0, -2, 1)
        );

        TextView label = text(name, 15, 0xFFE8D7FF);
        item.addView(label);

        TextView value = text(number, 25, WHITE);
        value.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams valueParams =
            new LinearLayout.LayoutParams(-2, -2);
        valueParams.topMargin = dp(3);
        item.addView(value, valueParams);
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
                    + "&select=nickname"
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

                    activity.runOnUiThread(() -> {
                        nickname = newName;
                        nicknameView.setText(nickname);
                    });
                }

            } catch (Exception e) {
                // 读取失败时保留默认昵称
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

    // 绘制紫色星空圆环与金色月亮默认头像
    private class MoonAvatar extends View {
        private final Paint paint = new Paint(
            Paint.ANTI_ALIAS_FLAG
        );

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

            paint.setShader(new LinearGradient(
                0, 0, w, h,
                0xFF35105F, 0xFF170A35,
                Shader.TileMode.CLAMP
            ));
            canvas.drawCircle(cx, cy, r, paint);
            paint.setShader(null);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(3));
            paint.setColor(0xFFDDAAFF);
            canvas.drawCircle(cx, cy, r - dp(2), paint);
            paint.setStyle(Paint.Style.FILL);

            paint.setColor(0xFFFFD66E);
            canvas.drawCircle(
                cx - r * 0.05f, cy,
                r * 0.53f, paint
            );

            paint.setColor(0xFF35105F);
            canvas.drawCircle(
                cx + r * 0.23f, cy - r * 0.19f,
                r * 0.46f, paint
            );

            paint.setColor(0xFFFFE28A);
            float sx = cx + r * 0.43f;
            float sy = cy - r * 0.30f;
            PathStar(canvas, sx, sy, r * 0.17f);

            paint.setColor(0xFFFFE9B0);
            canvas.drawCircle(
                cx - r * 0.48f, cy - r * 0.48f,
                dp(1.5f), paint
            );
        }

        private void PathStar(
            Canvas canvas, float x, float y, float size
        ) {
            android.graphics.Path path =
                new android.graphics.Path();
            path.moveTo(x, y - size);
            path.lineTo(x + size * 0.25f, y - size * 0.25f);
            path.lineTo(x + size, y);
            path.lineTo(x + size * 0.25f, y + size * 0.25f);
            path.lineTo(x, y + size);
            path.lineTo(x - size * 0.25f, y + size * 0.25f);
            path.lineTo(x - size, y);
            path.lineTo(x - size * 0.25f, y - size * 0.25f);
            path.close();
            canvas.drawPath(path, paint);
        }
    }
}

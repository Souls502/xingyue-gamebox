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
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
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
    private TextView pointsView;

    private String nickname = "星月玩家";
    private boolean checkinInProgress = false;

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

        // 显示云端真实积分
        addStat(stats, "我的积分", "0");

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
            "签到领积分", () -> claimDailyCheckin());

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

        addMenu(row2, "⭐", "我的收藏",
            "管理收藏游戏", () -> showFavorites());

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


    // 打开当前登录用户的云端收藏列表
    private void showFavorites() {
        MainActivity main = (MainActivity) activity;

        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(22), dp(18), dp(24));
        root.setBackgroundColor(BG);
        scroll.addView(root);

        LinearLayout header = new LinearLayout(activity);
        header.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(header);

        TextView back = text("‹ 返回", 17, 0xFFE6C8FF);
        back.setPadding(dp(4), dp(8), dp(16), dp(8));
        header.addView(back);
        back.setOnClickListener(v -> show());

        TextView title = text("我的收藏", 24, WHITE);
        title.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams titleParams =
            new LinearLayout.LayoutParams(0, -2, 1);
        header.addView(title, titleParams);

        TextView subtitle = text(
            "收藏的游戏会保存在你的账号中", 13, MUTED
        );
        LinearLayout.LayoutParams subtitleParams =
            new LinearLayout.LayoutParams(-1, -2);
        subtitleParams.topMargin = dp(10);
        root.addView(subtitle, subtitleParams);

        LinearLayout items = new LinearLayout(activity);
        items.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams itemsParams =
            new LinearLayout.LayoutParams(-1, -2);
        itemsParams.topMargin = dp(18);
        root.addView(items, itemsParams);

        main.showPageWithBottomNav(scroll, 2);
        loadFavorites(items);
    }

    // 从 Supabase 读取收藏记录及对应游戏资料
    private void loadFavorites(LinearLayout items) {
        items.removeAllViews();

        TextView loading = text("正在加载收藏游戏……", 15, MUTED);
        loading.setGravity(Gravity.CENTER);
        loading.setPadding(0, dp(35), 0, dp(35));
        items.addView(loading);

        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                String userId = auth.getUserId();
                String token = auth.getValidAccessToken();

                if (userId == null || userId.trim().isEmpty()
                    || token == null || token.trim().isEmpty()) {
                    activity.runOnUiThread(() -> {
                        items.removeAllViews();
                        showFavoritesMessage(items, "请先登录账号，再查看收藏游戏。");
                    });
                    return;
                }

                String query = BASE
                    + "/rest/v1/user_favorites"
                    + "?select=game_id,games(id,title,description,category,version,file_size,cover_url,download_url)"
                    + "&user_id=eq." + Uri.encode(userId)
                    + "&order=created_at.desc";

                URL url = new URL(query);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);
                conn.setRequestProperty("apikey", API_KEY);
                conn.setRequestProperty(
                    "Authorization", "Bearer " + token
                );
                conn.setRequestProperty("Accept", "application/json");

                int code = conn.getResponseCode();
                InputStream stream = code >= 200 && code < 300
                    ? conn.getInputStream() : conn.getErrorStream();
                String response = stream == null ? "" : readStream(stream);

                if (code < 200 || code >= 300) {
                    final int errorCode = code;
                    activity.runOnUiThread(() -> {
                        items.removeAllViews();
                        showFavoritesMessage(
                            items, "读取收藏失败，请稍后重试。\n错误代码：" + errorCode
                        );
                    });
                    return;
                }

                JSONArray rows = new JSONArray(response);
                java.util.ArrayList<MainActivity.Game> result =
                    new java.util.ArrayList<>();

                for (int i = 0; i < rows.length(); i++) {
                    JSONObject row = rows.optJSONObject(i);
                    if (row == null) continue;

                    JSONObject gameObject = row.optJSONObject("games");
                    if (gameObject == null) {
                        JSONArray gameArray = row.optJSONArray("games");
                        if (gameArray != null && gameArray.length() > 0) {
                            gameObject = gameArray.optJSONObject(0);
                        }
                    }
                    if (gameObject == null) continue;

                    MainActivity.Game game = new MainActivity.Game();
                    game.id = gameObject.optLong(
                        "id", row.optLong("game_id", -1)
                    );
                    game.name = gameObject.optString("title", "未命名游戏");
                    game.description = gameObject.optString("description", "");
                    game.type = gameObject.optString("category", "");
                    game.version = gameObject.optString("version", "");
                    game.fileSize = gameObject.optString("file_size", "");
                    game.coverUrl = gameObject.optString("cover_url", "");
                    game.downloadUrl = gameObject.optString("download_url", "");

                    if (game.id > 0) result.add(game);
                }

                activity.runOnUiThread(() -> {
                    items.removeAllViews();

                    if (result.isEmpty()) {
                        showFavoritesMessage(
                            items,
                            "还没有收藏游戏。\n去游戏首页发现喜欢的游戏吧！"
                        );
                        return;
                    }

                    MainActivity main = (MainActivity) activity;
                    for (MainActivity.Game game : result) {
                        addFavoriteItem(items, game, main);
                    }
                });

            } catch (Exception e) {
                activity.runOnUiThread(() -> {
                    items.removeAllViews();
                    showFavoritesMessage(
                        items, "暂时无法连接收藏服务，请检查网络后重试。"
                    );
                });
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    private void showFavoritesMessage(LinearLayout items, String message) {
        TextView empty = text(message, 15, MUTED);
        empty.setGravity(Gravity.CENTER);
        empty.setPadding(dp(18), dp(38), dp(18), dp(38));
        empty.setBackground(shape(CARD, 16));
        items.addView(
            empty,
            new LinearLayout.LayoutParams(-1, -2)
        );
    }

    // 收藏游戏卡片：点击卡片进入详情，点击按钮取消收藏
    private void addFavoriteItem(
        LinearLayout parent,
        MainActivity.Game game,
        MainActivity main
    ) {
        LinearLayout card = new LinearLayout(activity);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setPadding(dp(10), dp(10), dp(10), dp(10));
        card.setBackground(shape(CARD, 16));

        LinearLayout.LayoutParams cardParams =
            new LinearLayout.LayoutParams(-1, -2);
        cardParams.bottomMargin = dp(12);
        parent.addView(card, cardParams);

        FrameLayout cover = main.createCover(
            "🎮", game.coverUrl, main.dp(76), main.dp(76)
        );
        card.addView(
            cover,
            new LinearLayout.LayoutParams(dp(76), dp(76))
        );

        LinearLayout info = new LinearLayout(activity);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(dp(12), 0, dp(5), 0);
        card.addView(
            info,
            new LinearLayout.LayoutParams(0, -2, 1)
        );

        TextView name = text(game.name, 16, WHITE);
        name.setTypeface(null, Typeface.BOLD);
        info.addView(name);

        String category = game.type == null || game.type.trim().isEmpty()
            ? "游戏" : game.type;
        TextView type = text(category, 12, MUTED);
        LinearLayout.LayoutParams typeParams =
            new LinearLayout.LayoutParams(-1, -2);
        typeParams.topMargin = dp(7);
        info.addView(type, typeParams);

        TextView open = text("查看详情 ›", 12, 0xFFFFD77D);
        LinearLayout.LayoutParams openParams =
            new LinearLayout.LayoutParams(-1, -2);
        openParams.topMargin = dp(7);
        info.addView(open, openParams);

        TextView remove = text("取消收藏", 12, 0xFFFFA6C8);
        remove.setGravity(Gravity.CENTER);
        remove.setPadding(dp(9), dp(9), dp(9), dp(9));
        remove.setBackground(shape(0xFF49204F, 18));
        card.addView(remove);

        View.OnClickListener detailClick = v -> {
            int index = -1;
            for (int i = 0; i < main.games.size(); i++) {
                if (main.games.get(i).id == game.id) {
                    index = i;
                    break;
                }
            }

            if (index >= 0) {
                main.showDetail(index);
            } else {
                showMessage(
                    "暂时无法打开",
                    "这款游戏目前不在游戏列表中，请返回首页刷新后再试。"
                );
            }
        };

        cover.setOnClickListener(detailClick);
        info.setOnClickListener(detailClick);
        name.setOnClickListener(detailClick);
        open.setOnClickListener(detailClick);

        remove.setOnClickListener(v ->
            removeFavorite(game, parent, main)
        );
    }

    // 从云端删除当前用户的指定收藏
    private void removeFavorite(
        MainActivity.Game game,
        LinearLayout items,
        MainActivity main
    ) {
        new AlertDialog.Builder(activity)
            .setTitle("取消收藏")
            .setMessage("确定要取消收藏《" + game.name + "》吗？")
            .setNegativeButton("再想想", null)
            .setPositiveButton("确定", (dialog, which) -> {
                new Thread(() -> {
                    HttpURLConnection conn = null;
                    try {
                        String userId = auth.getUserId();
                        String token = auth.getValidAccessToken();

                        String query = BASE
                            + "/rest/v1/user_favorites"
                            + "?user_id=eq." + Uri.encode(userId)
                            + "&game_id=eq." + game.id;

                        URL url = new URL(query);
                        conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("DELETE");
                        conn.setConnectTimeout(15000);
                        conn.setReadTimeout(15000);
                        conn.setRequestProperty("apikey", API_KEY);
                        conn.setRequestProperty(
                            "Authorization", "Bearer " + token
                        );
                        conn.setRequestProperty("Accept", "application/json");

                        int code = conn.getResponseCode();
                        if (code >= 200 && code < 300) {
                            activity.runOnUiThread(() -> {
                                Toast.makeText(
                                    activity, "已取消收藏",
                                    Toast.LENGTH_SHORT
                                ).show();
                                loadFavorites(items);
                            });
                        } else {
                            activity.runOnUiThread(() ->
                                showMessage(
                                    "取消失败",
                                    "暂时无法取消收藏，请稍后重试。\n错误代码：" + code
                                )
                            );
                        }
                    } catch (Exception e) {
                        activity.runOnUiThread(() ->
                            showMessage(
                                "网络连接失败",
                                "暂时无法连接收藏服务，请检查网络后重试。"
                            )
                        );
                    } finally {
                        if (conn != null) conn.disconnect();
                    }
                }).start();
            })
            .show();
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

        if ("我的积分".equals(name)) {
            pointsView = value;
        }

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

    // 从云端读取昵称和真实积分
    private void loadProfile() {
        new Thread(() -> {
            HttpURLConnection conn = null;

            try {
                String token = auth.getValidAccessToken();

                URL url = new URL(
                    BASE + "/rest/v1/profiles?id=eq."
                    + auth.getUserId()
                    + "&select=nickname,points"
                );

                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(12000);
                conn.setReadTimeout(12000);
                conn.setRequestProperty("apikey", API_KEY);
                conn.setRequestProperty(
                    "Authorization",
                    "Bearer " + token
                );

                int code = conn.getResponseCode();
                if (code != 200) return;

                String result = readStream(conn.getInputStream());
                JSONArray rows = new JSONArray(result);

                if (rows.length() > 0) {
                    JSONObject obj = rows.getJSONObject(0);

                    String newName = obj.optString(
                        "nickname", "星月玩家"
                    );

                    int newPoints = obj.optInt("points", 0);

                    activity.runOnUiThread(() -> {
                        nickname = newName;
                        nicknameView.setText(nickname);
                        pointsView.setText(
                            String.valueOf(newPoints)
                        );
                    });
                }

            } catch (Exception e) {
                // 读取失败时保留默认显示
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    // 调用 Supabase 云端每日签到功能
    private void claimDailyCheckin() {

        if (checkinInProgress) {
            return;
        }

        String userId = auth.getUserId();

        if (userId == null || userId.trim().isEmpty()) {
            showMessage(
                "暂时无法签到",
                "请先登录账号，再进行每日签到。"
            );
            return;
        }

        checkinInProgress = true;

        new Thread(() -> {
            HttpURLConnection conn = null;

            try {
                // 在后台线程获取有效令牌，必要时自动刷新
                String token = auth.getValidAccessToken();

                if (token == null || token.trim().isEmpty()) {
                    throw new Exception("登录状态已失效");
                }

                URL url = new URL(
                    BASE + "/rest/v1/rpc/claim_daily_checkin"
                );

                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);
                conn.setDoOutput(true);

                conn.setRequestProperty("apikey", API_KEY);
                conn.setRequestProperty(
                    "Authorization", "Bearer " + token
                );
                conn.setRequestProperty(
                    "Content-Type", "application/json"
                );
                conn.setRequestProperty(
                    "Accept", "application/json"
                );

                byte[] body = "{}".getBytes("UTF-8");

                try (OutputStream output = conn.getOutputStream()) {
                    output.write(body);
                    output.flush();
                }

                int code = conn.getResponseCode();

                InputStream responseStream =
                    code >= 200 && code < 300
                    ? conn.getInputStream()
                    : conn.getErrorStream();

                String responseText = responseStream == null
                    ? ""
                    : readStream(responseStream);

                if (code >= 200 && code < 300) {

                    JSONObject result =
                        new JSONObject(responseText);

                    boolean success =
                        result.optBoolean("success", false);

                    boolean alreadyCheckedIn =
                        result.optBoolean(
                            "already_checked_in", false
                        );

                    int newPoints =
                        result.optInt("points", -1);

                    activity.runOnUiThread(() -> {

                        if (newPoints >= 0 && pointsView != null) {
                            pointsView.setText(
                                String.valueOf(newPoints)
                            );
                        }

                        if (alreadyCheckedIn) {
                            showMessage(
                                "今日已签到",
                                "你今天已经签到过了，明天再来吧！"
                            );
                        } else if (success) {
                            showMessage(
                                "签到成功",
                                "恭喜获得 2 积分！\n当前积分："
                                + (newPoints >= 0
                                    ? newPoints : "已更新")
                            );
                        } else {
                            showMessage(
                                "签到未完成",
                                "暂时无法确认签到结果，请稍后重试。"
                            );
                        }
                    });

                } else {
                    String errorMessage =
                        "签到请求失败，请稍后重试。";

                    activity.runOnUiThread(() ->
                        showMessage(
                            "签到失败",
                            errorMessage + "\n错误代码：" + code
                        )
                    );
                }

            } catch (Exception e) {
                activity.runOnUiThread(() ->
                    showMessage(
                        "网络连接失败",
                        "暂时无法连接签到服务，请检查网络后重试。"
                    )
                );

            } finally {
                if (conn != null) {
                    conn.disconnect();
                }

                activity.runOnUiThread(() ->
                    checkinInProgress = false
                );
            }
        }).start();
    }

    private String readStream(InputStream input)
        throws Exception {

        BufferedReader reader = new BufferedReader(
            new InputStreamReader(input, "UTF-8")
        );

        StringBuilder result = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            result.append(line);
        }

        reader.close();
        return result.toString();
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

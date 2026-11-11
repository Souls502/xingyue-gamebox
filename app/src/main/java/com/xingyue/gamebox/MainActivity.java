package com.xingyue.gamebox;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.MotionEvent;
import android.widget.*;
import android.text.TextWatcher;
import android.text.Editable;
import android.util.TypedValue;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;

public class MainActivity extends Activity {

    LinearLayout list;
    LinearLayout categoryBar;
    EditText search;
    TextView status;
    String category = "全部";
    boolean loading = true;
    boolean refreshingByGesture = false;

    ArrayList<Game> games = new ArrayList<>();

    final String SUPABASE_URL =
        "https://vavsdizrdsvavioxsmjh.supabase.co";

    final String SUPABASE_KEY =
        "sb_publishable_r1RfRiRkwcDVOoOtO-Vj8w_sQAs2VIk";

    int purple = Color.rgb(19,14,35);

    // 支持下拉刷新的主页面滚动容器
    class PullToRefreshScrollView extends ScrollView {
        float downY;

        PullToRefreshScrollView() {
            super(MainActivity.this);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                downY = event.getY();
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                float distance = event.getY() - downY;

                if (getScrollY() == 0 &&
                    distance > dp(90) &&
                    !loading) {

                    refreshingByGesture = true;
                    loadGames();
                }
            }

            return super.onTouchEvent(event);
        }
    }

    static class Game {
        String name = "";
        String description = "";
        String type = "其他";
        String version = "";
        String fileSize = "";
        String coverUrl = "";
        String downloadUrl = "";
    }

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(purple);
        getWindow().setNavigationBarColor(purple);
        home();
        loadGames();

        // 启动时检查 APP 更新
        checkForUpdate();
    }

    // 检查最新版本
    void checkForUpdate() {
        new Thread(() -> {
            HttpURLConnection conn = null;

            try {
                URL url = new URL(
                    SUPABASE_URL +
                    "/rest/v1/app_releases?select=version,version_code,release_notes,apk_url&is_latest=eq.true&order=created_at.desc&limit=1"
                );

                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);
                conn.setRequestProperty("apikey", SUPABASE_KEY);
                conn.setRequestProperty("Accept", "application/json");

                int responseCode = conn.getResponseCode();

                if (responseCode != 200) {
                    return;
                }

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

                JSONArray array =
                    new JSONArray(result.toString());

                if (array.length() == 0) {
                    return;
                }

                JSONObject release = array.getJSONObject(0);

                String version =
                    release.optString("version", "");

                int serverCode =
                    release.optInt("version_code", 0);

                String notes =
                    release.optString("release_notes", "");

                String apkUrl =
                    release.optString("apk_url", "");

                int currentCode =
                    getPackageManager()
                    .getPackageInfo(getPackageName(), 0)
                    .versionCode;

                if (serverCode > currentCode &&
                    !apkUrl.isEmpty()) {

                    runOnUiThread(() -> {
                        showUpdateDialog(
                            version,
                            notes,
                            apkUrl
                        );
                    });
                }

            } catch (Exception e) {
                // 检查失败时不影响正常使用
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }).start();
    }

    // 显示更新提示
    void showUpdateDialog(
        String version,
        String notes,
        String apkUrl
    ) {
        if (isFinishing()) return;

        if (notes == null || notes.trim().isEmpty()) {
            notes = "优化功能，修复已知问题";
        }

        new AlertDialog.Builder(this)
            .setTitle("✦ 发现新版本 " + version)
            .setMessage(
                "星月游戏盒有新版本可以更新！\n\n" +
                "更新内容：\n" + notes +
                "\n\n是否立即下载更新？"
            )
            .setPositiveButton("立即更新", (dialog, which) -> {
                try {
                    Intent intent = new Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(apkUrl)
                    );
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(
                        this,
                        "无法打开下载链接",
                        Toast.LENGTH_SHORT
                    ).show();
                }
            })
            .setNegativeButton("稍后再说", null)
            .show();
    }

    GradientDrawable shape(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(22);
        return d;
    }

    TextView label(String s,int size,int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);
        return t;
    }

    int dp(float value) {
        return (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value,
            getResources().getDisplayMetrics()
        );
    }

    // 在线加载封面图片
    void loadCover(ImageView image, String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return;
        }

        image.setTag(imageUrl);

        new Thread(() -> {
            HttpURLConnection conn = null;

            try {
                URL url = new URL(imageUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(12000);
                conn.setReadTimeout(12000);
                conn.setRequestMethod("GET");

                try (InputStream stream = conn.getInputStream()) {
                    Bitmap bitmap = BitmapFactory.decodeStream(stream);

                    if (bitmap != null) {
                        runOnUiThread(() -> {
                            if (imageUrl.equals(image.getTag())) {
                                image.setImageBitmap(bitmap);
                            }
                        });
                    }
                }

            } catch (Exception e) {
                // 图片加载失败时保留默认游戏图标
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }).start();
    }

    // 创建带默认图标的封面
    FrameLayout createCover(
        String iconText,
        String coverUrl,
        int width,
        int height
    ) {
        FrameLayout frame = new FrameLayout(this);
        frame.setBackground(shape(0xFF49346C));

        ImageView image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);

        FrameLayout.LayoutParams imageParams =
            new FrameLayout.LayoutParams(-1,-1);

        frame.addView(image,imageParams);

        TextView fallback = label(
            iconText,
            35,
            Color.WHITE
        );
        fallback.setGravity(Gravity.CENTER);

        FrameLayout.LayoutParams fallbackParams =
            new FrameLayout.LayoutParams(-1,-1);

        frame.addView(fallback,fallbackParams);

        if (coverUrl != null && !coverUrl.isEmpty()) {
            image.setTag(coverUrl);

            new Thread(() -> {
                HttpURLConnection conn = null;

                try {
                    URL url = new URL(coverUrl);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(12000);
                    conn.setReadTimeout(12000);

                    try (InputStream stream = conn.getInputStream()) {
                        Bitmap bitmap =
                            BitmapFactory.decodeStream(stream);

                        if (bitmap != null) {
                            runOnUiThread(() -> {
                                if (coverUrl.equals(image.getTag())) {
                                    image.setImageBitmap(bitmap);
                                    fallback.setVisibility(View.GONE);
                                }
                            });
                        }
                    }

                } catch (Exception e) {
                    // 保留默认图标
                } finally {
                    if (conn != null) {
                        conn.disconnect();
                    }
                }
            }).start();
        }

        frame.setLayoutParams(
            new LinearLayout.LayoutParams(width,height)
        );

        return frame;
    }

    void loadGames() {
        loading = true;
        render();

        new Thread(() -> {
            HttpURLConnection conn = null;

            try {
                URL url = new URL(
                    SUPABASE_URL +
                    "/rest/v1/games?select=id,title,description,category,version,file_size,cover_url,download_url,is_active,sort_order&is_active=eq.true&order=sort_order.asc"
                );

                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);
                conn.setRequestProperty("apikey", SUPABASE_KEY);
                conn.setRequestProperty("Accept", "application/json");

                int responseCode = conn.getResponseCode();

                if (responseCode != 200) {
                    throw new Exception("服务器返回：" + responseCode);
                }

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

                JSONArray array = new JSONArray(result.toString());
                ArrayList<Game> newGames = new ArrayList<>();

                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);

                    Game game = new Game();
                    game.name = obj.optString("title", "");
                    game.description = obj.optString("description", "");
                    game.type = obj.optString("category", "其他");
                    game.version = obj.optString("version", "");
                    game.fileSize = obj.optString("file_size", "");
                    game.coverUrl = obj.optString("cover_url", "");
                    game.downloadUrl = obj.optString("download_url", "");

                    if (!game.name.isEmpty()) {
                        newGames.add(game);
                    }
                }

                runOnUiThread(() -> {
                    games.clear();
                    games.addAll(newGames);
                    loading = false;
                    updateCategories();
                    render();

                    if (refreshingByGesture) {
                        refreshingByGesture = false;
                        Toast.makeText(
                            this,
                            "游戏列表已更新",
                            Toast.LENGTH_SHORT
                        ).show();
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    loading = false;
                    render();
                    refreshingByGesture = false;
                    Toast.makeText(
                        this,
                        "游戏加载失败，请检查网络或后台设置",
                        Toast.LENGTH_LONG
                    ).show();
                });

            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }).start();
    }

    void home() {
        PullToRefreshScrollView scroll =
            new PullToRefreshScrollView();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(1);
        root.setPadding(20,24,20,30);
        root.setBackgroundColor(purple);

        scroll.addView(root);
        setContentView(scroll);

        TextView title = label("✦ 星月游戏盒",27,Color.WHITE);
        title.setTypeface(null,Typeface.BOLD);
        root.addView(title);

        TextView sub = label(
            "发现好玩的游戏 · 开启星月之旅",
            14,0xFFB9AED5
        );

        LinearLayout.LayoutParams sp =
            new LinearLayout.LayoutParams(-1,-2);
        sp.topMargin=8;
        sp.bottomMargin=22;
        root.addView(sub,sp);

        TextView refreshHint = label(
            "↓ 下拉页面即可刷新游戏列表",
            12,0xFFB9AED5
        );

        LinearLayout.LayoutParams refreshHintParams =
            new LinearLayout.LayoutParams(-1,-2);
        refreshHintParams.bottomMargin = 16;
        root.addView(refreshHint,refreshHintParams);

        search = new EditText(this);
        search.setSingleLine(true);
        search.setHint("搜索你喜欢的游戏");
        search.setHintTextColor(0xFFB9AED5);
        search.setTextColor(Color.WHITE);
        search.setPadding(18,10,18,10);
        search.setBackground(shape(0xFF302546));
        root.addView(search);

        TextView heading = label("游戏分类",21,Color.WHITE);
        heading.setTypeface(null,Typeface.BOLD);

        LinearLayout.LayoutParams hp =
            new LinearLayout.LayoutParams(-1,-2);
        hp.topMargin=25;
        hp.bottomMargin=12;
        root.addView(heading,hp);

        categoryBar = new LinearLayout(this);
        categoryBar.setOrientation(0);

        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        hs.addView(categoryBar);
        root.addView(hs);

        TextView gamesTitle = label("热门游戏",21,Color.WHITE);
        gamesTitle.setTypeface(null,Typeface.BOLD);

        LinearLayout.LayoutParams gp =
            new LinearLayout.LayoutParams(-1,-2);
        gp.topMargin=25;
        gp.bottomMargin=15;
        root.addView(gamesTitle,gp);

        status = label("正在加载游戏...",14,0xFFB9AED5);
        root.addView(status);

        list = new LinearLayout(this);
        list.setOrientation(1);
        root.addView(list);

        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(
                CharSequence s,int st,int c,int a) {}

            public void onTextChanged(
                CharSequence s,int st,int before,int count) {
                render();
            }

            public void afterTextChanged(Editable e) {}
        });

        updateCategories();
        render();
    }

    void updateCategories() {
        if (categoryBar == null) return;

        categoryBar.removeAllViews();

        LinkedHashSet<String> categories = new LinkedHashSet<>();
        categories.add("全部");

        for (Game game : games) {
            if (!game.type.isEmpty()) {
                categories.add(game.type);
            }
        }

        for (String c : categories) {
            TextView btn = label(c,12,Color.WHITE);
            btn.setGravity(Gravity.CENTER);
            btn.setPadding(12,12,12,12);
            btn.setBackground(shape(
                category.equals(c) ? 0xFF7545B5 : 0xFF49346C
            ));

            LinearLayout.LayoutParams p =
                new LinearLayout.LayoutParams(-2,-2);
            p.rightMargin=7;
            categoryBar.addView(btn,p);

            btn.setOnClickListener(v -> {
                category=c;
                updateCategories();
                render();
            });
        }
    }

    String gameIcon(String type) {
        if (type.contains("动作")) return "⚔️";
        if (type.contains("角色")) return "🛡️";
        if (type.contains("休闲")) return "🎮";
        if (type.contains("竞速")) return "🏎️";
        if (type.contains("冒险")) return "🚀";
        return "🎮";
    }

    void render() {
        if (list == null) return;

        list.removeAllViews();

        if (loading) {
            status.setText(
                refreshingByGesture ? "正在刷新游戏..." : "正在加载游戏..."
            );
            return;
        }

        String key = search.getText().toString()
            .trim().toLowerCase();

        int count = 0;

        for (int i=0;i<games.size();i++) {
            Game game = games.get(i);

            if (!category.equals("全部") &&
                !category.equals(game.type)) continue;

            if (!game.name.toLowerCase().contains(key))
                continue;

            count++;

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(0);
            card.setGravity(Gravity.CENTER_VERTICAL);
            card.setPadding(15,18,15,18);
            card.setBackground(shape(0xFF302546));

            LinearLayout.LayoutParams cp =
                new LinearLayout.LayoutParams(-1,-2);
            cp.bottomMargin=12;
            list.addView(card,cp);

            FrameLayout cover = createCover(
                gameIcon(game.type),
                game.coverUrl,
                dp(82),
                dp(82)
            );

            card.addView(cover);

            LinearLayout info = new LinearLayout(this);
            info.setOrientation(1);
            info.setPadding(15,0,0,0);

            card.addView(info,
                new LinearLayout.LayoutParams(0,-2,1));

            TextView name = label(game.name,17,Color.WHITE);
            name.setTypeface(null,Typeface.BOLD);
            info.addView(name);

            TextView type = label(
                game.type,13,0xFFB9AED5
            );

            LinearLayout.LayoutParams tp =
                new LinearLayout.LayoutParams(-1,-2);
            tp.topMargin=5;
            info.addView(type,tp);

            TextView detail = label(
                "查看详情 ›",13,0xFFD5A7FF
            );
            card.addView(detail);

            final int gameIndex = i;

            card.setOnClickListener(v -> {
                showDetail(gameIndex);
            });
        }

        if (count == 0) {
            status.setText(
                games.isEmpty()
                ? "后台暂时没有已启用的游戏"
                : "没有找到符合条件的游戏"
            );
        } else {
            status.setText("已加载 " + count + " 款游戏");
        }
    }

    void showDetail(int index) {
        Game game = games.get(index);

        ScrollView scroll = new ScrollView(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(1);
        root.setPadding(24,30,24,30);
        root.setBackgroundColor(purple);

        scroll.addView(root);
        setContentView(scroll);

        TextView back = label(
            "‹ 返回游戏列表",16,0xFFD5A7FF
        );
        root.addView(back);

        back.setOnClickListener(v -> home());

        FrameLayout cover = createCover(
            gameIcon(game.type),
            game.coverUrl,
            -1,
            dp(210)
        );

        LinearLayout.LayoutParams coverParams =
            new LinearLayout.LayoutParams(-1,dp(210));
        coverParams.topMargin=20;
        coverParams.bottomMargin=18;
        root.addView(cover,coverParams);

        TextView title = label(
            game.name,26,Color.WHITE
        );
        title.setTypeface(null,Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView type = label(
            game.type,15,0xFFB9AED5
        );
        type.setGravity(Gravity.CENTER);
        root.addView(type);

        String description = game.description;

        if (description.isEmpty()) {
            description = "暂无游戏介绍";
        }

        String extra = "\n\n版本：" +
            (game.version.isEmpty() ? "未填写" : game.version);

        if (!game.fileSize.isEmpty()) {
            extra += "\n文件大小：" + game.fileSize;
        }

        TextView desc = label(
            description + extra,
            16,Color.WHITE
        );

        LinearLayout.LayoutParams dp =
            new LinearLayout.LayoutParams(-1,-2);
        dp.topMargin=30;
        root.addView(desc,dp);

        TextView download = label(
            "立即下载",18,Color.WHITE
        );
        download.setGravity(Gravity.CENTER);
        download.setPadding(20,16,20,16);
        download.setBackground(shape(0xFF7545B5));

        LinearLayout.LayoutParams bp =
            new LinearLayout.LayoutParams(-1,-2);
        bp.topMargin=35;
        root.addView(download,bp);

        download.setOnClickListener(v -> {
            if (!game.downloadUrl.isEmpty()) {
                try {
                    Intent intent = new Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(game.downloadUrl)
                    );
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(
                        this,
                        "无法打开下载链接",
                        Toast.LENGTH_SHORT
                    ).show();
                }
            } else {
                Toast.makeText(
                    this,
                    "暂未提供下载",
                    Toast.LENGTH_SHORT
                ).show();
            }
        });
    }
}

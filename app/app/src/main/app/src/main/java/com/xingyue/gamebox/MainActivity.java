package com.xingyue.gamebox;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.*;
import android.text.TextWatcher;
import android.text.Editable;
import java.util.*;

public class MainActivity extends Activity {

    LinearLayout list;
    EditText search;
    String category = "全部";

    String[] names = {
        "星际冒险", "像素勇者", "梦幻岛屿",
        "暗夜传说", "极速挑战", "喵星大作战"
    };

    String[] types = {
        "动作冒险", "角色扮演", "休闲益智",
        "角色扮演", "竞速游戏", "休闲益智"
    };

    String[] icons = {"🚀","⚔️","🏝️","🌙","🏎️","🐱"};

    int purple = Color.rgb(19,14,35);

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(purple);
        getWindow().setNavigationBarColor(purple);
        home();
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

    void home() {
        ScrollView scroll = new ScrollView(this);

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

        LinearLayout cats = new LinearLayout(this);
        cats.setOrientation(0);

        String[] categories = {
            "全部","动作冒险","角色扮演","休闲益智","竞速游戏"
        };

        for(String c:categories) {
            TextView btn = label(c,12,Color.WHITE);
            btn.setGravity(Gravity.CENTER);
            btn.setPadding(12,12,12,12);
            btn.setBackground(shape(0xFF49346C));

            LinearLayout.LayoutParams p =
                new LinearLayout.LayoutParams(-2,-2);
            p.rightMargin=7;
            cats.addView(btn,p);

            btn.setOnClickListener(v -> {
                category=c;
                render();
            });
        }

        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        hs.addView(cats);
        root.addView(hs);

        TextView games = label("热门游戏",21,Color.WHITE);
        games.setTypeface(null,Typeface.BOLD);

        LinearLayout.LayoutParams gp =
            new LinearLayout.LayoutParams(-1,-2);
        gp.topMargin=25;
        gp.bottomMargin=15;
        root.addView(games,gp);

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

        render();
    }

    void render() {
        list.removeAllViews();

        String key = search.getText().toString()
            .trim().toLowerCase();

        for(int i=0;i<names.length;i++) {

            if(!category.equals("全部") &&
                !category.equals(types[i])) continue;

            if(!names[i].toLowerCase().contains(key))
                continue;

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(0);
            card.setGravity(Gravity.CENTER_VERTICAL);
            card.setPadding(15,18,15,18);
            card.setBackground(shape(0xFF302546));

            LinearLayout.LayoutParams cp =
                new LinearLayout.LayoutParams(-1,-2);
            cp.bottomMargin=12;
            list.addView(card,cp);

            TextView icon = label(icons[i],35,Color.WHITE);
            card.addView(icon);

            LinearLayout info = new LinearLayout(this);
            info.setOrientation(1);
            info.setPadding(15,0,0,0);

            card.addView(info,
                new LinearLayout.LayoutParams(0,-2,1));

            TextView name = label(names[i],17,Color.WHITE);
            name.setTypeface(null,Typeface.BOLD);
            info.addView(name);

            TextView type = label(types[i],13,0xFFB9AED5);
            LinearLayout.LayoutParams tp =
                new LinearLayout.LayoutParams(-1,-2);
            tp.topMargin=5;
            info.addView(type,tp);

            TextView detail = label("查看详情 ›",13,0xFFD5A7FF);
            card.addView(detail);

            card.setOnClickListener(v -> {
                Toast.makeText(this,
                    "游戏详情："+names[i],
                    Toast.LENGTH_SHORT).show();
            });
        }
    }
  }

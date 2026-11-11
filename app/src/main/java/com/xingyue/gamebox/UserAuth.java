package com.xingyue.gamebox;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class UserAuth {
    private static final String PREFS = "xingyue_user_session";
    private static final String BASE =
        "https://vavsdizrdsvavioxsmjh.supabase.co";
    private static final String API_KEY =
        "sb_publishable_r1RfRiRkwcDVOoOtO-Vj8w_sQAs2VIk";

    private final Context context;

    public UserAuth(Context context) {
        this.context = context.getApplicationContext();
    }

    private SharedPreferences prefs() {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public boolean isLoggedIn() {
        return !prefs().getString("access_token", "").isEmpty();
    }

    public String getEmail() {
        return prefs().getString("email", "");
    }

    public String getUserId() {
        return prefs().getString("user_id", "");
    }

    public String getAccessToken() {
        return prefs().getString("access_token", "");
    }

    public void logout() {
        String token = getAccessToken();

        if (!token.isEmpty()) {
            try {
                request("/auth/v1/logout", new JSONObject(), token);
            } catch (Exception ignored) {
            }
        }

        prefs().edit().clear().apply();
    }

    public String signUp(String email, String password)
            throws Exception {
        JSONObject body = new JSONObject();
        body.put("email", email);
        body.put("password", password);

        JSONObject result = request(
            "/auth/v1/signup", body, null
        );

        if (result.has("access_token")) {
            saveSession(result, email);
            return "注册成功，已自动登录。";
        }

        return "注册申请已提交！请打开邮箱，点击 Supabase 发来的验证链接，验证后回到游戏盒登录。";
    }

    public String signIn(String email, String password)
            throws Exception {
        JSONObject body = new JSONObject();
        body.put("email", email);
        body.put("password", password);

        JSONObject result = request(
            "/auth/v1/token?grant_type=password",
            body,
            null
        );

        saveSession(result, email);
        return "登录成功！";
    }

    private void saveSession(JSONObject result, String email)
            throws Exception {
        String access = result.optString("access_token", "");
        String refresh = result.optString("refresh_token", "");
        JSONObject user = result.optJSONObject("user");

        String userId = user == null
            ? "" : user.optString("id", "");

        if (access.isEmpty()) {
            throw new Exception(
                "没有获取到登录凭证，请先验证邮箱后再登录。"
            );
        }

        prefs().edit()
            .putString("access_token", access)
            .putString("refresh_token", refresh)
            .putString(
                "email",
                user != null
                    ? user.optString("email", email)
                    : email
            )
            .putString("user_id", userId)
            .apply();
    }

    private JSONObject request(
            String path,
            JSONObject body,
            String bearer
    ) throws Exception {
        HttpURLConnection conn = null;

        try {
            URL url = new URL(BASE + path);
            conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setDoOutput(true);

            conn.setRequestProperty("apikey", API_KEY);
            conn.setRequestProperty(
                "Content-Type", "application/json"
            );
            conn.setRequestProperty(
                "Accept", "application/json"
            );

            if (bearer != null && !bearer.isEmpty()) {
                conn.setRequestProperty(
                    "Authorization", "Bearer " + bearer
                );
            }

            byte[] data = body.toString().getBytes("UTF-8");

            try (OutputStream out = conn.getOutputStream()) {
                out.write(data);
            }

            int code = conn.getResponseCode();

            InputStream stream =
                code >= 200 && code < 300
                    ? conn.getInputStream()
                    : conn.getErrorStream();

            StringBuilder response = new StringBuilder();

            if (stream != null) {
                try (BufferedReader reader =
                         new BufferedReader(
                             new InputStreamReader(
                                 stream, "UTF-8"
                             )
                         )) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                }
            }

            JSONObject json = response.length() == 0
                ? new JSONObject()
                : new JSONObject(response.toString());

            if (code < 200 || code >= 300) {
                String message = json.optString(
                    "msg",
                    json.optString(
                        "message",
                        json.optString(
                            "error_description",
                            json.optString(
                                "error",
                                "请求失败（" + code + "）"
                            )
                        )
                    )
                );

                String lower = message.toLowerCase();

                if (lower.contains("invalid login credentials")) {
                    message = "邮箱或密码不正确。";
                } else if (lower.contains("email not confirmed")) {
                    message = "邮箱还没有验证，请先到邮箱点击验证链接。";
                } else if (lower.contains("user already registered")) {
                    message = "这个邮箱已经注册，请直接登录。";
                } else if (lower.contains("password")) {
                    message = "密码不符合要求，请检查密码长度和填写内容。";
                }

                throw new Exception(message);
            }

            return json;

        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}

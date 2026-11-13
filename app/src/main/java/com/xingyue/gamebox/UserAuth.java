package com.xingyue.gamebox;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

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
        return context.getSharedPreferences(
            PREFS, Context.MODE_PRIVATE
        );
    }

    public boolean isLoggedIn() {
        return !prefs().getString(
            "access_token", ""
        ).isEmpty();
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

    // 获取有效的访问令牌。
    // 必须在后台线程调用，不能在主线程调用。
    public synchronized String getValidAccessToken()
            throws Exception {

        String accessToken = getAccessToken();

        if (accessToken.isEmpty()) {
            throw new Exception("登录凭证不存在，请重新登录。");
        }

        long expiration = getTokenExpiration(accessToken);

        long now = System.currentTimeMillis() / 1000L;

        // 令牌还有超过60秒才过期，直接使用。
        if (expiration > 0 && expiration > now + 60) {
            return accessToken;
        }

        // 令牌即将过期或已经过期，自动刷新。
        return refreshSession();
    }

    // 使用 refresh_token 向 Supabase 申请新令牌。
    public synchronized String refreshSession()
            throws Exception {

        String refreshToken = prefs().getString(
            "refresh_token", ""
        );

        if (refreshToken.isEmpty()) {
            throw new Exception(
                "登录凭证已过期，请重新登录一次。"
            );
        }

        JSONObject body = new JSONObject();
        body.put("refresh_token", refreshToken);

        JSONObject result;

        try {
            result = request(
                "/auth/v1/token?grant_type=refresh_token",
                body,
                null
            );
        } catch (Exception e) {
            throw new Exception(
                "自动续期失败，请检查网络或重新登录。"
            );
        }

        String newAccess = result.optString(
            "access_token", ""
        );

        String newRefresh = result.optString(
            "refresh_token", ""
        );

        if (newAccess.isEmpty()) {
            throw new Exception(
                "服务器没有返回新的登录凭证，请重新登录。"
            );
        }

        JSONObject user = result.optJSONObject("user");

        SharedPreferences.Editor editor = prefs().edit();

        editor.putString("access_token", newAccess);

        if (!newRefresh.isEmpty()) {
            editor.putString("refresh_token", newRefresh);
        }

        if (user != null) {
            String newUserId = user.optString("id", "");

            String newEmail = user.optString("email", "");

            if (!newUserId.isEmpty()) {
                editor.putString("user_id", newUserId);
            }

            if (!newEmail.isEmpty()) {
                editor.putString("email", newEmail);
            }
        }

        editor.apply();

        return newAccess;
    }

    // 读取 JWT 中的过期时间。
    private long getTokenExpiration(String token) {

        try {
            String[] parts = token.split("\\.");

            if (parts.length < 2) {
                return 0;
            }

            byte[] decoded = Base64.decode(
                parts[1],
                Base64.URL_SAFE | Base64.NO_WRAP
                    | Base64.NO_PADDING
            );

            JSONObject payload = new JSONObject(
                new String(decoded, "UTF-8")
            );

            return payload.optLong("exp", 0);

        } catch (Exception e) {
            return 0;
        }
    }

    public void logout() {

        String token = getAccessToken();

        if (!token.isEmpty()) {
            try {
                request(
                    "/auth/v1/logout",
                    new JSONObject(),
                    token
                );
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
            "/auth/v1/signup",
            body,
            null
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

    private void saveSession(
            JSONObject result, String email
    ) throws Exception {

        String access = result.optString(
            "access_token", ""
        );

        String refresh = result.optString(
            "refresh_token", ""
        );

        JSONObject user = result.optJSONObject("user");

        String userId = user == null
            ? "" : user.optString("id", "");

        if (access.isEmpty()) {
            throw new Exception(
                "没有获取到登录凭证，请先验证邮箱后再登录。"
            );
        }

        SharedPreferences.Editor editor = prefs().edit();

        editor.putString("access_token", access);

        if (!refresh.isEmpty()) {
            editor.putString("refresh_token", refresh);
        }

        editor.putString(
            "email",
            user != null
                ? user.optString("email", email)
                : email
        );

        if (!userId.isEmpty()) {
            editor.putString("user_id", userId);
        }

        editor.apply();
    }

    private JSONObject request(
            String path,
            JSONObject body,
            String bearer
    ) throws Exception {

        HttpURLConnection conn = null;

        try {
            URL url = new URL(BASE + path);

            conn = (HttpURLConnection)
                url.openConnection();

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

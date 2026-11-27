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
import java.util.Locale;

public class UserAuth {

    private static final String PREFS = "xingyue_user_session";
    private static final String BASE = "https://4.wc3.tech";

    private final Context context;

    public UserAuth(Context context) {
        this.context = context.getApplicationContext();
    }

    private SharedPreferences prefs() {
        return context.getSharedPreferences(
            PREFS, Context.MODE_PRIVATE
        );
    }

    // 安装邀请识别码单独保存，退出登录时不会被清除。
    private SharedPreferences installPrefs() {
        return context.getSharedPreferences(
            "xingyue_install_referral", Context.MODE_PRIVATE
        );
    }

    // 保存待提交的安装识别码。
    public void setPendingInstallCode(String code) {
        if (code == null) return;

        String value = code.trim();

        if (value.matches(
            "(?i)^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"
        )) {
            installPrefs().edit()
                .putString("pending_install_code", value)
                .apply();
        }
    }

    // 获取待提交的安装识别码。
    public String getPendingInstallCode() {
        return installPrefs().getString(
            "pending_install_code", ""
        );
    }

    // 注册成功后清除安装识别码。
    public void clearPendingInstallCode() {
        installPrefs().edit()
            .remove("pending_install_code")
            .apply();
    }

    public boolean isLoggedIn() {
        return !prefs().getString(
            "access_token", ""
        ).isEmpty();
    }

    public String getEmail() {
        return prefs().getString("email", "");
    }

    // 获取星月账号；邮箱注册用户返回空字符串。
    public String getAccount() {
        return prefs().getString("xingyue_account", "");
    }

    public String getUserId() {
        return prefs().getString("user_id", "");
    }

    public String getAccessToken() {
        return prefs().getString("access_token", "");
    }

    // 一键注册成功后返回的账号和密码。
    public static class OneTapResult {
        public final String account;
        public final String password;

        public OneTapResult(String account, String password) {
            this.account = account;
            this.password = password;
        }
    }

    private boolean isXingyueAccount(String value) {
        return value != null &&
            value.matches("(?i)^XY[A-Z0-9]{8}$");
    }

    // 一键注册：创建账号、保存登录状态并自动登录。
    public OneTapResult oneTapRegister()
            throws Exception {

        JSONObject signupBody = new JSONObject();

        String pendingInstallCode = getPendingInstallCode();

        if (!pendingInstallCode.isEmpty()) {
            signupBody.put("install_code", pendingInstallCode);
        }

        JSONObject result = request(
            "/auth/v1/signup/one-tap",
            signupBody,
            null
        );

        String account = result.optString("account", "");

        String password = result.optString(
            "generated_password",
            result.optString("password", "")
        );

        JSONObject user = result.optJSONObject("user");

        String internalEmail = user == null
            ? ""
            : user.optString("email", "");

        if (account.isEmpty() && !internalEmail.isEmpty()) {
            int at = internalEmail.indexOf("@");
            if (at > 0) {
                account = internalEmail.substring(0, at)
                    .toUpperCase(Locale.ROOT);
            }
        }

        if (account.isEmpty() || password.isEmpty()) {
            throw new Exception(
                "注册信息不完整，服务器没有返回账号或密码。"
            );
        }

        if (!isXingyueAccount(account)) {
            throw new Exception(
                "服务器返回的星月账号格式不正确。"
            );
        }

        if (internalEmail.isEmpty()) {
            internalEmail = account.toLowerCase(Locale.ROOT)
                + "@xingyue.local";
        }

        saveSession(result, internalEmail);

        prefs().edit()
            .putString("xingyue_account", account)
            .apply();

        // 注册和登录信息保存成功后清除安装识别码。
        clearPendingInstallCode();

        return new OneTapResult(account, password);
    }

    // 获取有效访问令牌。
    // 必须在后台线程调用。
    public synchronized String getValidAccessToken()
            throws Exception {

        String accessToken = getAccessToken();

        if (accessToken.isEmpty()) {
            throw new Exception("登录凭证不存在，请重新登录。");
        }

        long expiration = getTokenExpiration(accessToken);
        long now = System.currentTimeMillis() / 1000L;

        if (expiration > 0 && expiration > now + 60) {
            return accessToken;
        }

        return refreshSession();
    }

    // 自动刷新登录令牌。
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

    // 读取 JWT 过期时间。
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

    // 退出登录。
    public void logout() {

        String token = getAccessToken();
        String refreshToken = prefs().getString(
            "refresh_token", ""
        );

        if (!token.isEmpty()) {
            try {
                JSONObject body = new JSONObject();

                if (!refreshToken.isEmpty()) {
                    body.put("refresh_token", refreshToken);
                }

                request(
                    "/auth/v1/logout",
                    body,
                    token
                );
            } catch (Exception ignored) {
            }
        }

        // 只清除登录信息，不清除尚未使用的安装识别码。
        prefs().edit().clear().apply();
    }

    // 邮箱注册，保留原有功能并提交安装识别码。
    public String signUp(String email, String password)
            throws Exception {

        JSONObject body = new JSONObject();
        body.put("email", email);
        body.put("password", password);

        String pendingInstallCode = getPendingInstallCode();

        if (!pendingInstallCode.isEmpty()) {
            body.put("install_code", pendingInstallCode);
        }

        JSONObject result = request(
            "/auth/v1/signup",
            body,
            null
        );

        if (!result.optString("access_token", "").isEmpty()) {
            saveSession(result, email);

            // 注册和登录信息保存成功后清除安装识别码。
            clearPendingInstallCode();

            return "注册成功，已自动登录。";
        }

        throw new Exception(
            "注册未完成：服务器没有返回登录凭证，请稍后重试。"
        );
    }

    // 登录：支持邮箱或星月账号。
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

    // 保存登录凭证。
    private void saveSession(
            JSONObject result, String loginName
    ) throws Exception {

        String access = result.optString(
            "access_token", ""
        );

        String refresh = result.optString(
            "refresh_token", ""
        );

        JSONObject user = result.optJSONObject("user");

        String userId = user == null
            ? ""
            : user.optString("id", "");

        if (access.isEmpty()) {
            throw new Exception(
                "服务器没有返回登录凭证，请稍后重试。"
            );
        }

        String savedEmail = user != null
            ? user.optString("email", loginName)
            : loginName;

        SharedPreferences.Editor editor = prefs().edit();

        editor.putString("access_token", access);
        editor.putString("email", savedEmail);

        if (!refresh.isEmpty()) {
            editor.putString("refresh_token", refresh);
        }

        if (!userId.isEmpty()) {
            editor.putString("user_id", userId);
        }

        if (isXingyueAccount(loginName)) {
            editor.putString(
                "xingyue_account",
                loginName.toUpperCase(Locale.ROOT)
            );
        } else {
            editor.remove("xingyue_account");
        }

        editor.apply();
    }

    // 网络请求。
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

                String lower = message.toLowerCase(Locale.ROOT);

                if (lower.contains("invalid login credentials")) {
                    message = "邮箱、星月账号或密码不正确。";

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

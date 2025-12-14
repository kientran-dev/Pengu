package cinema.ticket.booking.utils;

import org.json.JSONObject;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.DataOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import cinema.ticket.booking.model.Payment;

public class VNPay extends HttpServlet {

    private static String vnp_PayUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    private static String vnp_Version = "2.1.0";

    // Lấy Key từ biến môi trường (nếu có), nếu không thì dùng key test mặc định
    private static String vnp_TmnCode = System.getenv("VNP_TMN_CODE") != null ? System.getenv("VNP_TMN_CODE") : "K2HAGWZO";
    private static String vnp_HashSecret = System.getenv("VNP_HASH_SECRET") != null ? System.getenv("VNP_HASH_SECRET") : "F51NP6JCH16I0X6S584KR9CNJFZPTN5J";

    // Tự động chọn URL: Nếu trên Render thì lấy FRONTEND_URL, nếu local thì dùng localhost
    private static String getReturnUrl() {
        String appUrl = System.getenv("FRONTEND_URL");

        // --- THÊM DÒNG NÀY ĐỂ SOI LỖI TRÊN LOG RENDER ---
        System.out.println("DEBUG VNPAY - FRONTEND_URL IS: " + appUrl);
        // ------------------------------------------------

        if (appUrl == null || appUrl.isEmpty()) {
            System.out.println("DEBUG VNPAY - Fallback to LOCALHOST");
            return "http://localhost/order-complete";
        }
        return appUrl.replaceAll("/$", "") + "/order-complete";
    }
    private static String vnp_Returnurl = getReturnUrl();

    private static String vnp_apiUrl = "https://sandbox.vnpayment.vn/merchant_webapi/api/transaction";

    public static String createPay(Payment payment, String bankCode, String ip_addr)
            throws ServletException, IOException {
        // --- THÊM LOG VÀO ĐÂY ---
        System.out.println("DEBUG: Dang tao link thanh toan...");
        System.out.println("DEBUG: vnp_ReturnUrl hien tai la: " + vnp_Returnurl);
        // ------------------------

        String vnp_Command = "pay";

        long amount = Math.round(payment.getAmount() * 100);
        String vnp_IpAddr = ip_addr;
        String vnp_TxnRef = payment.getId();

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amount));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_OrderType", "other");

        // SỬA PHẦN NÀY
        if (bankCode != null && !bankCode.trim().isEmpty() && !bankCode.equalsIgnoreCase("VNPAY")) {
            vnp_Params.put("vnp_BankCode", bankCode.trim());
        }
        // Nếu không truyền hoặc truyền "VNPAY" thì để người dùng chọn trên trang VNPay

        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", "Thanh toan ve xem phim - Don hang: " + vnp_TxnRef);
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", vnp_Returnurl);
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        cld.setTime(payment.getCreateAt());
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        // --- ĐOẠN SỬA LỖI ---
        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();

        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                // Chuẩn bị dữ liệu mã hóa
                String encodedField = URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString());
                String encodedValue = URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString());

                // Build hash data (SỬA: Phải encode cả dữ liệu đưa vào hash)
                hashData.append(encodedField);
                hashData.append('=');
                hashData.append(encodedValue);

                // Build query
                query.append(encodedField);
                query.append('=');
                query.append(encodedValue);

                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }
        // --------------------

        String queryUrl = query.toString();
        String vnp_SecureHash = hmacSHA512(vnp_HashSecret, hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
        String paymentUrl = vnp_PayUrl + "?" + queryUrl;

        URL url = new URL(vnp_apiUrl);

// --- THÊM DÒNG NÀY ---
        System.out.println("DEBUG VNPAY URL: " + vnp_apiUrl);
// --------------------

        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        return paymentUrl;
    }

    /*
     * Return 0 -> Payment success
     * Return 1 -> Payment is in processing or error while request
     * Return 2 -> Payment is canceled or not completed
     */
    public static Integer verifyPay(Payment payment) throws ServletException, IOException {
        String vnp_RequestId = payment.getId() + getRandomID(10000, 99999);
        String vnp_Command = "querydr";
        String vnp_TxnRef = payment.getId();
        String vnp_OrderInfo = "Kiem tra ket qua GD don hang " + vnp_TxnRef;

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());

        cld.setTime(payment.getCreateAt());
        String vnp_TransDate = formatter.format(cld.getTime());

        String vnp_IpAddr = "127.0.0.1"; // Use a stable IP address

        JsonObject vnp_Params = new JsonObject();

        vnp_Params.addProperty("vnp_RequestId", vnp_RequestId);
        vnp_Params.addProperty("vnp_Version", vnp_Version);
        vnp_Params.addProperty("vnp_Command", vnp_Command);
        vnp_Params.addProperty("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.addProperty("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.addProperty("vnp_OrderInfo", vnp_OrderInfo);
        vnp_Params.addProperty("vnp_TransactionDate", vnp_TransDate);
        vnp_Params.addProperty("vnp_CreateDate", vnp_CreateDate);
        vnp_Params.addProperty("vnp_IpAddr", vnp_IpAddr);

        String hash_Data = vnp_RequestId + "|" + vnp_Version + "|" + vnp_Command + "|" + vnp_TmnCode + "|" + vnp_TxnRef
                + "|" + vnp_TransDate + "|" + vnp_CreateDate + "|" + vnp_IpAddr + "|" + vnp_OrderInfo;

        String vnp_SecureHash = hmacSHA512(vnp_HashSecret, hash_Data.toString());

        vnp_Params.addProperty("vnp_SecureHash", vnp_SecureHash);

        URL url = new URL(vnp_apiUrl);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setDoOutput(true);
        try (DataOutputStream wr = new DataOutputStream(con.getOutputStream())) {
            wr.writeBytes(vnp_Params.toString());
            wr.flush();
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
            String output;
            while ((output = in.readLine()) != null) {
                response.append(output);
            }
        }
        
        System.out.println("VNPAY Response: " + response.toString());

        JSONObject json = new JSONObject(response.toString());

        String res_ResponseCode = json.optString("vnp_ResponseCode");
        if (!"00".equals(res_ResponseCode)) {
            return 1; // General error
        }

        String res_TransactionStatus = json.optString("vnp_TransactionStatus");
        if ("00".equals(res_TransactionStatus)) {
             // Additional checks for security
            String res_TxnRef = json.optString("vnp_TxnRef");
            double res_Amount = json.optDouble("vnp_Amount") / 100;

            if (!res_TxnRef.equals(payment.getId()) || res_Amount != payment.getAmount()) {
                return 2; // Data mismatch, potential fraud
            }
            return 0; // Success
        } else if ("01".equals(res_TransactionStatus) || "02".equals(res_TransactionStatus)) {
            return 1; // Pending
        } else {
            return 2; // Canceled or failed
        }
    }

    private static String hmacSHA512(final String key, final String data) {
        try {

            if (key == null || data == null) {
                throw new NullPointerException();
            }
            final Mac hmac512 = Mac.getInstance("HmacSHA512");
            byte[] hmacKeyBytes = key.getBytes();
            final SecretKeySpec secretKey = new SecretKeySpec(hmacKeyBytes, "HmacSHA512");
            hmac512.init(secretKey);
            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            byte[] result = hmac512.doFinal(dataBytes);
            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();

        } catch (Exception ex) {
            return "";
        }
    }

    private static String getRandomID(int min, int max) {
        return String.valueOf((int) ((Math.random() * (max - min)) + min));
    }
}

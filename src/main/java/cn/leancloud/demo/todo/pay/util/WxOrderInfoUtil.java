package cn.leancloud.demo.todo.pay.util;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 微信订单生成工具
 */
public class WxOrderInfoUtil {

    public static Map<String, String> buildPrepareOrderParamMap(String wxAppId, String wxAppKey, String wxMchId,
                                                                String body, float total_amount, String spbill_create_ip,
                                                                String passback_params) {
        Map<String, String> param = new HashMap<>();
        param.put("appid", wxAppId);
        param.put("mch_id", wxMchId);
        param.put("nonce_str", genNonceStr());
        param.put("body", body);
        param.put("out_trade_no", genOutTradeNo());
        // 注意total_amount单元是元,total_fee单位是分,需要*100
        param.put("total_fee", String.valueOf((int) (total_amount * 100)));
        param.put("spbill_create_ip", spbill_create_ip);
        param.put("notify_url", "http://diandianbo.leanapp.cn/wxpaycallback");
        param.put("trade_type", "APP");
        // 透传数据, 用户信息
        param.put("attach", passback_params);

        param.put("sign", genSign(wxAppKey, param));
        return param;
    }

    /**
     * 生成 sign 签名
     */
    public static String genSign(String wxAppKey, Map<String, String> param) {
        String sign = "";

        // =拼接键值对, 然后&连接起来
        List<String> signList = new ArrayList<>();
        for (Map.Entry<String, String> entry : param.entrySet()) {
            signList.add("&" + entry.getKey() + "=" + entry.getValue());
        }
        // 按照字母排序
        Collections.sort(signList);

        // 拼成字符串
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < signList.size(); i++) {
            sb.append(signList.get(i).substring(i == 0 ? 1 : 0));
        }
        // 末尾再拼接一个API秘钥
        sb.append("&key=").append(wxAppKey);
        try {
            // MD5 + toUpperCase生成签名
            String md5 = MD5Encrypt.MD5(sb.toString());
            if (md5 != null) {
                sign = md5.toUpperCase();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sign;
    }

    /**
     * 订单号 当前时间年月日+毫秒值, 保证唯一
     */
    private static String genOutTradeNo() {
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        return sdf.format(date) + System.currentTimeMillis() % 1000;
    }

    /**
     * 随机生成10~20位的大小写字母
     */
    private static String genNonceStr() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < new Random().nextInt(10) + 10; i++) {
            char c = new Random().nextBoolean()
                    ? (char) (new Random().nextInt(26) + 65)
                    : (char) (new Random().nextInt(26) + 97);
            sb.append(c);
        }
        return sb.toString();
    }
}

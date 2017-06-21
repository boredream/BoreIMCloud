package cn.leancloud.demo.todo.pay;

import cn.leancloud.demo.todo.pay.net.HttpUtils;
import cn.leancloud.demo.todo.pay.net.XmlHttpUtils;
import cn.leancloud.demo.todo.pay.util.OrderInfoUtil2_0;
import cn.leancloud.demo.todo.pay.util.WxOrderInfoUtil;
import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVQuery;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * 重要说明:
 * <p>
 * 这里只是为了方便直接向商户展示支付宝的整个支付流程；所以Demo中加签过程直接放在客户端完成；
 * 真实App里，privateKey等数据严禁放在客户端，加签过程务必要放在服务端完成；
 * 防止商户私密数据泄露，造成不必要的资金损失，及面临各种安全风险；
 */
public class PayUtils {

    private static PayUtils instance;

    private PayUtils() {
    }

    public static PayUtils getInstance() {
        if (instance == null) {
            instance = new PayUtils();
        }
        return instance;
    }

    /**
     * [思] 支付宝支付业务：入参app_id
     */
    public static final String APPID = "2016121304206651";

    /**
     * [思] 私钥
     */
    public static final String RSA_PRIVATE = "MIICdQIBADANBgkqhkiG9w0BAQEFAASCAl8wggJbAgEAAoGBAKyhJ18jqx6UzRJEHAc/Ai2QpxPaNfOCCrr0fxHKy9r37o+0LoHI7/lO1j1xnPM3WKS+7Q4kVGQE3p7uyJjgQfxH85bo7QD3u35bZbx3YY6aLC8gxqiCZlDcEU/1dxYE0XG3iOjVgTs72MGSmn/N1n5hi8R4nsQQkZQe8UXJEDo9AgMBAAECgYB5fHwBt09QmTVsemQjMVNuD5OVJAa91LGKelAGjGpEMiFAHnRwP6GqGLIq0Y67lyKZ5gdb4XNGZCrrPG4NMB0erlTW/fwa8bgUeRtATiimCYhlV9kNUXi3Pbm+MofrpzM2UXcsHU36FftphW6Ipgr0Rc0gEQ2JR4QuiPsGarMZ3QJBAOCz8UgIOgHLwX6MQNA+ypVWpHWtAvTlrAMpnHenj+SnW4vL93NgDl2ESA+vUnJPlUOFgIOji4gfx46zSP3jK38CQQDErHh4OrN9dzuX5VCM0KYWxXZUXTJf4b4SiBvdHTQrjKmloHtO1cu0hRymg7O+8NTPFkHsqFkowqvY4rBQMChDAkA59vFFDao5EGDHzlJh5fDIeWNPX+QlXKH05uUQEM+TwoBhHaqlvp+2DAuy0B1Kk7EDjArM6oFyChCmhSVNKS7tAkB4pDdNwOxjVSVoGpFbn42SQFRCkdyZtjbK1VUKQKTE5gcWgyfAhyLqvBCEEBvNT5uq4ENWMTpn52pUo1F2DuRTAkANN3Fx/VwK/OSJfQJKxfUGVTtbDobUevT3eV8BC/rSU5I0BovbfenKG7n5AkIsizGqdzDpVSKGwewQM4S34pUf";

    /**
     * 支付宝公钥
     */
    public static final String ALIPAY_PUBLIC_KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDDI6d306Q8fIfCOaTXyiUeJHkrIvYISRcc73s3vF1ZT7XN8RNPwJxo8pWaJMmvyTn9N4HQ632qJBVHf8sxHi/fEsraprwCtzvzQETrNRwVxLO5jVmRGi60j8Ue1efIlzPXV9je9mkjzOmdssymZkh2QhUrCmZYI/FCEa3/cNMW0QIDAQAB";

    public static final int PAY_TYPE_ALIPAY = 1;
    public static final int PAY_TYPE_WX = 2;

    /**
     * 微信app id
     */
    public static final String WX_APP_ID = "wx8172f08f9e96f671";

    /**
     * 微信秘钥
     */
    public static final String WX_APP_KEY = "A9D0BFCF46750FFD1812A85FED12A900";

    /**
     * 微信商户号
     */
    public static final String WX_MCH_ID = "1430763602";

    /**
     * 获取签名后订单信息
     */
    public OrderSignInfo getPaySignInfo(float total_amount, String subject, String body, int rechargeType, int goodsType, String userid, String bookid, float money, String spbill_create_ip) {
        OrderSignInfo orderSignInfo = new OrderSignInfo();
        orderSignInfo.rechargeType = rechargeType;

        // 透传数据, 回调时会返回, 注意要url encode
        String passback_params = "{\"goodsType\":" + goodsType + ", \"userid\":\"" + userid + "\", \"bookid\":\"" + bookid + "\", \"money\":" + money + ", \"rechargeType\":" + rechargeType + "}";

        if(rechargeType == 1) {
            try {
                // 支付宝的透传数据需要url encode
                passback_params = URLEncoder.encode(passback_params, "utf-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        if(rechargeType == PAY_TYPE_ALIPAY) {
            Map<String, String> params = OrderInfoUtil2_0.buildOrderParamMap(APPID, total_amount, subject, body, passback_params);
            String orderParam = OrderInfoUtil2_0.buildOrderParam(params);
            String sign = OrderInfoUtil2_0.getSign(params, RSA_PRIVATE);

            orderSignInfo.return_code = OrderSignInfo.SUCCESS;
            orderSignInfo.alipayOrderSign = orderParam + "&" + sign;
            System.out.println("alipay order info = " + orderSignInfo.alipayOrderSign);
        } else if(rechargeType == PAY_TYPE_WX) {
            Map<String, String> param = WxOrderInfoUtil.buildPrepareOrderParamMap(WX_APP_ID, WX_APP_KEY, WX_MCH_ID, body, total_amount, spbill_create_ip, passback_params);

            try {
                String response = XmlHttpUtils.postString("https://api.mch.weixin.qq.com/pay/unifiedorder", param);
                Map<String, String> responseMap = XmlHttpUtils.readStringXmlOut(response);

                orderSignInfo.return_code = responseMap.get("return_code");
                orderSignInfo.return_msg = responseMap.get("return_msg");
                orderSignInfo.appid = responseMap.get("appid");
                orderSignInfo.mch_id = responseMap.get("mch_id");
                orderSignInfo.device_info = responseMap.get("device_info");
                orderSignInfo.prepay_id = responseMap.get("prepay_id");
                orderSignInfo.nonce_str = responseMap.get("nonce_str");
                orderSignInfo.result_code = responseMap.get("result_code");
                orderSignInfo.err_code = responseMap.get("err_code");
                orderSignInfo.err_code_des = responseMap.get("err_code_des");
                orderSignInfo.packageValue = "Sign=WXPay";
                orderSignInfo.timeStamp = System.currentTimeMillis() + "";

                responseMap.put("packageValue", "Sign=WXPay");
                responseMap.put("timeStamp", System.currentTimeMillis() + "");
                responseMap.remove("sign");

                System.out.println("-- pay sign map start --");
                Map<String, String> payMap = new HashMap<>();
                payMap.put("appid", orderSignInfo.appid);
                payMap.put("partnerid", orderSignInfo.mch_id);
                payMap.put("prepayid", orderSignInfo.prepay_id);
                payMap.put("noncestr", orderSignInfo.nonce_str);
                payMap.put("package", orderSignInfo.packageValue);
                payMap.put("timestamp", orderSignInfo.timeStamp);
                for (Map.Entry<String, String> entry : payMap.entrySet()) {
                    System.out.println(entry.getKey() + " = " + entry.getValue());
                }
                System.out.println("-- pay sign map end --");

                // 加入新字段,再次生成sign
                orderSignInfo.sign = WxOrderInfoUtil.genSign(WX_APP_KEY, payMap);

                System.out.println("get wx prepare order info = " + orderSignInfo);

                // 订单成功时, 先在服务端记录一个待支付订单, 记录订单号, preparedId
                OrderInfo waitPayOrder = new OrderInfo();
                waitPayOrder.put("trade_no", param.get("out_trade_no"));
                waitPayOrder.put("prepared_id", orderSignInfo.prepay_id);
                waitPayOrder.put("return_code", "WAIT_PAY");

                try {
                    AVQuery<OrderInfo> query = AVQuery.getQuery(OrderInfo.class)
                            .whereEqualTo("trade_no", param.get("out_trade_no"));
                    OrderInfo info = query.getFirst();
                    if(info == null) {
                        waitPayOrder.save();
                        // 订单保存成功
                        System.out.println("wait pay oder save success");
                    } else {
                        System.out.println("wait pay oder duplicate");
                    }
                } catch (AVException e){
                    orderSignInfo.return_code = "FAIL";
                    orderSignInfo.return_msg = "支付订单生成失败";

                    System.out.println("wait pay oder save error");
                }

            } catch (Exception e) {
                orderSignInfo.return_code = "FAIL";
                orderSignInfo.return_msg = "支付订单生成失败";
                System.out.println("wx get order error");
                e.printStackTrace();
            }
        } else {
            orderSignInfo.return_code = "FAIL";
            orderSignInfo.return_msg = "支付订单生成失败";
            System.out.println("not a legal recharge type");
        }

        return orderSignInfo;
    }
}

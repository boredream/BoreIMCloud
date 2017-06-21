package cn.leancloud.demo.todo.pay;

import java.io.Serializable;

public class OrderSignInfo implements Serializable {

    public static final String SUCCESS = "SUCCESS";

    /**
     * 1-支付宝 2-微信
     */
    public int rechargeType;

    public String return_code;
    public String return_msg;

    // 以下字段在return_code为SUCCESS的时候有返回

    // 微信字段
    // 应用APPID   调用接口提交的应用ID
    public String appid;
    // 商户号 调用接口提交的商户号
    public String mch_id;
    // 设备号  调用接口提交的终端设备号，
    public String device_info;
    // 随机字符串  微信返回的随机字符串
    public String nonce_str;
    // 签名 sign 微信返回的签名，详见签名算法
    public String sign;
    // 业务结果 SUCCESS/FAIL
    public String result_code;
    // 错误代码 详细参见第6节错误列表
    public String err_code;
    // 错误代码描述 系统错误 错误返回的信息描述
    public String err_code_des;
    // 支付id
    public String prepay_id;
    public String packageValue;
    public String timeStamp;


    // 支付宝字段
    public String alipayOrderSign;

    @Override
    public String toString() {
        return "OrderSignInfo{" +
                "rechargeType=" + rechargeType +
                ", return_code='" + return_code + '\'' +
                ", return_msg='" + return_msg + '\'' +
                ", appid='" + appid + '\'' +
                ", mch_id='" + mch_id + '\'' +
                ", device_info='" + device_info + '\'' +
                ", nonce_str='" + nonce_str + '\'' +
                ", sign='" + sign + '\'' +
                ", result_code='" + result_code + '\'' +
                ", err_code='" + err_code + '\'' +
                ", err_code_des='" + err_code_des + '\'' +
                ", prepay_id='" + prepay_id + '\'' +
                ", packageValue='" + packageValue + '\'' +
                ", timeStamp='" + timeStamp + '\'' +
                ", alipayOrderSign='" + alipayOrderSign + '\'' +
                '}';
    }
}

package cn.leancloud.demo.todo.pay;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.avos.avoscloud.*;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

@WebServlet(name = "PayCallback", urlPatterns = {"/paycallback"})
public class PayCallbackServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Map<String, String[]> reqParameterMap = req.getParameterMap();
        Map<String, String> reqParams = new HashMap<>();
        System.out.println("-- pay callback start --");
        for (Map.Entry<String, String[]> entry : reqParameterMap.entrySet()) {
            reqParams.put(entry.getKey(), entry.getValue()[0]);
            System.out.println(entry.getKey() + ":" + entry.getValue()[0]);
        }
        System.out.println("-- pay callback end --");

        try {
            boolean checkResult = AlipaySignature.rsaCheckV1(reqParams, PayUtils.ALIPAY_PUBLIC_KEY, "utf-8");
            System.out.println("check result = " + checkResult);
            if (checkResult) {
                // 验证通过, 记录订单, 其中passback_params包含userid、money和rechargeType
                System.out.println("save order info");

                OrderInfo orderInfo = new OrderInfo();

                String passback_params = reqParams.get("passback_params");
                passback_params = URLDecoder.decode(passback_params, "utf-8");
                System.out.println("passback params = " + passback_params);
                JSONObject jsonObject = JSONObject.parseObject(passback_params);
                // 充值类型
                int rechargeType = jsonObject.getIntValue("rechargeType");
                orderInfo.put("rechargeType", rechargeType);
                // 商品类型
                int goodsType = jsonObject.getIntValue("goodsType");
                orderInfo.put("goodsType", goodsType);
                // 用户id
                String userid = jsonObject.getString("userid");
                orderInfo.put("userid", userid);

                if (goodsType == 1) {
                    // 充值
                    Float money = jsonObject.getFloat("money");
                    orderInfo.put("money", money);
                } else if (goodsType == 2) {
                    // 买书
                    String bookid = jsonObject.getString("bookid");
                    orderInfo.put("bookid", bookid);
                }

                orderInfo.put("gmt_create", reqParams.get("gmt_create"));
                orderInfo.put("charset", reqParams.get("charset"));
                orderInfo.put("seller_email", reqParams.get("seller_email"));
                orderInfo.put("subject", reqParams.get("subject"));
                orderInfo.put("body", reqParams.get("body"));
                orderInfo.put("buyer_id", reqParams.get("buyer_id"));
                orderInfo.put("invoice_amount", reqParams.get("invoice_amount"));
                orderInfo.put("notify_id", reqParams.get("notify_id"));
                orderInfo.put("fund_bill_list", reqParams.get("fund_bill_list"));
                orderInfo.put("notify_type", reqParams.get("notify_type"));
                orderInfo.put("trade_status", reqParams.get("trade_status"));
                orderInfo.put("receipt_amount", reqParams.get("receipt_amount"));
                orderInfo.put("buyer_pay_amount", reqParams.get("buyer_pay_amount"));
                orderInfo.put("app_id", reqParams.get("app_id"));
                orderInfo.put("seller_id", reqParams.get("seller_id"));
                orderInfo.put("notify_time", reqParams.get("notify_time"));
                orderInfo.put("gmt_payment", reqParams.get("gmt_payment"));
                orderInfo.put("version", reqParams.get("version"));
                orderInfo.put("out_trade_no", reqParams.get("out_trade_no"));
                orderInfo.put("total_amount", reqParams.get("total_amount"));
                orderInfo.put("trade_no", reqParams.get("trade_no"));
                orderInfo.put("auth_app_id", reqParams.get("auth_app_id"));
                orderInfo.put("buyer_logon_id", reqParams.get("buyer_logon_id"));
                orderInfo.put("point_amount", reqParams.get("point_amount"));

                try {
                    AVQuery<OrderInfo> query = AVQuery.getQuery(OrderInfo.class)
                            .whereEqualTo("trade_no", reqParams.get("trade_no"));
                    OrderInfo info = query.getFirst();
                    if (info == null) {
                        orderInfo.save();
                        // 订单保存成功
                        System.out.println("order save success");
                    } else {
                        System.out.println("order save duplicate");
                    }
                } catch (AVException e) {
                    System.out.println("order save error");
                }

                System.out.println("pay status = " + reqParams.get("trade_status"));

                if (reqParams.get("trade_status").equals(OrderInfo.TRADE_SUCCESS)) {
                    AVUser user = new AVUser();
                    user.setObjectId(userid);

                    if (goodsType == 1) {
                        // 充值
                        Float money = jsonObject.getFloat("money");
                        try {
                            user.increment("money", money);
                            user.save();
                            System.out.println("pay success, increase success, user: " + userid + " ... money: " + money);
                        } catch (AVException e) {
                            System.out.println("pay success, increase error, user: " + userid + " ... money: " + money);
                            e.printStackTrace();
                        }
                    } else if (goodsType == 2) {
                        // 买书
                        String bookid = jsonObject.getString("bookid");
                        try {
                            AVObject book = new AVObject("Book");
                            book.setObjectId(bookid);

                            AVRelation<AVObject> relation = user.getRelation("purchasedBook");
                            relation.add(book);
                            user.save();
                            System.out.println("pay success, purchase book success, user: " + userid + " ... bookid: " + bookid);
                        } catch (AVException e) {
                            System.out.println("pay success, purchase book error, user: " + userid + " ... bookid: " + bookid);
                            e.printStackTrace();
                        }
                    }
                }
            }
        } catch (AlipayApiException e) {
            System.out.println("check error");
            e.printStackTrace();
        }

        // 支付宝必须要打印success
        System.out.println("success");
    }

}

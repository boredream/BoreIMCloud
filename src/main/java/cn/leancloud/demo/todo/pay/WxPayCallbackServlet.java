package cn.leancloud.demo.todo.pay;

import cn.leancloud.demo.todo.pay.net.XmlHttpUtils;
import com.alibaba.fastjson.JSONObject;
import com.alipay.api.internal.util.XmlUtils;
import com.avos.avoscloud.*;
import org.xml.sax.SAXException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

@WebServlet(name = "WxPayCallback", urlPatterns = {"/wxpaycallback"})
public class WxPayCallbackServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(req.getInputStream()));
        StringBuilder stb = new StringBuilder();
        String line = null;
        while ((line = in.readLine()) != null) {
            stb.append(line);
        }
        System.out.println("wx call back response = " + stb.toString());

        try {
            Map<String, String> reqParams = XmlHttpUtils.readStringXmlOut(stb.toString());
            System.out.println("-- wx pay callback start --");
            for (Map.Entry<String, String> entry : reqParams.entrySet()) {
                System.out.println(entry.getKey() + ":" + entry.getValue());
            }
            System.out.println("-- wx pay callback end --");
            
            OrderInfo orderInfo = new OrderInfo();

            String passback_params = reqParams.get("attach");
            System.out.println("attach params = " + passback_params);
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

            // 主键订单号
            orderInfo.put("trade_no", reqParams.get("out_trade_no"));

            orderInfo.put("transaction_id", reqParams.get("transaction_id"));
            orderInfo.put("bank_type", reqParams.get("bank_type"));
            orderInfo.put("openid", reqParams.get("openid"));
            orderInfo.put("fee_type", reqParams.get("fee_type"));
            orderInfo.put("mch_id", reqParams.get("mch_id"));
            orderInfo.put("cash_fee", reqParams.get("cash_fee"));
            orderInfo.put("appid", reqParams.get("appid"));
            orderInfo.put("total_fee", reqParams.get("total_fee"));
            orderInfo.put("trade_type", reqParams.get("trade_type"));
            orderInfo.put("result_code", reqParams.get("result_code"));
            orderInfo.put("time_end", reqParams.get("time_end"));
            orderInfo.put("is_subscribe", reqParams.get("is_subscribe"));
            orderInfo.put("return_code", reqParams.get("return_code"));

            try {
                AVQuery<OrderInfo> query = AVQuery.getQuery(OrderInfo.class)
                        .whereEqualTo("trade_no", reqParams.get("out_trade_no"));
                OrderInfo info = query.getFirst();
                if(info == null) {
                    // 没有待支付订单，错误
                    System.out.println("have no wait pay order, error! ... " + reqParams.get("out_trade_no"));
                } else {
                    if(info.getString("return_code").equals("WAIT_PAY")) {
                        // 有待支付订单，更新数据
                        orderInfo.setObjectId(info.getObjectId());
                        orderInfo.save();

                        System.out.println("order update  ... " + reqParams.get("out_trade_no"));
                    } else {
                        System.out.println("order already update  ... " + reqParams.get("out_trade_no"));
                    }
                }
            } catch (AVException e){
                System.out.println("order save error");
            }

            System.out.println("pay return_code = " + reqParams.get("return_code"));
            System.out.println("pay result_code = " + reqParams.get("result_code"));

            if(reqParams.get("return_code").equals("SUCCESS") && reqParams.get("result_code").equals("SUCCESS")) {
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

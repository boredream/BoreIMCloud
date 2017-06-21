package cn.leancloud.demo.todo;

import cn.leancloud.EngineFunction;
import cn.leancloud.EngineFunctionParam;
import cn.leancloud.demo.todo.pay.OrderSignInfo;
import cn.leancloud.demo.todo.pay.PayUtils;
import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVUser;

public class Cloud {

    @EngineFunction("hello")
    public static String hello() {
        AVUser user = new AVUser();
        // body为userid
        user.setObjectId("58589928b123db00658a6b9f");
        user.increment("money", 10.0f);
        return "udpate success";
    }

    /**
     * 获取签名后订单信息
     *
     * @param total_amount
     * @param subject
     * @param body
     * @param rechargeType     1-支付宝 2-微信
     * @param goodsType     商品类型 1-充值 2-买书
     * @param bookid     买书时传入, 书籍id
     * @param userid
     * @param spbill_create_ip
     * @param money
     */
    @EngineFunction("getOrderSign")
    public static OrderSignInfo getOrderSign(@EngineFunctionParam("total_amount") float total_amount,
                                             @EngineFunctionParam("subject") String subject,
                                             @EngineFunctionParam("body") String body,
                                             @EngineFunctionParam("rechargeType") int rechargeType,
                                             @EngineFunctionParam("goodsType") int goodsType,
                                             @EngineFunctionParam("userid") String userid,
                                             @EngineFunctionParam("bookid") String bookid,
                                             @EngineFunctionParam("spbill_create_ip") String spbill_create_ip,
                                             @EngineFunctionParam("money") float money) {
        return PayUtils.getInstance().getPaySignInfo(total_amount, subject, body, rechargeType, goodsType, userid, bookid, money, spbill_create_ip);
    }

}

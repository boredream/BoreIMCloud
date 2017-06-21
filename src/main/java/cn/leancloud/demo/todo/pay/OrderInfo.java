package cn.leancloud.demo.todo.pay;

import com.avos.avoscloud.AVClassName;
import com.avos.avoscloud.AVObject;

@AVClassName("OrderInfo")
public class OrderInfo extends AVObject{
    public static final String TRADE_SUCCESS = "TRADE_SUCCESS";
}

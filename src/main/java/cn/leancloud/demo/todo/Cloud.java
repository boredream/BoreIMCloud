package cn.leancloud.demo.todo;

import cn.leancloud.EngineFunction;
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

}

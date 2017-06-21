# coding: utf-8

from leancloud import Engine
from leancloud import LeanEngineError

from app import app

import urllib2
import uuid
import time
import hashlib

# 需要重定向到 HTTPS 可去除下一行的注释。
# app = HttpsRedirectMiddleware(app)
engine = Engine(app)

from rongcloud import RongCloud

rcloud = RongCloud('vnroth0kr97so', '11sOK84w1p')

@engine.on_login
def login(user):
    print 'on login:', user

    # 获取token
    response = rcloud.User.getToken(
        userId=user.get("username"),
        name=user.get("nickname"),
        portraitUri=user.get("avatarUrl"))
    if response.ok:
        token = response.result.get("token")
        user["token"] = token
        print "get token success = " + token
        print 'im login:', user
    else:
        raise LeanEngineError('get token error')




@engine.define
def hello(**params):
    if 'name' in params:
        return 'Hello, {}!'.format(params['name'])
    else:
        return 'Hello, LeanCloud!'


@engine.before_save('Todo')
def before_todo_save(todo):
    content = todo.get('content')
    if not content:
        raise LeanEngineError('内容不能为空')
    if len(content) >= 240:
        todo.set('content', content[:240] + ' ...')
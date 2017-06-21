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

@engine.on_login
def login(user):
    print 'ong login:', user

    app_key = "vnroth0kr97so"
    nonce = str(uuid.uuid1())
    timestamp = str(int(time.time() * 1000))
    total_str = app_key + nonce + timestamp
    signature = hashlib.sha1(total_str).hexdigest()

    headers = {
        "RC-App-Key": app_key,
        "RC-Nonce": nonce,
        "RC-Timestamp": timestamp,
        "RC-Signature": signature,
        "Content-Type": "application/json"
    }

    data = {
        "userId": user.get("username"),
        "name": user.get("nickname"),
        "portraitUri": user.get("avatarUrl")
    }
    ry_token_url = "http://api.cn.ronghub.com/user/getToken.json"
    request = urllib2.Request(ry_token_url, headers=headers, data=data)
    content = urllib2.urlopen(request).read()

    print str(content)





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

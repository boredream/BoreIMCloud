# coding: utf-8

from leancloud import Engine
from leancloud import LeanEngineError
from leancloud import LeanCloudError
import leancloud
import json

from app import app

# 需要重定向到 HTTPS 可去除下一行的注释。
# app = HttpsRedirectMiddleware(app)
engine = Engine(app)

from rongcloud import RongCloud

rcloud = RongCloud('vnroth0kr97so', '11sOK84w1p')


@engine.define
def imlogin(**params):
    user = leancloud.User()

    username = params.get('username')
    password = params.get('password')

    try:
        user.login(username, password)

        # 获取token
        response = rcloud.User.getToken(
            userId = user.get('username'),
            name = user.get('nickname'),
            portraitUri = user.get('avatarUrl'))
        if response.ok:
            token = response.result.get("imToken")
            user.set("imToken", token)
            user.save()
            print 'im login success ', user._attributes
            return user._attributes
        else:
            print 'im login error : get token error'
            raise LeanEngineError('get token error')
    except LeanCloudError, e:
        print 'im login error '
        raise LeanEngineError(e.code, e.error)

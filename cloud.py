# coding: utf-8

from leancloud import Engine
from leancloud import LeanEngineError
from leancloud import LeanCloudError
import leancloud

from app import app

# 需要重定向到 HTTPS 可去除下一行的注释。
# app = HttpsRedirectMiddleware(app)
engine = Engine(app)

from rongcloud import RongCloud

rcloud = RongCloud('vnroth0kr97so', '11sOK84w1p')


# @engine.on_login
# def on_login(user):
#     if user.get("imToken"):
#         print 'has im token'
#     else:
#         print 'has not im token, do im login'
#         response = rcloud.User.getToken(
#             userId=user.get('username'),
#             name=user.get('nickname'),
#             portraitUri=user.get('avatarUrl'))
#         if response.ok:
#             token = response.result.get("token")
#             print 'get im token success ', token
#             user.set("imToken", token)
#             user.save()
#         else:
#             print 'im login error : get token error'
#             raise LeanEngineError('get token error')


@engine.define
def imlogin(**params):
    user = leancloud.User()

    try:
        sessionToken = params.get('sessionToken')
        if sessionToken:
            print 'session login'
            user = user.become(sessionToken)
        else:
            print 'username password login'
            username = params.get('username')
            password = params.get('password')
            user.login(username, password)

        if user.get("imToken"):
            print 'has im token'
        else:
            print 'has not im token, do im login'
            response = rcloud.User.getToken(
                userId=user.get('username'),
                name=user.get('nickname'),
                portraitUri=user.get('avatarUrl'))
            if response.ok:
                token = response.result.get("token")
                print 'get im token success ', token
                user.set("imToken", token)
                user.save()
            else:
                print 'im login error : get token error'
                raise LeanEngineError('get token error')

        return_user = user.dump()
        return_user['sessionToken'] = user._session_token
        return return_user

    except LeanCloudError, e:
        print 'im login error '
        raise LeanEngineError(e.code, e.error)

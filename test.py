# coding: utf-8

from rongcloud import RongCloud

rcloud = RongCloud('vnroth0kr97so', '11sOK84w1p')

class User():

    def __init__(self, username='', nickname='', avatarUrl=''):
        self._username = username
        self._nickname = nickname
        self._avatarUrl = avatarUrl

user = User()
user._username = "123"
user._nickname = "nnnnnnnnick"
user._avatarUrl = "http://"

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
    print 'get token error'
# coding: utf-8

from rongcloud import RongCloud

rcloud = RongCloud('vnroth0kr97so', '11sOK84w1p')

r = rcloud.User.getToken(
    userId='userId1',
    name='username',
    portraitUri='http://www.rongcloud.cn/images/logo.png')

if r.ok:
    print r.result.get("token")
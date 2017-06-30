# coding: utf-8

import leancloud
from leancloud import LeanCloudError

user = leancloud.User()
try:
    user.login("tom", "cat")
except LeanCloudError, e:
    print e
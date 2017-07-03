# coding: utf-8

import leancloud
from leancloud import LeanCloudError

APP_ID = "kCb4hozHVvHQIYbL9xf2rdRi-gzGzoHsz"
APP_KEY = "teykeivIf7fbObEo1TEvredO"
MASTER_KEY = "j7Ncx5DPSokvrSQAuShvJ788"
leancloud.init(APP_ID, app_key=APP_KEY, master_key=MASTER_KEY)

user = leancloud.User()
try:
    user = user.become("0ciu07qz3xspnw14gpi1v0c0n")
except LeanCloudError,e:
    print e

print user.__dict__
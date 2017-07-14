# coding: utf-8

import leancloud
from leancloud import LeanEngineError
from leancloud import LeanCloudError


APP_ID = "kCb4hozHVvHQIYbL9xf2rdRi-gzGzoHsz"
APP_KEY = "teykeivIf7fbObEo1TEvredO"
MASTER_KEY = "j7Ncx5DPSokvrSQAuShvJ788"
leancloud.init(APP_ID, app_key=APP_KEY, master_key=MASTER_KEY)
leancloud.use_master_key(True)

srcUserId = '5949e37fda2f6000679a9b74'
tarUserId = '1'


def test_friend_request():
    srcUser = leancloud.User.create_without_data(srcUserId)
    tarUser = leancloud.User.create_without_data(tarUserId)

    # src -> target
    query_src = leancloud.Query(query_class='FriendRelation')\
        .equal_to(key='srcUser', value=srcUser)
    query_tar = leancloud.Query(query_class='FriendRelation')\
        .equal_to(key='tarUser', value=tarUser)
    result = None
    try:
        result = leancloud.Query(query_class='FriendRelation')\
            .and_(query_src, query_tar).first()
    except LeanCloudError, e:
        if e.code != 101:
            raise e

    # target -> src
    query_src_rev = leancloud.Query(query_class='FriendRelation')\
        .equal_to(key='srcUser', value=tarUser)
    query_tar_rev = leancloud.Query(query_class='FriendRelation')\
        .equal_to(key='tarUser', value=srcUser)
    result_rev = None
    try:
        result_rev = leancloud.Query(query_class='FriendRelation')\
            .and_(query_src_rev, query_tar_rev).first()
    except LeanCloudError, e:
        if e.code != 101:
            raise e

    if (result and result.get('relation') == 1) or \
            (result_rev and result_rev.get('relation') == 1):
        # 如果已经是好友，则返回已添加提示
        print '[src=' + srcUserId + '] and [tar=' + tarUserId + '] is already friend'
        raise LeanEngineError(u'已经是好友了，无需重复添加')

    if result and result.get('relation') == -1:
        # 如果src曾经向tar提交过申请，也视为成功申请
        # 但是数据库中不再重复添加好友关系
        print '[src='+srcUserId+'] has requested friend to [tar='+tarUserId+'] before'
        return

    if result_rev and result_rev.get('relation') == -1:
        # 如果是tar已经向src提交过申请，则直接relation=1双方改为好友
        result_rev.set('relation', 1)
        result_rev.save()
        print '[tar='+tarUserId+'] has requested friend to [src='+srcUserId+'], be friend now'
        return

    # 如果双方没关系，则新建一条src申请加tar为好友的关系数据
    relation = leancloud.Object().create(class_name='FriendRelation')
    relation.set('srcUser', srcUser)
    relation.set('tarUser', tarUser)
    relation.save()
    print '[src='+srcUserId+'] request friend to [tar='+tarUserId+']'

    # 同时向tar发送一条IM系统消息“src申请添加您为好友”


def get_friends():
    srcUser = leancloud.User.create_without_data(srcUserId)

    query_src = leancloud.Query(query_class='FriendRelation') \
        .equal_to(key='srcUser', value=srcUser)
    query_tar = leancloud.Query(query_class='FriendRelation') \
        .equal_to(key='tarUser', value=srcUser)
    query_relation = leancloud.Query(query_class='FriendRelation') \
        .equal_to(key='relation', value=1)
    query_id = leancloud.Query(query_class='FriendRelation') \
        .or_(query_src, query_tar)
    query = leancloud.Query(query_class='FriendRelation') \
        .and_(query_id, query_relation)\
        .include('srcUser', 'tarUser')

    users = []
    try:
        for result in query.find():
            src = result.get('srcUser').dump()
            tar = result.get('tarUser').dump()
            if src.get('objectId') == srcUserId:
                users.append(tar)
            else:
                users.append(src)
    except LeanCloudError:
        print 'no friends'

    for user in users:
        print user

    return users


if __name__ == '__main__':
    get_friends()




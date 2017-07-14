# coding: utf-8

from leancloud import Engine
from leancloud import LeanEngineError
from leancloud import LeanCloudError
import leancloud
from pypinyin import lazy_pinyin

from app import app

# 需要重定向到 HTTPS 可去除下一行的注释。
# app = HttpsRedirectMiddleware(app)
engine = Engine(app)

from rongcloud import RongCloud

rcloud = RongCloud('vnroth0kr97so', '11sOK84w1p')


@engine.before_save('_User')
def before_save_user(user):
    # save user 即注册创建用户的时候，如果没有nickname，默认设置为username
    nickname = user.get('nickname')
    if not nickname:
        nickname = user.get('username')
        user.set('nickname', nickname)

    letter = lazy_pinyin(nickname)
    print 'create user , generate letter : ', letter
    user.set('letter', ' '.join(letter))


@engine.before_update('_User')
def before_update_user(user):
    # 修改用户的时候，注意更新昵称拼音
    letter = lazy_pinyin(user.get('nickname'))
    print 'update user , generate letter : ', letter
    user.set('letter', ' '.join(letter))
    user.save()


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
            # TODO 啥时候更新im的信息？
            print 'has not im token, do im login'
            response = rcloud.User.getToken(
                userId=user.get('objectId'),
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
        lee = LeanEngineError(e.code, e.error)
        print 'im login error '
        print lee.__dict__
        raise lee


@engine.define
def getFriends():
    user = engine.current.user
    srcUserId = user.get('objectId')
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
        .and_(query_id, query_relation) \
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

    return users


@engine.define
def get_friend_requests():
    user = engine.current.user
    srcUserId = user.get('objectId')
    srcUser = leancloud.User.create_without_data(srcUserId)

    # 两种情况：对方加我了，显示“同意”、同意后显示“已添加”
    # 注意，发起方不显示好友请求列表，接收方才显示
    query_tar = leancloud.Query(query_class='FriendRelation') \
        .equal_to(key='tarUser', value=srcUser)
    query_request = leancloud.Query(query_class='FriendRelation') \
        .equal_to(key='relation', value=-1)
    query = leancloud.Query(query_class='FriendRelation') \
        .and_(query_tar, query_request) \
        .include('srcUser')

    users = []
    try:
        for result in query.find():
            tar = result.get('srcUser').dump()
            users.append(tar)
    except LeanCloudError:
        print 'no friend requests'

    return users


@engine.define
def friendRequest(**params):
    user = engine.current.user
    srcUserId = user.get('objectId')
    tarUserId = params.get('userId')

    srcUser = leancloud.User.create_without_data(srcUserId)
    tarUser = leancloud.User.create_without_data(tarUserId)

    # src -> target
    query_src = leancloud.Query(query_class='FriendRelation') \
        .equal_to(key='srcUser', value=srcUser)
    query_tar = leancloud.Query(query_class='FriendRelation') \
        .equal_to(key='tarUser', value=tarUser)
    result = None
    try:
        result = leancloud.Query(query_class='FriendRelation') \
            .and_(query_src, query_tar).first()
    except LeanCloudError, e:
        if e.code != 101:
            raise e

    # target -> src
    query_src_rev = leancloud.Query(query_class='FriendRelation') \
        .equal_to(key='srcUser', value=tarUser)
    query_tar_rev = leancloud.Query(query_class='FriendRelation') \
        .equal_to(key='tarUser', value=srcUser)
    result_rev = None
    try:
        result_rev = leancloud.Query(query_class='FriendRelation') \
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
        print '[src=' + srcUserId + '] has requested friend to [tar=' + tarUserId + '] before'
        return

    if result_rev and result_rev.get('relation') == -1:
        # 如果是tar已经向src提交过申请，则直接relation=1双方改为好友
        result_rev.set('relation', 1)
        result_rev.save()
        print '[tar=' + tarUserId + '] has requested friend to [src=' + srcUserId + '], be friend now'
        return

    # 如果双方没关系，则新建一条src申请加tar为好友的关系数据
    relation = leancloud.Object().create(class_name='FriendRelation')
    relation.set('srcUser', srcUser)
    relation.set('tarUser', tarUser)
    relation.save()
    print '[src=' + srcUserId + '] request friend to [tar=' + tarUserId + ']'

    # 同时向tar发送一条IM系统消息“src申请添加您为好友”
    message = u'申请添加您为好友'
    rcloud.Message.PublishSystem(
        fromUserId=srcUserId,
        toUserId={tarUserId},
        objectName='RC:TxtMsg',
        content="{\"content\":\""+message+"\",\"extra\":\"helloExtra\"}",
        pushContent='thisisapush',
        pushData="{\"pushData\":\""+message+"\"}",
        isPersisted='0',
        isCounted='0')


@engine.define
def apply_friend_request(**params):
    user = engine.current.user
    srcUserId = user.get('objectId')
    tarUserId = params.get('userId')
    curUser = leancloud.User.create_without_data(srcUserId)
    tarUser = leancloud.User.create_without_data(tarUserId)

    # 当前用户是被动方
    query_src = leancloud.Query(query_class='FriendRelation') \
        .equal_to(key='srcUser', value=tarUser)
    query_tar = leancloud.Query(query_class='FriendRelation') \
        .equal_to(key='tarUser', value=curUser)
    result = leancloud.Query(query_class='FriendRelation') \
        .and_(query_src, query_tar).first()

    if result.get('relation') == -1:
        # 如果已经存在请求，同意为好友
        result.set('relation', 1)
        result.save()
    else:
        raise LeanEngineError(u'已经是好友了，无需同意添加')
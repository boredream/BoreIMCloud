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
def friendRequest(**params):
    user = engine.current.user
    srcUserId = user.get('objectId')
    targetUserId = params.get('userId')

    # src -> target
    query_src = leancloud.Query(query_class='FriendRelation')
    query_tar = leancloud.Query(query_class='FriendRelation')
    query = leancloud.Query(query_class='FriendRelation')

    query_src.equal_to(key='srcUserId', value=srcUserId)
    query_tar.equal_to(key='targetUserId', value=targetUserId)
    result = None
    try:
        result = query.and_(query_src, query_tar).first()
    except LeanCloudError, e:
        if e.code != 101:
            raise e

    # target -> src
    query_src_rev = leancloud.Query(query_class='FriendRelation')
    query_tar_rev = leancloud.Query(query_class='FriendRelation')
    query_rev = leancloud.Query(query_class='FriendRelation')

    query_src_rev.equal_to(key='srcUserId', value=targetUserId)
    query_tar_rev.equal_to(key='targetUserId', value=srcUserId)
    result_rev = None
    try:
        result_rev = query_rev.and_(query_src_rev, query_tar_rev).first()
    except LeanCloudError, e:
        if e.code != 101:
            raise e

    if (result and result.get('relation') == 1) or \
            (result_rev and result_rev.get('relation') == 1):
        # 如果已经是好友，则返回已添加提示
        print '[src=' + srcUserId + '] and [tar=' + targetUserId + '] is already friend'
        raise LeanEngineError(u'已经是好友了，无需重复添加')

    if result and result.get('relation') == -1:
        # 如果src曾经向tar提交过申请，也视为成功申请
        # 但是数据库中不再重复添加好友关系
        print '[src='+srcUserId+'] has requested friend to [tar='+targetUserId+'] before'
        return

    if result_rev and result_rev.get('relation') == -1:
        # 如果是tar已经向src提交过申请，则直接relation=1双方改为好友
        result_rev.set('relation', 1)
        result_rev.save()
        print '[tar='+targetUserId+'] has requested friend to [src='+srcUserId+'], be friend now'
        return

    # 如果双方没关系，则新建一条src申请加tar为好友的关系数据
    relation = leancloud.Object().create(class_name='FriendRelation')
    relation.set('srcUserId', srcUserId)
    relation.set('targetUserId', targetUserId)
    relation.save()
    print '[src='+srcUserId+'] request friend to [tar='+targetUserId+']'

    # 同时向tar发送一条IM系统消息“src申请添加您为好友”
    message = u'申请添加您为好友'
    rcloud.Message.PublishSystem(
        fromUserId=srcUserId,
        toUserId={targetUserId},
        objectName='RC:TxtMsg',
        content="{\"content\":\""+message+"\",\"extra\":\"helloExtra\"}",
        pushContent='thisisapush',
        pushData="{\"pushData\":\""+message+"\"}",
        isPersisted='0',
        isCounted='0')

    print 'friend request '+srcUserId+' -> '+targetUserId



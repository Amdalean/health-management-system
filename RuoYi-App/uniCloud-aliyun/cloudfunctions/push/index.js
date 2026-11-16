'use strict';
const uniPush = uniCloud.getPushManager({appId: "__UNI__73E64E4"});

exports.main = async (event, context) => {
    try {
        const body = typeof event.body === 'string' ? JSON.parse(event.body) : event;
        const { type, title, content, payload, cids, request_id } = body;

        if (!title || !content) {
            return { code: -1, msg: "title 和 content 不能为空" };
        }

        // 构建推送参数
        const msg = {
            title,
            content,
            payload: payload ? JSON.parse(payload) : {},
            force_notification: true,
            request_id: request_id || Date.now().toString(36) + Math.random().toString(36).substr(2, 9),
            settings: {
                ttl: 86400000  // 离线消息有效期 1 天
            },
            category: {
                harmony: "MARKETING"
            },
            options: {
                android: {
                    HW: { "/message/android/target_user_type": 1 },
                    HO: { "/android/targetUserType": 1 },
                    VV: { "/pushMode": 1 }
                },
                harmony: {
                    HW: { "/pushOptions/testMessage": true }
                }
            }
        };

        let res;
        if (type === 'all') {
            msg.push_clientid = "all";
            res = await uniPush.sendMessage(msg);
        } else if (type === 'single') {
            if (!cids || !Array.isArray(cids) || cids.length === 0) {
                return { code: -1, msg: "type=single 时 cids 必须是非空数组" };
            }
            msg.push_clientid = cids;
            res = await uniPush.sendMessage(msg);
        } else {
            return { code: -1, msg: "type 参数错误，必须是 'single' 或 'all'" };
        }

        return { code: 0, msg: "success", data: res };

    } catch (e) {
        console.error('[push] 推送失败:', e);
        return { code: -1, msg: e.message || "推送失败" };
    }
};

package com.trade.beauty.chatmo.frg;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Pair;
import android.view.View;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMConversation;
import com.hyphenate.chat.EMMessage;
import com.trade.beauty.chatmo.Constants;
import com.trade.beauty.chatmo.act.ChatActivity;
import com.trade.beauty.chatmo.adapter.ConverAdapter;
import com.trade.beauty.chatmo.base.BaseFragment;
import com.trade.beauty.chatmo.easyutils.EasyUtil;
import com.trade.beauty.chatmo.easyutils.emlisenter.MyEMMessageListener;
import com.trade.beauty.mylibrary.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Created by zhangzz on 2018/9/21
 */
public class ConversationFrg extends BaseFragment {
    private RecyclerView mRecyclerView;
    private BaseQuickAdapter converAdapter;
    private MyEMMessageListener myEMMessageListener;
    private List<EMConversation> list = new ArrayList<>();

    @Override
    protected void onCreateV(Bundle savedInstanceState) {
        myEMMessageListener = new MyEMMessageListener() {
            @Override
            public void onMessageReceived(List<EMMessage> messages) {
                super.onMessageReceived(messages);
                // notify new message
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        list.clear();
                        list = loadConversationList();
                        converAdapter.setNewData(list);
                    }
                });
            }
        };
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_conversation;
    }

    @Override
    public void onResume() {
        super.onResume();
        EasyUtil.getEmManager().addMessageListener(myEMMessageListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        EasyUtil.getEmManager().removeMessageListener(myEMMessageListener);
    }

    @Override
    protected void initView() {
        mRecyclerView = fragmentView.findViewById(R.id.rv_force_record);
    }

    @Override
    protected void initData() {
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setNestedScrollingEnabled(false);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        converAdapter = new ConverAdapter(R.layout.layout_records_item);
        converAdapter.openLoadAnimation();
        mRecyclerView.setAdapter(converAdapter);
    }

    @Override
    protected void initEvent() {
        converAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                if (list != null && list.size() > 0) {
                    Intent intent = new Intent(getActivity(), ChatActivity.class);
                    intent.putExtra(Constants.EXTRA_USER_ID, list.get(position).conversationId());
                    intent.putExtra(Constants.EXTRA_CHAT_TYPE, Constants.CHATTYPE_GROUP);
                    startActivity(intent);
                }
            }
        });
    }

    protected List<EMConversation> loadConversationList() {
        // get all conversations
        Map<String, EMConversation> conversations = EMClient.getInstance().chatManager().getAllConversations();
        List<Pair<Long, EMConversation>> sortList = new ArrayList<Pair<Long, EMConversation>>();
        /**
         * lastMsgTime will change if there is new message during sorting
         * so use synchronized to make sure timestamp of last message won't change.
         */
        synchronized (conversations) {
            for (EMConversation conversation : conversations.values()) {
                if (conversation.getAllMessages().size() != 0) {
                    sortList.add(new Pair<Long, EMConversation>(conversation.getLastMessage().getMsgTime(), conversation));
                }
            }
        }
        try {
            // Internal is TimSort algorithm, has bug
            sortConversationByLastChatTime(sortList);
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<EMConversation> list = new ArrayList<EMConversation>();
        for (Pair<Long, EMConversation> sortItem : sortList) {
            list.add(sortItem.second);
        }
        return list;
    }

    private void sortConversationByLastChatTime(List<Pair<Long, EMConversation>> conversationList) {
        Collections.sort(conversationList, new Comparator<Pair<Long, EMConversation>>() {
            @Override
            public int compare(final Pair<Long, EMConversation> con1, final Pair<Long, EMConversation> con2) {

                if (con1.first.equals(con2.first)) {
                    return 0;
                } else if (con2.first.longValue() > con1.first.longValue()) {
                    return 1;
                } else {
                    return -1;
                }
            }

        });
    }
}

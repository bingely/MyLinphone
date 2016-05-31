package org.linphone;
/*
HistoryFragment.java
Copyright (C) 2012  Belledonne Communications, Grenoble, France

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
*/

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.linphone.core.CallDirection;
import org.linphone.core.LinphoneAddress;
import org.linphone.core.LinphoneCallLog;

import java.util.Arrays;
import java.util.List;

/**
 * @author Sylvain Berfini
 */
public class HistorySimpleFragment2 extends Fragment implements OnClickListener, AdapterView.OnItemClickListener {
    private ListView historyList;
    private LayoutInflater mInflater;
    private TextView allCalls, missedCalls, edit, ok, deleteAll, noCallHistory, noMissedCallHistory;
    private List<LinphoneCallLog> mLogs;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mInflater = inflater;
        View view = inflater.inflate(R.layout.history_simple, container, false);

        init(view);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // 获得拨打电话记录
        mLogs = Arrays.asList(LinphoneManager.getLc().getCallLogs());
        historyList.setAdapter(new CallHistoryAdapter(getActivity()));
    }

    private void init(View view) {
        noCallHistory = (TextView) view.findViewById(R.id.noCallHistory);
        noMissedCallHistory = (TextView) view.findViewById(R.id.noMissedCallHistory);

        historyList = (ListView) view.findViewById(R.id.historyList);
        historyList.setOnItemClickListener(this);
        registerForContextMenu(historyList);

        deleteAll = (TextView) view.findViewById(R.id.deleteAll);
        deleteAll.setOnClickListener(this);
        deleteAll.setVisibility(View.INVISIBLE);

        allCalls = (TextView) view.findViewById(R.id.allCalls);
        allCalls.setOnClickListener(this);

        missedCalls = (TextView) view.findViewById(R.id.missedCalls);
        missedCalls.setOnClickListener(this);

        allCalls.setEnabled(false);

        edit = (TextView) view.findViewById(R.id.edit);
        edit.setOnClickListener(this);

        ok = (TextView) view.findViewById(R.id.ok);
        ok.setOnClickListener(this);
    }


    @Override
    public void onClick(View v) {

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (LinphoneActivity.isInstanciated()) {
            LinphoneCallLog log = mLogs.get(position);
            LinphoneAddress address;
            if (log.getDirection() == CallDirection.Incoming) {
                address = log.getFrom();
            } else {
                address = log.getTo();
            }
            LinphoneActivity.instance().setAddresGoToDialerAndCall(address.asStringUriOnly(), address.getDisplayName(), null);
        }
    }





    private class CallHistoryAdapter extends BaseAdapter {

        public CallHistoryAdapter(FragmentActivity activity) {

        }

        @Override
        public int getCount() {
            return mLogs.size();
        }

        @Override
        public Object getItem(int position) {
            return mLogs.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = null;
            if (convertView != null) {
                view = convertView;
            } else {
                view = mInflater.inflate(R.layout.history_cell_simple, parent,false);
            }
            // 获得每个位置的
            final LinphoneCallLog log = mLogs.get(position);
            final LinphoneAddress address;

            TextView contact = (TextView) view.findViewById(R.id.sipUri);


            if (log.getDirection() == CallDirection.Incoming) {
                address = log.getFrom();
            } else {
                address = log.getTo();
            }

            // 根据地址找到相应的联系人
            Contact c = ContactsManager.getInstance().findContactWithAddress(address);
            String displayName = null;
            final String sipUri = address.asStringUriOnly();
            if(c != null){
                displayName = c.getName();
            }

            if (displayName == null) {
                if (getResources().getBoolean(R.bool.only_display_username_if_unknown) && LinphoneUtils.isSipAddress(sipUri)) {
                    contact.setText(address.getUserName());
                } else {
                    contact.setText(sipUri);
                }
            } else {
                if (getResources().getBoolean(R.bool.only_display_username_if_unknown) && LinphoneUtils.isSipAddress(address.getDisplayName())) {
                    contact.setText(displayName);
                } else {
                    contact.setText(sipUri);
                }
            }
            view.setTag(sipUri);

            return view;
        }
    }
}

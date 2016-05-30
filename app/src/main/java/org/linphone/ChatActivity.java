package org.linphone;
/*
ChatActivity.java
Copyright (C) 2015  Belledonne Communications, Grenoble, France

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
import android.support.v4.app.FragmentActivity;

/**
 * @author Margaux Clerc
 */
public class ChatActivity extends FragmentActivity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.chat_activity);
		
		Bundle extras = new Bundle();
		extras.putString("SipUri", getIntent().getExtras().getString("SipUri"));
		extras.putString("DisplayName", getIntent().getExtras().getString("DisplayName"));
		extras.putString("PictureUri", getIntent().getExtras().getString("PictureUri"));
		
		ChatFragment fragment = new ChatFragment();
		fragment.setArguments(extras);
		getSupportFragmentManager().beginTransaction().add(R.id.fragmentContainer, fragment, "ChatFragment").commit();
	}
}


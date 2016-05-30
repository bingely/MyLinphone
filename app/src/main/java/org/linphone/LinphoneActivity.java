package org.linphone;

/*
 LinphoneActivity.java
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
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.Fragment.SavedState;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.linphone.LinphoneManager.AddressType;
import org.linphone.compatibility.Compatibility;
import org.linphone.core.CallDirection;
import org.linphone.core.LinphoneAddress;
import org.linphone.core.LinphoneAuthInfo;
import org.linphone.core.LinphoneCall;
import org.linphone.core.LinphoneCall.State;
import org.linphone.core.LinphoneCallLog;
import org.linphone.core.LinphoneCallLog.CallStatus;
import org.linphone.core.LinphoneChatMessage;
import org.linphone.core.LinphoneChatRoom;
import org.linphone.core.LinphoneCore;
import org.linphone.core.LinphoneCore.RegistrationState;
import org.linphone.core.LinphoneCoreException;
import org.linphone.core.LinphoneCoreFactory;
import org.linphone.core.LinphoneCoreListenerBase;
import org.linphone.core.LinphoneProxyConfig;
import org.linphone.mediastream.Log;
import org.linphone.setup.RemoteProvisioningLoginActivity;
import org.linphone.setup.SetupActivity;
import org.linphone.ui.AddressText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;

import static android.content.Intent.ACTION_MAIN;

/**
 * @author Sylvain Berfini
 */
public class LinphoneActivity extends FragmentActivity implements OnClickListener, ContactPicked {
	public static final String PREF_FIRST_LAUNCH = "pref_first_launch";
	private static final int SETTINGS_ACTIVITY = 123;
	private static final int FIRST_LOGIN_ACTIVITY = 101;
	private static final int REMOTE_PROVISIONING_LOGIN_ACTIVITY = 102;
	private static final int CALL_ACTIVITY = 19;
    private static final int CHAT_ACTIVITY = 21;

	private static LinphoneActivity instance;

	private StatusFragment statusFragment;
	private TextView missedCalls, missedChats;
	private ImageView dialer;
	private LinearLayout menu, mark;
	private RelativeLayout contacts, history, settings, chat, aboutChat, aboutSettings;
	private FragmentsAvailable currentFragment, nextFragment;
	private List<FragmentsAvailable> fragmentsHistory;
	private Fragment dialerFragment, messageListFragment, friendStatusListenerFragment;
	private SavedState dialerSavedState;
	private boolean isAnimationDisabled = false, preferLinphoneContacts = false;
	private OrientationEventListener mOrientationHelper;
	private LinphoneCoreListenerBase mListener;

	static final boolean isInstanciated() {
		return instance != null;
	}

	public static final LinphoneActivity instance() {
		if (instance != null)
			return instance;
		throw new RuntimeException("LinphoneActivity not instantiated yet");
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (isTablet() && getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
        	setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else if (!isTablet() && getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
        	setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

		if (!LinphoneManager.isInstanciated()) {
			Log.e("No service running: avoid crash by starting the launcher", this.getClass().getName());
			// super.onCreate called earlier
			finish();
			startActivity(getIntent().setClass(this, LinphoneLauncherActivity.class));
			return;
		}

		boolean useFirstLoginActivity = getResources().getBoolean(R.bool.display_account_wizard_at_first_start);
		if (LinphonePreferences.instance().isProvisioningLoginViewEnabled()) {
			Intent wizard = new Intent();
			wizard.setClass(this, RemoteProvisioningLoginActivity.class);
			wizard.putExtra("Domain", LinphoneManager.getInstance().wizardLoginViewDomain);
			startActivityForResult(wizard, REMOTE_PROVISIONING_LOGIN_ACTIVITY);
		} else if (useFirstLoginActivity && LinphonePreferences.instance().isFirstLaunch()) {
			if (LinphonePreferences.instance().getAccountCount() > 0) {
				LinphonePreferences.instance().firstLaunchSuccessful();
			} else {
				startActivityForResult(new Intent().setClass(this, SetupActivity.class), FIRST_LOGIN_ACTIVITY);
			}
		}

		ContactsManager.getInstance().initializeSyncAccount(getApplicationContext(), getContentResolver());

	 	if(!LinphonePreferences.instance().isContactsMigrationDone()){
			ContactsManager.getInstance().migrateContacts();
			LinphonePreferences.instance().contactsMigrationDone();
		}

		setContentView(R.layout.main);
		instance = this;
		fragmentsHistory = new ArrayList<FragmentsAvailable>();
		initButtons();

		currentFragment = nextFragment = FragmentsAvailable.DIALER;
		fragmentsHistory.add(currentFragment);
		if (savedInstanceState == null) {
			if (findViewById(R.id.fragmentContainer) != null) {
				dialerFragment = new DialerFragment();
				dialerFragment.setArguments(getIntent().getExtras());
				getSupportFragmentManager().beginTransaction().add(R.id.fragmentContainer, dialerFragment, currentFragment.toString()).commit();
				selectMenu(FragmentsAvailable.DIALER);
			}
		}

		mListener = new LinphoneCoreListenerBase(){
			@Override
			public void messageReceived(LinphoneCore lc, LinphoneChatRoom cr, LinphoneChatMessage message) {
		        displayMissedChats(getChatStorage().getUnreadMessageCount());
		        if (messageListFragment != null && messageListFragment.isVisible()) {
		            ((ChatListFragment) messageListFragment).refresh();
		        }
			}

			@Override
			public void registrationState(LinphoneCore lc, LinphoneProxyConfig proxy, LinphoneCore.RegistrationState state, String smessage) {
				if (state.equals(RegistrationState.RegistrationCleared)) {
					if (lc != null) {
						LinphoneAuthInfo authInfo = lc.findAuthInfo(proxy.getIdentity(), proxy.getRealm(), proxy.getDomain());
						if (authInfo != null)
							lc.removeAuthInfo(authInfo);
					}
				}
			}

			@Override
			public void callState(LinphoneCore lc, LinphoneCall call, LinphoneCall.State state, String message) {
				if (state == State.IncomingReceived) {
					startActivity(new Intent(LinphoneActivity.instance(), IncomingCallActivity.class));
				} else if (state == State.OutgoingInit) {
					if (call.getCurrentParamsCopy().getVideoEnabled()) {
						startVideoActivity(call);
					} else {
						startIncallActivity(call);
					}
				} else if (state == State.CallEnd || state == State.Error || state == State.CallReleased) {
					// Convert LinphoneCore message for internalization
					if (message != null && message.equals("Call declined.")) {
						displayCustomToast(getString(R.string.error_call_declined), Toast.LENGTH_LONG);
					} else if (message != null && message.equals("Not Found")) {
						displayCustomToast(getString(R.string.error_user_not_found), Toast.LENGTH_LONG);
					} else if (message != null && message.equals("Unsupported media type")) {
						displayCustomToast(getString(R.string.error_incompatible_media), Toast.LENGTH_LONG);
					} else if (message != null && state == State.Error) {
						displayCustomToast(getString(R.string.error_unknown) + " - " + message, Toast.LENGTH_LONG);
					}
					resetClassicMenuLayoutAndGoBackToCallIfStillRunning();
				}

				int missedCalls = LinphoneManager.getLc().getMissedCallsCount();
				displayMissedCalls(missedCalls);
			}
		};

		LinphoneCore lc = LinphoneManager.getLcIfManagerNotDestroyedOrNull();
		if (lc != null) {
			lc.addListener(mListener);
		}

		int missedCalls = LinphoneManager.getLc().getMissedCallsCount();
		displayMissedCalls(missedCalls);

		int rotation = getWindowManager().getDefaultDisplay().getRotation();
		switch (rotation) {
		case Surface.ROTATION_0:
			rotation = 0;
			break;
		case Surface.ROTATION_90:
			rotation = 90;
			break;
		case Surface.ROTATION_180:
			rotation = 180;
			break;
		case Surface.ROTATION_270:
			rotation = 270;
			break;
		}

		LinphoneManager.getLc().setDeviceRotation(rotation);
		mAlwaysChangingPhoneAngle = rotation;

		updateAnimationsState();
	}


	private void initButtons() {
		menu = (LinearLayout) findViewById(R.id.menu);
		mark = (LinearLayout) findViewById(R.id.mark);

		history = (RelativeLayout) findViewById(R.id.history);
		history.setOnClickListener(this);
		contacts = (RelativeLayout) findViewById(R.id.contacts);
		contacts.setOnClickListener(this);
		dialer = (ImageView) findViewById(R.id.dialer);
		dialer.setOnClickListener(this);
		settings = (RelativeLayout) findViewById(R.id.settings);
		settings.setOnClickListener(this);
		chat = (RelativeLayout) findViewById(R.id.chat);
		chat.setOnClickListener(this);
		aboutChat = (RelativeLayout) findViewById(R.id.about_chat);
		aboutSettings = (RelativeLayout) findViewById(R.id.about_settings);

		if (getResources().getBoolean(R.bool.replace_chat_by_about)) {
			chat.setVisibility(View.GONE);
			chat.setOnClickListener(null);
			findViewById(R.id.completeChat).setVisibility(View.GONE);
			aboutChat.setVisibility(View.VISIBLE);
			aboutChat.setOnClickListener(this);
		}
		if (getResources().getBoolean(R.bool.replace_settings_by_about)) {
			settings.setVisibility(View.GONE);
			settings.setOnClickListener(null);
			aboutSettings.setVisibility(View.VISIBLE);
			aboutSettings.setOnClickListener(this);
		}

		missedCalls = (TextView) findViewById(R.id.missedCalls);
		missedChats = (TextView) findViewById(R.id.missedChats);
	}

	private boolean isTablet() {
		return getResources().getBoolean(R.bool.isTablet);
	}

	public void hideStatusBar() {
		if (isTablet()) {
			return;
		}

		findViewById(R.id.status).setVisibility(View.GONE);
		findViewById(R.id.fragmentContainer).setPadding(0, 0, 0, 0);
	}

	public void showStatusBar() {
		if (isTablet()) {
			return;
		}

		if (statusFragment != null && !statusFragment.isVisible()) {
			// Hack to ensure statusFragment is visible after coming back to
			// dialer from chat
			statusFragment.getView().setVisibility(View.VISIBLE);
		}
		findViewById(R.id.status).setVisibility(View.VISIBLE);
		findViewById(R.id.fragmentContainer).setPadding(0, LinphoneUtils.pixelsToDpi(getResources(), 40), 0, 0);
	}

	private void changeCurrentFragment(FragmentsAvailable newFragmentType, Bundle extras) {
		changeCurrentFragment(newFragmentType, extras, false);
	}

	private void changeCurrentFragment(FragmentsAvailable newFragmentType, Bundle extras, boolean withoutAnimation) {
		if (newFragmentType == currentFragment && newFragmentType != FragmentsAvailable.CHAT) {
			return;
		}
		nextFragment = newFragmentType;

		if (currentFragment == FragmentsAvailable.DIALER) {
			try {
				dialerSavedState = getSupportFragmentManager().saveFragmentInstanceState(dialerFragment);
			} catch (Exception e) {
			}
		}

		Fragment newFragment = null;

		switch (newFragmentType) {
		case HISTORY:
			if (getResources().getBoolean(R.bool.use_simple_history)) {
				newFragment = new HistorySimpleFragment();
			} else {
				newFragment = new HistoryFragment();
			}
			break;
		case HISTORY_DETAIL:
			newFragment = new HistoryDetailFragment();
			break;
		case CONTACTS:
			if (getResources().getBoolean(R.bool.use_android_native_contact_edit_interface)) {
				Intent i = new Intent();
			    i.setComponent(new ComponentName("com.android.contacts", "com.android.contacts.DialtactsContactsEntryActivity"));
			    i.setAction("android.intent.action.MAIN");
			    i.addCategory("android.intent.category.LAUNCHER");
			    i.addCategory("android.intent.category.DEFAULT");
			    startActivity(i);
			} else {
				newFragment = new ContactsFragment();
				friendStatusListenerFragment = newFragment;
			}
			break;
		case CONTACT:
			newFragment = new ContactFragment();
			break;
		case EDIT_CONTACT:
			newFragment = new EditContactFragment();
			break;
		case DIALER:
			newFragment = new DialerFragment();
			if (extras == null) {
				newFragment.setInitialSavedState(dialerSavedState);
			}
			dialerFragment = newFragment;
			break;
		case SETTINGS:
			newFragment = new SettingsFragment();
			break;
		case ACCOUNT_SETTINGS:
			newFragment = new AccountPreferencesFragment();
			break;
		case ABOUT:
		case ABOUT_INSTEAD_OF_CHAT:
		case ABOUT_INSTEAD_OF_SETTINGS:
			newFragment = new AboutFragment();
			break;
		case CHATLIST:
			newFragment = new ChatListFragment();
			messageListFragment = new Fragment();
			break;
		case CHAT:
			newFragment = new ChatFragment();
			break;
		default:
			break;
		}

		if (newFragment != null) {
			newFragment.setArguments(extras);
			if (isTablet()) {
				changeFragmentForTablets(newFragment, newFragmentType, withoutAnimation);
			} else {
				changeFragment(newFragment, newFragmentType, withoutAnimation);
			}
		}
	}

	private void updateAnimationsState() {
		isAnimationDisabled = getResources().getBoolean(R.bool.disable_animations) || !LinphonePreferences.instance().areAnimationsEnabled();
	}

	public boolean isAnimationDisabled() {
		return isAnimationDisabled;
	}

	private void changeFragment(Fragment newFragment, FragmentsAvailable newFragmentType, boolean withoutAnimation) {
		if (statusFragment != null) {
			statusFragment.closeStatusBar();
		}

		FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

		if (!withoutAnimation && !isAnimationDisabled && currentFragment.shouldAnimate()) {
			if (newFragmentType.isRightOf(currentFragment)) {
				transaction.setCustomAnimations(R.anim.slide_in_right_to_left,
						R.anim.slide_out_right_to_left,
						R.anim.slide_in_left_to_right,
						R.anim.slide_out_left_to_right);
			} else {
				transaction.setCustomAnimations(R.anim.slide_in_left_to_right,
						R.anim.slide_out_left_to_right,
						R.anim.slide_in_right_to_left,
						R.anim.slide_out_right_to_left);
			}
		}

		if (newFragmentType != FragmentsAvailable.DIALER
				|| newFragmentType != FragmentsAvailable.ABOUT_INSTEAD_OF_CHAT
				|| newFragmentType != FragmentsAvailable.ABOUT_INSTEAD_OF_SETTINGS
				|| newFragmentType != FragmentsAvailable.CONTACTS
				|| newFragmentType != FragmentsAvailable.CHATLIST
				|| newFragmentType != FragmentsAvailable.HISTORY) {
			transaction.addToBackStack(newFragmentType.toString());
		}
		transaction.replace(R.id.fragmentContainer, newFragment, newFragmentType.toString());
		transaction.commitAllowingStateLoss();
		getSupportFragmentManager().executePendingTransactions();

		currentFragment = newFragmentType;
	}

	private void changeFragmentForTablets(Fragment newFragment, FragmentsAvailable newFragmentType, boolean withoutAnimation) {
//		if (getResources().getBoolean(R.bool.show_statusbar_only_on_dialer)) {
//			if (newFragmentType == FragmentsAvailable.DIALER) {
//				showStatusBar();
//			} else {
//				hideStatusBar();
//			}
//		}
		if (statusFragment != null) {
			statusFragment.closeStatusBar();
		}

		LinearLayout ll = (LinearLayout) findViewById(R.id.fragmentContainer2);

		FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
		if (newFragmentType.shouldAddItselfToTheRightOf(currentFragment)) {
			ll.setVisibility(View.VISIBLE);

			transaction.addToBackStack(newFragmentType.toString());
			transaction.replace(R.id.fragmentContainer2, newFragment);
		} else {
			if (newFragmentType == FragmentsAvailable.DIALER
					|| newFragmentType == FragmentsAvailable.ABOUT
					|| newFragmentType == FragmentsAvailable.ABOUT_INSTEAD_OF_CHAT
					|| newFragmentType == FragmentsAvailable.ABOUT_INSTEAD_OF_SETTINGS
					|| newFragmentType == FragmentsAvailable.SETTINGS
					|| newFragmentType == FragmentsAvailable.ACCOUNT_SETTINGS) {
				ll.setVisibility(View.GONE);
			} else {
				ll.setVisibility(View.INVISIBLE);
			}

			if (!withoutAnimation && !isAnimationDisabled && currentFragment.shouldAnimate()) {
				if (newFragmentType.isRightOf(currentFragment)) {
					transaction.setCustomAnimations(R.anim.slide_in_right_to_left, R.anim.slide_out_right_to_left, R.anim.slide_in_left_to_right, R.anim.slide_out_left_to_right);
				} else {
					transaction.setCustomAnimations(R.anim.slide_in_left_to_right, R.anim.slide_out_left_to_right, R.anim.slide_in_right_to_left, R.anim.slide_out_right_to_left);
				}
			}

			transaction.replace(R.id.fragmentContainer, newFragment);
		}
		transaction.commitAllowingStateLoss();
		getSupportFragmentManager().executePendingTransactions();

		currentFragment = newFragmentType;
		if (newFragmentType == FragmentsAvailable.DIALER
				|| newFragmentType == FragmentsAvailable.ABOUT_INSTEAD_OF_CHAT
				|| newFragmentType == FragmentsAvailable.ABOUT_INSTEAD_OF_SETTINGS
				|| newFragmentType == FragmentsAvailable.SETTINGS
				|| newFragmentType == FragmentsAvailable.CONTACTS
				|| newFragmentType == FragmentsAvailable.CHATLIST
				|| newFragmentType == FragmentsAvailable.HISTORY) {
			try {
				getSupportFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
			} catch (IllegalStateException e) {

			}
		}
		fragmentsHistory.add(currentFragment);
	}

	public void displayHistoryDetail(String sipUri, LinphoneCallLog log) {
		LinphoneAddress lAddress;
		try {
			lAddress = LinphoneCoreFactory.instance().createLinphoneAddress(sipUri);
		} catch (LinphoneCoreException e) {
			Log.e("Cannot display history details", e);
			return;
		}
		Contact c = ContactsManager.getInstance().findContactWithAddress(lAddress);

		String displayName = c != null ? c.getName() : null;
		String pictureUri = c != null && c.getPhotoUri() != null ? c.getPhotoUri().toString() : null;

		String status;
		if (log.getDirection() == CallDirection.Outgoing) {
			status = "Outgoing";
		} else {
			if (log.getStatus() == CallStatus.Missed) {
				status = "Missed";
			} else {
				status = "Incoming";
			}
		}

		String callTime = secondsToDisplayableString(log.getCallDuration());
		String callDate = String.valueOf(log.getTimestamp());

		Fragment fragment2 = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer2);
		if (fragment2 != null && fragment2.isVisible() && currentFragment == FragmentsAvailable.HISTORY_DETAIL) {
			HistoryDetailFragment historyDetailFragment = (HistoryDetailFragment) fragment2;
			historyDetailFragment.changeDisplayedHistory(sipUri, displayName, pictureUri, status, callTime, callDate);
		} else {
			Bundle extras = new Bundle();
			extras.putString("SipUri", sipUri);
			if (displayName != null) {
				extras.putString("DisplayName", displayName);
				extras.putString("PictureUri", pictureUri);
			}
			extras.putString("CallStatus", status);
			extras.putString("CallTime", callTime);
			extras.putString("CallDate", callDate);

			changeCurrentFragment(FragmentsAvailable.HISTORY_DETAIL, extras);
		}
	}

	@SuppressLint("SimpleDateFormat")
	private String secondsToDisplayableString(int secs) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
		Calendar cal = Calendar.getInstance();
		cal.set(0, 0, 0, 0, 0, secs);
		return dateFormat.format(cal.getTime());
	}

	public void displayContact(Contact contact, boolean chatOnly) {
		Fragment fragment2 = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer2);
		if (fragment2 != null && fragment2.isVisible() && currentFragment == FragmentsAvailable.CONTACT) {
			ContactFragment contactFragment = (ContactFragment) fragment2;
			contactFragment.changeDisplayedContact(contact);
		} else {
			Bundle extras = new Bundle();
			extras.putSerializable("Contact", contact);
			extras.putBoolean("ChatAddressOnly", chatOnly);
			changeCurrentFragment(FragmentsAvailable.CONTACT, extras);
		}
	}

	public void displayContacts(boolean chatOnly) {
		if (chatOnly) {
			preferLinphoneContacts = true;
		}

		Bundle extras = new Bundle();
		extras.putBoolean("ChatAddressOnly", chatOnly);
		changeCurrentFragment(FragmentsAvailable.CONTACTS, extras);
		preferLinphoneContacts = false;
	}

	public void displayContactsForEdition(String sipAddress) {
		Bundle extras = new Bundle();
		extras.putBoolean("EditOnClick", true);
		extras.putString("SipAddress", sipAddress);
		changeCurrentFragment(FragmentsAvailable.CONTACTS, extras);
	}

	public void displayAbout() {
		changeCurrentFragment(FragmentsAvailable.ABOUT, null);
	}

	public void displayChat(String sipUri) {
		if (getResources().getBoolean(R.bool.disable_chat)) {
			return;
		}

		LinphoneAddress lAddress;
		try {
			lAddress = LinphoneCoreFactory.instance().createLinphoneAddress(sipUri);
		} catch (LinphoneCoreException e) {
			Log.e("Cannot display chat", e);
			return;
		}
		Contact contact = ContactsManager.getInstance().findContactWithAddress(lAddress);
		String displayName = contact != null ? contact.getName() : null;
		String pictureUri = contact != null && contact.getPhotoUri() != null ? contact.getPhotoUri().toString() : null;

		if (currentFragment == FragmentsAvailable.CHATLIST || currentFragment == FragmentsAvailable.CHAT) {
			if (isTablet()) {
				Fragment fragment2 = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer2);
				if (fragment2 != null && fragment2.isVisible() && currentFragment == FragmentsAvailable.CHAT) {
					ChatFragment chatFragment = (ChatFragment) fragment2;
					chatFragment.changeDisplayedChat(sipUri, displayName, pictureUri);
				} else {
					Bundle extras = new Bundle();
					extras.putString("SipUri", sipUri);
					if (lAddress.getDisplayName() != null) {
						extras.putString("DisplayName", displayName);
						extras.putString("PictureUri", pictureUri);
					}
					changeCurrentFragment(FragmentsAvailable.CHAT, extras);
				}
			}
		} else {
			Intent intent = new Intent(this, ChatActivity.class);
			intent.putExtra("SipUri", sipUri);
			if (contact != null) {
				intent.putExtra("DisplayName", contact.getName());
				intent.putExtra("PictureUri", contact.getPhotoUri());
			}
			startOrientationSensor();
			startActivityForResult(intent, CHAT_ACTIVITY);
		}

		LinphoneService.instance().resetMessageNotifCount();
		LinphoneService.instance().removeMessageNotification();
		displayMissedChats(getChatStorage().getUnreadMessageCount());
	}

	@Override
	public void onClick(View v) {
		int id = v.getId();
		resetSelection();

		if (id == R.id.history) {
			changeCurrentFragment(FragmentsAvailable.HISTORY, null);
			history.setSelected(true);
			LinphoneManager.getLc().resetMissedCallsCount();
			displayMissedCalls(0);
		} else if (id == R.id.contacts) {
			changeCurrentFragment(FragmentsAvailable.CONTACTS, null);
			contacts.setSelected(true);
		} else if (id == R.id.dialer) {
			changeCurrentFragment(FragmentsAvailable.DIALER, null);
			dialer.setSelected(true);
		} else if (id == R.id.settings) {
			changeCurrentFragment(FragmentsAvailable.SETTINGS, null);
			settings.setSelected(true);
		} else if (id == R.id.about_chat) {
			Bundle b = new Bundle();
			b.putSerializable("About", FragmentsAvailable.ABOUT_INSTEAD_OF_CHAT);
			changeCurrentFragment(FragmentsAvailable.ABOUT_INSTEAD_OF_CHAT, b);
			aboutChat.setSelected(true);
		} else if (id == R.id.about_settings) {
			Bundle b = new Bundle();
			b.putSerializable("About", FragmentsAvailable.ABOUT_INSTEAD_OF_SETTINGS);
			changeCurrentFragment(FragmentsAvailable.ABOUT_INSTEAD_OF_SETTINGS, b);
			aboutSettings.setSelected(true);
		} else if (id == R.id.chat) {
			changeCurrentFragment(FragmentsAvailable.CHATLIST, null);
			chat.setSelected(true);
		}
	}

	private void resetSelection() {
		history.setSelected(false);
		contacts.setSelected(false);
		dialer.setSelected(false);
		settings.setSelected(false);
		chat.setSelected(false);
		aboutChat.setSelected(false);
		aboutSettings.setSelected(false);
	}

	@SuppressWarnings("incomplete-switch")
	public void selectMenu(FragmentsAvailable menuToSelect) {
		currentFragment = menuToSelect;
		resetSelection();

		switch (menuToSelect) {
		case HISTORY:
		case HISTORY_DETAIL:
			history.setSelected(true);
			break;
		case CONTACTS:
		case CONTACT:
		case EDIT_CONTACT:
			contacts.setSelected(true);
			break;
		case DIALER:
			dialer.setSelected(true);
			break;
		case SETTINGS:
		case ACCOUNT_SETTINGS:
			settings.setSelected(true);
			break;
		case ABOUT_INSTEAD_OF_CHAT:
			aboutChat.setSelected(true);
			break;
		case ABOUT_INSTEAD_OF_SETTINGS:
			aboutSettings.setSelected(true);
			break;
		case CHATLIST:
		case CHAT:
			chat.setSelected(true);
			break;
		}
	}

	public void updateDialerFragment(DialerFragment fragment) {
		dialerFragment = fragment;
		// Hack to maintain soft input flags
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN | WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
	}

	public void updateChatListFragment(ChatListFragment fragment) {
		messageListFragment = fragment;
	}

	public void hideMenu(boolean hide) {
		menu.setVisibility(hide ? View.GONE : View.VISIBLE);
		mark.setVisibility(hide ? View.GONE : View.VISIBLE);
	}

	public void updateStatusFragment(StatusFragment fragment) {
		statusFragment = fragment;
	}

	public void displaySettings() {
		changeCurrentFragment(FragmentsAvailable.SETTINGS, null);
		settings.setSelected(true);
	}

	public void applyConfigChangesIfNeeded() {
		if (nextFragment != FragmentsAvailable.SETTINGS && nextFragment != FragmentsAvailable.ACCOUNT_SETTINGS) {
			updateAnimationsState();
		}
	}

	public void displayAccountSettings(int accountNumber) {
		Bundle bundle = new Bundle();
		bundle.putInt("Account", accountNumber);
		changeCurrentFragment(FragmentsAvailable.ACCOUNT_SETTINGS, bundle);
		settings.setSelected(true);
	}

	public StatusFragment getStatusFragment() {
		return statusFragment;
	}

	public List<String> getChatList() {
		return getChatStorage().getChatList();
	}

	public List<String> getDraftChatList() {
		return getChatStorage().getDrafts();
	}

	public List<ChatMessage> getChatMessages(String correspondent) {
		return getChatStorage().getMessages(correspondent);
	}

	public void removeFromChatList(String sipUri) {
		getChatStorage().removeDiscussion(sipUri);
	}

	public void removeFromDrafts(String sipUri) {
		getChatStorage().deleteDraft(sipUri);
	}



	public void updateMissedChatCount() {
		displayMissedChats(getChatStorage().getUnreadMessageCount());
	}

	public int onMessageSent(String to, String message) {
		getChatStorage().deleteDraft(to);
		return getChatStorage().saveTextMessage("", to, message, System.currentTimeMillis());
	}

	public int onMessageSent(String to, Bitmap image, String imageURL) {
		getChatStorage().deleteDraft(to);
		return getChatStorage().saveImageMessage("", to, image, imageURL, System.currentTimeMillis());
	}

	public void onMessageStateChanged(String to, String message, int newState) {
		getChatStorage().updateMessageStatus(to, message, newState);
	}

	public void onImageMessageStateChanged(String to, int id, int newState) {
		getChatStorage().updateMessageStatus(to, id, newState);
	}

	private void displayMissedCalls(final int missedCallsCount) {
		if (missedCallsCount > 0) {
			missedCalls.setText(missedCallsCount + "");
			missedCalls.setVisibility(View.VISIBLE);
			if (!isAnimationDisabled) {
				missedCalls.startAnimation(AnimationUtils.loadAnimation(LinphoneActivity.this, R.anim.bounce));
			}
		} else {
			missedCalls.clearAnimation();
			missedCalls.setVisibility(View.GONE);
		}
	}

	private void displayMissedChats(final int missedChatCount) {
		if (missedChatCount > 0) {
			missedChats.setText(missedChatCount + "");
			if (missedChatCount > 99) {
				missedChats.setTextSize(12);
			} else {
				missedChats.setTextSize(20);
			}
			missedChats.setVisibility(View.VISIBLE);
			if (!isAnimationDisabled) {
				missedChats.startAnimation(AnimationUtils.loadAnimation(LinphoneActivity.this, R.anim.bounce));
			}
		} else {
			missedChats.clearAnimation();
			missedChats.setVisibility(View.GONE);
		}
	}

	public void displayCustomToast(final String message, final int duration) {
		LayoutInflater inflater = getLayoutInflater();
		View layout = inflater.inflate(R.layout.toast, (ViewGroup) findViewById(R.id.toastRoot));

		TextView toastText = (TextView) layout.findViewById(R.id.toastMessage);
		toastText.setText(message);

		final Toast toast = new Toast(getApplicationContext());
		toast.setGravity(Gravity.CENTER, 0, 0);
		toast.setDuration(duration);
		toast.setView(layout);
		toast.show();
	}

	@Override
	public void setAddresGoToDialerAndCall(String number, String name, Uri photo) {
//		Bundle extras = new Bundle();
//		extras.putString("SipUri", number);
//		extras.putString("DisplayName", name);
//		extras.putString("Photo", photo == null ? null : photo.toString());
//		changeCurrentFragment(FragmentsAvailable.DIALER, extras);

		AddressType address = new AddressText(this, null);
		address.setDisplayedName(name);
		address.setText(number);
		LinphoneManager.getInstance().newOutgoingCall(address);
	}

	public void setAddressAndGoToDialer(String number) {
		Bundle extras = new Bundle();
		extras.putString("SipUri", number);
		changeCurrentFragment(FragmentsAvailable.DIALER, extras);
	}

	@Override
	public void goToDialer() {
		changeCurrentFragment(FragmentsAvailable.DIALER, null);
	}

	public void startVideoActivity(LinphoneCall currentCall) {
		Intent intent = new Intent(this, InCallActivity.class);
		intent.putExtra("VideoEnabled", true);
		startOrientationSensor();
		startActivityForResult(intent, CALL_ACTIVITY);
	}

	public void startIncallActivity(LinphoneCall currentCall) {
		Intent intent = new Intent(this, InCallActivity.class);
		intent.putExtra("VideoEnabled", false);
		startOrientationSensor();
		startActivityForResult(intent, CALL_ACTIVITY);
	}

	/**
	 * Register a sensor to track phoneOrientation changes
	 */
	private synchronized void startOrientationSensor() {
		if (mOrientationHelper == null) {
			mOrientationHelper = new LocalOrientationEventListener(this);
		}
		mOrientationHelper.enable();
	}

	private int mAlwaysChangingPhoneAngle = -1;

	private class LocalOrientationEventListener extends OrientationEventListener {
		public LocalOrientationEventListener(Context context) {
			super(context);
		}

		@Override
		public void onOrientationChanged(final int o) {
			if (o == OrientationEventListener.ORIENTATION_UNKNOWN) {
				return;
			}

			int degrees = 270;
			if (o < 45 || o > 315)
				degrees = 0;
			else if (o < 135)
				degrees = 90;
			else if (o < 225)
				degrees = 180;

			if (mAlwaysChangingPhoneAngle == degrees) {
				return;
			}
			mAlwaysChangingPhoneAngle = degrees;

			Log.d("Phone orientation changed to ", degrees);
			int rotation = (360 - degrees) % 360;
			LinphoneCore lc = LinphoneManager.getLcIfManagerNotDestroyedOrNull();
			if (lc != null) {
				lc.setDeviceRotation(rotation);
				LinphoneCall currentCall = lc.getCurrentCall();
				if (currentCall != null && currentCall.cameraEnabled() && currentCall.getCurrentParamsCopy().getVideoEnabled()) {
					lc.updateCall(currentCall, null);
				}
			}
		}
	}

	private void initInCallMenuLayout(boolean callTransfer) {
		selectMenu(FragmentsAvailable.DIALER);
		if (dialerFragment != null) {
			((DialerFragment) dialerFragment).resetLayout(callTransfer);
		}
	}

	public void resetClassicMenuLayoutAndGoBackToCallIfStillRunning() {
		if (dialerFragment != null) {
			((DialerFragment) dialerFragment).resetLayout(false);
		}

		if (LinphoneManager.isInstanciated() && LinphoneManager.getLc().getCallsNb() > 0) {
			LinphoneCall call = LinphoneManager.getLc().getCalls()[0];
			if (call.getState() == LinphoneCall.State.IncomingReceived) {
				startActivity(new Intent(LinphoneActivity.this, IncomingCallActivity.class));
			} else if (call.getCurrentParamsCopy().getVideoEnabled()) {
				startVideoActivity(call);
			} else {
				startIncallActivity(call);
			}
		}
	}

	public FragmentsAvailable getCurrentFragment() {
		return currentFragment;
	}

	public ChatStorage getChatStorage() {
		return ChatStorage.getInstance();
	}

	public void addContact(String displayName, String sipUri)
	{
		if (getResources().getBoolean(R.bool.use_android_native_contact_edit_interface)) {
			Intent intent = Compatibility.prepareAddContactIntent(displayName, sipUri);
			startActivity(intent);
		} else {
			Bundle extras = new Bundle();
			extras.putSerializable("NewSipAdress", sipUri);
			changeCurrentFragment(FragmentsAvailable.EDIT_CONTACT, extras);
		}
	}

	public void editContact(Contact contact)
	{
		if (getResources().getBoolean(R.bool.use_android_native_contact_edit_interface)) {
			Intent intent = Compatibility.prepareEditContactIntent(Integer.parseInt(contact.getID()));
			startActivity(intent);
		} else {
			Bundle extras = new Bundle();
			extras.putSerializable("Contact", contact);
			changeCurrentFragment(FragmentsAvailable.EDIT_CONTACT, extras);
		}

	}

	public void editContact(Contact contact, String sipAddress)
	{
		if (getResources().getBoolean(R.bool.use_android_native_contact_edit_interface)) {
			Intent intent = Compatibility.prepareEditContactIntentWithSipAddress(Integer.parseInt(contact.getID()), sipAddress);
			startActivity(intent);
		} else {
			Bundle extras = new Bundle();
			extras.putSerializable("Contact", contact);
			extras.putSerializable("NewSipAdress", sipAddress);
			changeCurrentFragment(FragmentsAvailable.EDIT_CONTACT, extras);
		}
	}

	public void exit() {
		finish();
		stopService(new Intent(ACTION_MAIN).setClass(this, LinphoneService.class));
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == Activity.RESULT_FIRST_USER && requestCode == SETTINGS_ACTIVITY) {
			if (data.getExtras().getBoolean("Exit", false)) {
				exit();
			} else {
				FragmentsAvailable newFragment = (FragmentsAvailable) data.getExtras().getSerializable("FragmentToDisplay");
				changeCurrentFragment(newFragment, null, true);
				selectMenu(newFragment);
			}
		} else if (resultCode == Activity.RESULT_FIRST_USER && requestCode == CALL_ACTIVITY) {
			getIntent().putExtra("PreviousActivity", CALL_ACTIVITY);
			boolean callTransfer = data == null ? false : data.getBooleanExtra("Transfer", false);
			if (LinphoneManager.getLc().getCallsNb() > 0) {
				initInCallMenuLayout(callTransfer);
			} else {
				resetClassicMenuLayoutAndGoBackToCallIfStillRunning();
			}
		} else {
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

	@Override
	protected void onPause() {
		getIntent().putExtra("PreviousActivity", 0);
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (!LinphoneService.isReady())  {
			startService(new Intent(ACTION_MAIN).setClass(this, LinphoneService.class));
		}

		ContactsManager.getInstance().prepareContactsInBackground();

		updateMissedChatCount();

		displayMissedCalls(LinphoneManager.getLc().getMissedCallsCount());

		LinphoneManager.getInstance().changeStatusToOnline();

		if(getIntent().getIntExtra("PreviousActivity", 0) != CALL_ACTIVITY){
			if (LinphoneManager.getLc().getCalls().length > 0) {
				LinphoneCall call = LinphoneManager.getLc().getCalls()[0];
				LinphoneCall.State callState = call.getState();
				if (callState == State.IncomingReceived) {
					startActivity(new Intent(this, IncomingCallActivity.class));
				} else {

						if (call.getCurrentParamsCopy().getVideoEnabled()) {
							startVideoActivity(call);
						} else {
							startIncallActivity(call);
						}
					}
				}
		}
	}

	@Override
	protected void onDestroy() {
		if (mOrientationHelper != null) {
			mOrientationHelper.disable();
			mOrientationHelper = null;
		}

		LinphoneCore lc = LinphoneManager.getLcIfManagerNotDestroyedOrNull();
		if (lc != null) {
			lc.removeListener(mListener);
		}

		instance = null;
		super.onDestroy();

		unbindDrawables(findViewById(R.id.topLayout));
		System.gc();
	}

	private void unbindDrawables(View view) {
		if (view != null && view.getBackground() != null) {
			view.getBackground().setCallback(null);
		}
		if (view instanceof ViewGroup && !(view instanceof AdapterView)) {
			for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
				unbindDrawables(((ViewGroup) view).getChildAt(i));
			}
			((ViewGroup) view).removeAllViews();
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);

		Bundle extras = intent.getExtras();
		if (extras != null && extras.getBoolean("GoToChat", false)) {
			LinphoneService.instance().removeMessageNotification();
			String sipUri = extras.getString("ChatContactSipUri");
			displayChat(sipUri);
		} else if (extras != null && extras.getBoolean("Notification", false)) {
			if (LinphoneManager.getLc().getCallsNb() > 0) {
				LinphoneCall call = LinphoneManager.getLc().getCalls()[0];
				if (call.getCurrentParamsCopy().getVideoEnabled()) {
					startVideoActivity(call);
				} else {
					startIncallActivity(call);
				}
			}
		} else {
			if (dialerFragment != null) {
				if (extras != null && extras.containsKey("SipUriOrNumber")) {
					if (getResources().getBoolean(R.bool.automatically_start_intercepted_outgoing_gsm_call)) {
						((DialerFragment) dialerFragment).newOutgoingCall(extras.getString("SipUriOrNumber"));
					} else {
						((DialerFragment) dialerFragment).displayTextInAddressBar(extras.getString("SipUriOrNumber"));
					}
				} else {
					((DialerFragment) dialerFragment).newOutgoingCall(intent);
				}
			}
			if (LinphoneManager.getLc().getCalls().length > 0) {
				LinphoneCall calls[] = LinphoneManager.getLc().getCalls();
				if (calls.length > 0) {
					LinphoneCall call = calls[0];

					if (call != null && call.getState() != LinphoneCall.State.IncomingReceived) {
						if (call.getCurrentParamsCopy().getVideoEnabled()) {
							startVideoActivity(call);
						} else {
							startIncallActivity(call);
						}
					}
				}

				// If a call is ringing, start incomingcallactivity
				Collection<State> incoming = new ArrayList<State>();
				incoming.add(LinphoneCall.State.IncomingReceived);
				if (LinphoneUtils.getCallsInState(LinphoneManager.getLc(), incoming).size() > 0) {
					if (InCallActivity.isInstanciated()) {
						InCallActivity.instance().startIncomingCallActivity();
					} else {
						startActivity(new Intent(this, IncomingCallActivity.class));
					}
				}
			}
		}
	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (currentFragment == FragmentsAvailable.DIALER
					|| currentFragment == FragmentsAvailable.CONTACTS
					|| currentFragment == FragmentsAvailable.HISTORY
					|| currentFragment == FragmentsAvailable.CHATLIST
					|| currentFragment == FragmentsAvailable.ABOUT_INSTEAD_OF_CHAT
					|| currentFragment == FragmentsAvailable.ABOUT_INSTEAD_OF_SETTINGS) {
				boolean isBackgroundModeActive = LinphonePreferences.instance().isBackgroundModeEnabled();
				if (!isBackgroundModeActive) {
					stopService(new Intent(Intent.ACTION_MAIN).setClass(this, LinphoneService.class));
					finish();
				} else if (LinphoneUtils.onKeyBackGoHome(this, keyCode, event)) {
					return true;
				}
			} else {
				if (isTablet()) {
					if (currentFragment == FragmentsAvailable.SETTINGS) {
						updateAnimationsState();
					}
				}
			}
		} else if (keyCode == KeyEvent.KEYCODE_MENU && statusFragment != null) {
			if (event.getRepeatCount() < 1) {
				statusFragment.openOrCloseStatusBar(true);
			}
		}
		return super.onKeyDown(keyCode, event);
	}
}

interface ContactPicked {
	void setAddresGoToDialerAndCall(String number, String name, Uri photo);
	void goToDialer();
}

package org.linphone;

import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TextView;

import org.linphone.compatibility.Compatibility;
import org.linphone.core.LinphoneCore;
import org.linphone.core.LinphoneProxyConfig;
import org.linphone.ui.AvatarWithShadow;

import java.io.InputStream;

/**
 * @author Sylvain Berfini
 */
public class ContactFragment2 extends Fragment implements OnClickListener {
	private LayoutInflater inflater;
	private View view;
	private boolean displayChatAddressOnly = false;
	private Contact contact;

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		this.inflater = inflater;
		view = inflater.inflate(R.layout.contact, container, false);

		if (getArguments() != null) {
			// 控制是否就只能聊天，而不能打电话
			displayChatAddressOnly = getArguments().getBoolean("ChatAddressOnly");
			// 这个应该是得到上一个页面传递过来的实体类联系人数据
			contact = (Contact) getArguments().getSerializable("Contact");
		}
		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		contact.refresh(getActivity().getContentResolver());
		if (contact.getName() == null || contact.getName().equals("")) {
			//Contact has been deleted, return
			LinphoneActivity.instance().displayContacts(false);
		}
		displayContact(inflater, view);
	}

	private void displayContact(LayoutInflater inflater, View view) {
		AvatarWithShadow contactPicture = (AvatarWithShadow) view.findViewById(R.id.contactPicture);
		if (contact.getPhotoUri() != null) {
			InputStream input = Compatibility.getContactPictureInputStream(LinphoneActivity.instance().getContentResolver(), contact.getID());
			contactPicture.setImageBitmap(BitmapFactory.decodeStream(input));
		} else {
			contactPicture.setImageResource(R.drawable.unknown_small);
		}

		TextView contactName = (TextView) view.findViewById(R.id.contactName);
		contactName.setText(contact.getName());

		TableLayout controls = (TableLayout) view.findViewById(R.id.controls);
		controls.removeAllViews();

		for (String numberOrAddress : contact.getNumbersOrAddresses()) {
			View v = inflater.inflate(R.layout.contact_control_row, null);

			String displayednumberOrAddress = numberOrAddress;
			if (numberOrAddress.startsWith("sip:")) {
				displayednumberOrAddress = displayednumberOrAddress.replace("sip:", "");
			}

			TextView tv = (TextView) v.findViewById(R.id.numeroOrAddress);
			tv.setText(displayednumberOrAddress);
			tv.setSelected(true);

			if (!displayChatAddressOnly) {
				v.findViewById(R.id.dial).setOnClickListener(dialListener);
				v.findViewById(R.id.dial).setTag(displayednumberOrAddress);
			} else {
				v.findViewById(R.id.dial).setVisibility(View.GONE);
			}

			LinphoneProxyConfig lpc = LinphoneManager.getLc().getDefaultProxyConfig();
			if (lpc != null) {
				if (!displayednumberOrAddress.startsWith("sip:")) {
					numberOrAddress = "sip:" + displayednumberOrAddress;
				}

				String tag = numberOrAddress;
				if (!numberOrAddress.contains("@")) {
					tag = numberOrAddress + "@" + lpc.getDomain();
				}
				v.findViewById(R.id.start_chat).setTag(tag);
			} else {
				v.findViewById(R.id.start_chat).setTag(numberOrAddress);
			}

			controls.addView(v);
		}

	}

	public void changeDisplayedContact(Contact newContact) {
		contact = newContact;
		contact.refresh(getActivity().getContentResolver());
		displayContact(inflater, view);
	}

	@Override
	public void onClick(View v) {

	}

	private OnClickListener dialListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			if (LinphoneActivity.isInstanciated()) {
				LinphoneCore lc = LinphoneManager.getLcIfManagerNotDestroyedOrNull();
				if (lc != null) {
					LinphoneProxyConfig lpc = lc.getDefaultProxyConfig();
					String to;
					if (lpc != null) {
						String address = v.getTag().toString();
						if (address.contains("@")) {
							to = lpc.normalizePhoneNumber(address.split("@")[0]);
						} else {
							to = lpc.normalizePhoneNumber(address);
						}
					} else {
						to = v.getTag().toString();
					}
					LinphoneActivity.instance().setAddresGoToDialerAndCall(to, contact.getName(), contact.getPhotoUri());
				}
			}
		}
	};
}

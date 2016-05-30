package org.linphone.compatibility;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;

import org.linphone.R;
/*
ApiSixteenPlus.java
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

/**
 * @author Sylvain Berfini
 */
@TargetApi(16)
public class ApiSixteenPlus {

	public static Notification createMessageNotification(Context context,
			int msgCount, String msgSender, String msg, Bitmap contactIcon,
			PendingIntent intent) {
		String title;
		if (msgCount == 1) {
			title = msgSender;
		} else {
			title = context.getString(R.string.unread_messages)
					.replace("%i", String.valueOf(msgCount));
		}

		Notification notif = new Notification.Builder(context)
						.setContentTitle(title)
						.setContentText(msg)
						.setSmallIcon(R.drawable.chat_icon_over)
						.setAutoCancel(true)
						.setContentIntent(intent)
						.setDefaults(
								Notification.DEFAULT_LIGHTS
										| Notification.DEFAULT_SOUND
										| Notification.DEFAULT_VIBRATE)
						.setWhen(System.currentTimeMillis())
						.setLargeIcon(contactIcon)
				.build();

		return notif;
	}

	public static Notification createInCallNotification(Context context,
			String title, String msg, int iconID, Bitmap contactIcon,
			String contactName, PendingIntent intent) {

		Notification notif = new Notification.Builder(context).setContentTitle(contactName)
						.setContentText(msg).setSmallIcon(iconID)
						.setAutoCancel(false)
						.setContentIntent(intent)
						.setWhen(System.currentTimeMillis())
						.setLargeIcon(contactIcon).build();
		notif.flags |= Notification.FLAG_ONGOING_EVENT;
		
		return notif;
	}
	
	public static Notification createNotification(Context context, String title, String message, int icon, int level, Bitmap largeIcon, PendingIntent intent, boolean isOngoingEvent) {
		Notification notif;
		
		if (largeIcon != null) {
			notif = new Notification.Builder(context)
	        .setContentTitle(title)
	        .setContentText(message)
	        .setSmallIcon(icon, level)
	        .setLargeIcon(largeIcon)
	        .setContentIntent(intent)
	        .setWhen(System.currentTimeMillis())
	        .build();
		} else {
			notif = new Notification.Builder(context)
	        .setContentTitle(title)
	        .setContentText(message)
	        .setSmallIcon(icon, level)
	        .setContentIntent(intent)
	        .setWhen(System.currentTimeMillis())
	        .build();
		}
		if (isOngoingEvent) {
			notif.flags |= Notification.FLAG_ONGOING_EVENT;
		}
		
		return notif;
	}

	public static void removeGlobalLayoutListener(ViewTreeObserver viewTreeObserver, OnGlobalLayoutListener keyboardListener) {
		viewTreeObserver.removeOnGlobalLayoutListener(keyboardListener);		
	}
}

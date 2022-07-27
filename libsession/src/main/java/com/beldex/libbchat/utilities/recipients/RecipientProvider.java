/*
 * Copyright (C) 2011 Whisper Systems
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.beldex.libbchat.utilities.recipients;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.beldex.libbchat.messaging.MessagingModuleConfiguration;
import com.beldex.libbchat.utilities.Address;
import com.beldex.libbchat.utilities.GroupRecord;
import com.beldex.libbchat.utilities.ListenableFutureTask;
import com.beldex.libbchat.utilities.MaterialColor;
import com.beldex.libbchat.utilities.TextSecurePreferences;
import com.beldex.libbchat.utilities.Util;
import com.beldex.libsignal.utilities.guava.Optional;

import com.beldex.libbchat.R;
import com.beldex.libbchat.messaging.MessagingModuleConfiguration;
import com.beldex.libbchat.utilities.Address;
import com.beldex.libbchat.utilities.GroupRecord;
import com.beldex.libbchat.utilities.TextSecurePreferences;
import com.beldex.libbchat.utilities.Util;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

class RecipientProvider {

  @SuppressWarnings("unused")
  private static final String TAG = RecipientProvider.class.getSimpleName();

  private static final RecipientCache  recipientCache         = new RecipientCache();
  private static final ExecutorService asyncRecipientResolver = Util.newSingleThreadedLifoExecutor();

  @NonNull Recipient getRecipient(@NonNull Context context, @NonNull Address address, @NonNull Optional<Recipient.RecipientSettings> settings, @NonNull Optional<GroupRecord> groupRecord, boolean asynchronous) {
    Recipient cachedRecipient = recipientCache.get(address);

    if (cachedRecipient != null && (asynchronous || !cachedRecipient.isResolving()) && ((!groupRecord.isPresent() && !settings.isPresent()) || !cachedRecipient.isResolving() || cachedRecipient.getName() != null)) {
      return cachedRecipient;
    }

    Optional<RecipientDetails> prefetchedRecipientDetails = createPrefetchedRecipientDetails(context, address, settings, groupRecord);

    if (asynchronous) {
      cachedRecipient = new Recipient(context, address, cachedRecipient, prefetchedRecipientDetails, getRecipientDetailsAsync(context, address, settings, groupRecord));
    } else {
      cachedRecipient = new Recipient(context, address, getRecipientDetailsSync(context, address, settings, groupRecord, false));
    }

    recipientCache.set(address, cachedRecipient);
    return cachedRecipient;
  }

  @NonNull Optional<Recipient> getCached(@NonNull Address address) {
    return Optional.fromNullable(recipientCache.get(address));
  }

  boolean removeCached(@NonNull Address address) {
    return recipientCache.remove(address);
  }

  private @NonNull Optional<RecipientDetails> createPrefetchedRecipientDetails(@NonNull Context context, @NonNull Address address,
                                                                               @NonNull Optional<Recipient.RecipientSettings> settings,
                                                                               @NonNull Optional<GroupRecord> groupRecord)
  {
    if (address.isGroup() && settings.isPresent() && groupRecord.isPresent()) {
      return Optional.of(getGroupRecipientDetails(context, address, groupRecord, settings, true));
    } else if (!address.isGroup() && settings.isPresent()) {
      boolean isLocalNumber = address.serialize().equals(TextSecurePreferences.getLocalNumber(context));

      return Optional.of(new RecipientDetails(null, null, !TextUtils.isEmpty(settings.get().getSystemDisplayName()), isLocalNumber, settings.get(), null));
    }

    return Optional.absent();
  }

  private @NonNull
  ListenableFutureTask<RecipientDetails> getRecipientDetailsAsync(final Context context, final @NonNull Address address, final @NonNull Optional<Recipient.RecipientSettings> settings, final @NonNull Optional<GroupRecord> groupRecord)
  {
    Callable<RecipientDetails> task = () -> getRecipientDetailsSync(context, address, settings, groupRecord, true);

    ListenableFutureTask<RecipientDetails> future = new ListenableFutureTask<>(task);
    asyncRecipientResolver.submit(future);
    return future;
  }

  private @NonNull RecipientDetails getRecipientDetailsSync(Context context, @NonNull Address address, Optional<Recipient.RecipientSettings> settings, Optional<GroupRecord> groupRecord, boolean nestedAsynchronous) {
    if (address.isGroup()) return getGroupRecipientDetails(context, address, groupRecord, settings, nestedAsynchronous);
    else                   return getIndividualRecipientDetails(context, address, settings);
  }

  private @NonNull RecipientDetails getIndividualRecipientDetails(Context context, @NonNull Address address, Optional<Recipient.RecipientSettings> settings) {
    if (!settings.isPresent()) {
      settings = Optional.fromNullable(MessagingModuleConfiguration.shared.getStorage().getRecipientSettings(address));
    }

    //New Line v32
    boolean systemContact = settings.isPresent() && !TextUtils.isEmpty(settings.get().getSystemDisplayName());
    boolean isLocalNumber = address.serialize().equals(TextSecurePreferences.getLocalNumber(context));
    return new RecipientDetails(null, null, systemContact, isLocalNumber, settings.orNull(), null);
  }

  private @NonNull RecipientDetails getGroupRecipientDetails(Context context, Address groupId, Optional<GroupRecord> groupRecord, Optional<Recipient.RecipientSettings> settings, boolean asynchronous) {

    if (!groupRecord.isPresent()) {
      groupRecord = Optional.fromNullable(MessagingModuleConfiguration.shared.getStorage().getGroup(groupId.toGroupString()));
    }

    if (!settings.isPresent()) {

      settings = Optional.fromNullable(MessagingModuleConfiguration.shared.getStorage().getRecipientSettings(groupId));
    }

    if (groupRecord.isPresent()) {
      String          title           = groupRecord.get().getTitle();
      List<Address>   memberAddresses = groupRecord.get().getMembers();
      List<Recipient> members         = new LinkedList<>();
      Long            avatarId        = null;

      for (Address memberAddress : memberAddresses) {
        members.add(getRecipient(context, memberAddress, Optional.absent(), Optional.absent(), asynchronous));
      }

      if (groupRecord.get().getAvatar() != null && groupRecord.get().getAvatar().length > 0) {
        avatarId = groupRecord.get().getAvatarId();
      }

      return new RecipientDetails(title, avatarId, false, false, settings.orNull(), members);
    }

    return new RecipientDetails(context.getString(R.string.RecipientProvider_unnamed_group), null, false, false, settings.orNull(), null);
  }

  static class RecipientDetails {
    @Nullable final String                 name;
    @Nullable final String                 customLabel;
    @Nullable final Uri                    systemContactPhoto;
    @Nullable final Uri                    contactUri;
    @Nullable final Long                   groupAvatarId;
    @Nullable final MaterialColor color;
    @Nullable final Uri                    messageRingtone;
    @Nullable final Uri                    callRingtone;
    final long                   mutedUntil;
    final int                    notifyType;
    @Nullable final Recipient.VibrateState messageVibrateState;
    @Nullable final Recipient.VibrateState callVibrateState;
    final boolean                blocked;
    final boolean                approved;
    final boolean                approvedMe;
    final int                    expireMessages;
    @NonNull  final List<Recipient>        participants;
    @Nullable final String                 profileName;
    final Optional<Integer>      defaultSubscriptionId;
    @NonNull  final Recipient.RegisteredState registered;
    @Nullable final byte[]                 profileKey;
    @Nullable final String                 profileAvatar;
    final boolean                profileSharing;
    final boolean                systemContact;
    final boolean                isLocalNumber;
    @Nullable final String                 notificationChannel;
    @NonNull  final Recipient.UnidentifiedAccessMode unidentifiedAccessMode;
    final boolean                forceSmsSelection;

    RecipientDetails(@Nullable String name, @Nullable Long groupAvatarId,
                     boolean systemContact, boolean isLocalNumber, @Nullable Recipient.RecipientSettings settings,
                     @Nullable List<Recipient> participants)
    {
      this.groupAvatarId                   = groupAvatarId;
      this.systemContactPhoto              = settings     != null ? Util.uri(settings.getSystemContactPhotoUri()) : null;
      this.customLabel                     = settings     != null ? settings.getSystemPhoneLabel() : null;
      this.contactUri                      = settings     != null ? Util.uri(settings.getSystemContactUri()) : null;
      this.color                           = settings     != null ? settings.getColor() : null;
      this.messageRingtone                 = settings     != null ? settings.getMessageRingtone() : null;
      this.callRingtone                    = settings     != null ? settings.getCallRingtone() : null;
      this.mutedUntil                      = settings     != null ? settings.getMuteUntil() : 0;
      this.notifyType                      = settings     != null ? settings.getNotifyType() : 0;
      this.messageVibrateState             = settings     != null ? settings.getMessageVibrateState() : null;
      this.callVibrateState                = settings     != null ? settings.getCallVibrateState() : null;
      this.blocked                         = settings     != null && settings.isBlocked();
      this.approved                        = settings     != null && settings.isApproved();
      this.approvedMe                      = settings     != null && settings.hasApprovedMe();
      this.expireMessages                  = settings     != null ? settings.getExpireMessages() : 0;
      this.participants                    = participants == null ? new LinkedList<>() : participants;
      this.profileName                     = settings     != null ? settings.getProfileName() : null;
      this.defaultSubscriptionId           = settings     != null ? settings.getDefaultSubscriptionId() : Optional.absent();
      this.registered                      = settings     != null ? settings.getRegistered() : Recipient.RegisteredState.UNKNOWN;
      this.profileKey                      = settings     != null ? settings.getProfileKey() : null;
      this.profileAvatar                   = settings     != null ? settings.getProfileAvatar() : null;
      this.profileSharing                  = settings     != null && settings.isProfileSharing();
      this.systemContact                   = systemContact;
      this.isLocalNumber                   = isLocalNumber;
      this.notificationChannel             = settings     != null ? settings.getNotificationChannel() : null;
      this.unidentifiedAccessMode          = settings     != null ? settings.getUnidentifiedAccessMode() : Recipient.UnidentifiedAccessMode.DISABLED;
      this.forceSmsSelection               = settings     != null && settings.isForceSmsSelection();

      if (name == null && settings != null) this.name = settings.getSystemDisplayName();
      else                                  this.name = name;
    }
  }

  private static class RecipientCache {

    private final Map<Address,Recipient> cache = new ConcurrentHashMap<>(1000);

    public Recipient get(Address address) {
      return cache.get(address);
    }

    public void set(Address address, Recipient recipient) {
      cache.put(address, recipient);
    }

    public boolean remove(Address address) {
      return cache.remove(address) != null;
    }

  }

}
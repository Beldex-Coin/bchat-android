package io.beldex.bchat.search.model;

import android.database.ContentObserver;

import androidx.annotation.NonNull;

import com.beldex.libbchat.messaging.contacts.Contact;
import com.beldex.libbchat.utilities.GroupRecord;
import io.beldex.bchat.database.CursorList;

import java.util.List;

/**
 * Represents an all-encompassing search result that can contain various result for different
 * subcategories.
 */
public class SearchResult {

  public static final SearchResult EMPTY = new SearchResult("", CursorList.emptyList(), CursorList.emptyList(), CursorList.emptyList());

  private final String                    query;
  private final CursorList<Contact>     contacts;
  private final CursorList<GroupRecord>  conversations;
  private final CursorList<MessageResult> messages;

  public SearchResult(@NonNull String                    query,
                      @NonNull CursorList<Contact>     contacts,
                      @NonNull CursorList<GroupRecord>  conversations,
                      @NonNull CursorList<MessageResult> messages)
  {
    this.query         = query;
    this.contacts      = contacts;
    this.conversations = conversations;
    this.messages      = messages;
  }

  public List<Contact> getContacts() {
    return contacts;
  }

  public List<GroupRecord> getConversations() {
    return conversations;
  }

  public List<MessageResult> getMessages() {
    return messages;
  }

  public String getQuery() {
    return query;
  }

  public int size() {
    return contacts.size() + conversations.size() + messages.size();
  }

  public boolean isEmpty() {
    return size() == 0;
  }

  public void registerContentObserver(@NonNull ContentObserver observer) {
    contacts.registerContentObserver(observer);
    conversations.registerContentObserver(observer);
    messages.registerContentObserver(observer);
  }

  public void unregisterContentObserver(@NonNull ContentObserver observer) {
    contacts.unregisterContentObserver(observer);
    conversations.unregisterContentObserver(observer);
    messages.unregisterContentObserver(observer);
  }

  public void close() {
    contacts.close();
    conversations.close();
    messages.close();
  }
}

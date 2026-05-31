package com.android.countdowntimer.home;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.UUID;

public class Event {

    @NonNull
    private String mId;

    @Nullable
    private String mTitle;

    @Nullable
    private String mNote;

    private long mStartDate;

    private long mEndDate;

    private int mState;

    private boolean mDurableEvent;

    private int mPriority;

    private int mReminder;

    private String mCategory;

    private long mCreationDate;

    @Nullable
    private String mImageUri;

    @Nullable
    private String mSource;

    @Nullable
    private String mSourceCalendarId;

    @Nullable
    private String mSourceEventId;

    private boolean mExternalEvent;

    private boolean mNowBarEnabled;

    //Use this constructor to create a new active Event.
    public Event(@Nullable String title, @Nullable String note, long startDate, long endDate, int state, boolean durableEvent, int priority, int reminder, String category) {
        this(title, note, startDate, endDate, state, durableEvent, priority, UUID.randomUUID().toString(), reminder, category, System.currentTimeMillis(), null, null, null, null, false, false);
    }

    public Event(@Nullable String title, @Nullable String note, long startDate, long endDate, int state, boolean durableEvent, int priority, String id, int reminder, String category, long creationDate) {
        this(title, note, startDate, endDate, state, durableEvent, priority, id, reminder, category, creationDate, null, null, null, null, false, false);
    }

    public Event(@Nullable String title, @Nullable String note, long startDate, long endDate, int state, boolean durableEvent, int priority, String id, int reminder, String category, long creationDate, @Nullable String imageUri, @Nullable String source, @Nullable String sourceCalendarId, @Nullable String sourceEventId, boolean externalEvent, boolean nowBarEnabled) {
        mId = id;
        mCreationDate = creationDate;
        mTitle = title;
        mNote = note;
        mStartDate = startDate;
        mEndDate = endDate;
        mState = state;
        mDurableEvent = durableEvent;
        mPriority = priority;
        mReminder = reminder;
        mCategory = category;
        mImageUri = imageUri;
        mSource = source;
        mSourceCalendarId = sourceCalendarId;
        mSourceEventId = sourceEventId;
        mExternalEvent = externalEvent;
        mNowBarEnabled = nowBarEnabled;
    }

    @NonNull
    public String getId() {
        return mId;
    }

    public long getCreationDate() {
        return mCreationDate;
    }

    @Nullable
    public String getTitle() {
        return mTitle;
    }

    @Nullable
    public String getNote() {
        return mNote;
    }

    public long getStartDate() {
        return mStartDate;
    }

    public long getEndDate() {
        return mEndDate;
    }

    public int getState() {
        return mState;
    }

    public boolean isDurableEvent() {
        return mDurableEvent;
    }

    public int getPriority() {
        return mPriority;
    }

    public int getReminder() {
        return mReminder;
    }

    public String getCategory() {
        return mCategory;
    }

    @Nullable
    public String getImageUri() {
        return mImageUri;
    }

    @Nullable
    public String getSource() {
        return mSource;
    }

    @Nullable
    public String getSourceCalendarId() {
        return mSourceCalendarId;
    }

    @Nullable
    public String getSourceEventId() {
        return mSourceEventId;
    }

    public boolean isExternalEvent() {
        return mExternalEvent;
    }

    public boolean isNowBarEnabled() {
        return mNowBarEnabled;
    }

    public void setTitle(@Nullable String title) {
        mTitle = title;
    }

    public void setEndDate(long endDate) {
        mEndDate = endDate;
    }

    public void setImageUri(@Nullable String imageUri) {
        mImageUri = imageUri;
    }

    public void setSource(@Nullable String source) {
        mSource = source;
    }

    public void setSourceCalendarId(@Nullable String sourceCalendarId) {
        mSourceCalendarId = sourceCalendarId;
    }

    public void setSourceEventId(@Nullable String sourceEventId) {
        mSourceEventId = sourceEventId;
    }

    public void setExternalEvent(boolean externalEvent) {
        mExternalEvent = externalEvent;
    }

    public void setNowBarEnabled(boolean nowBarEnabled) {
        mNowBarEnabled = nowBarEnabled;
    }
}

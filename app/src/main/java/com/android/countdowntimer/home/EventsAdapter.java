package com.android.countdowntimer.home;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.countdowntimer.R;
import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class EventsAdapter extends RecyclerView.Adapter<EventsAdapter.ViewHolder> implements EventTouchHelperListener {

    private EventItemActionListener mEventItemActionListener;
    private final LayoutInflater mInflater;
    private List<Event> mEvents;

    @Override
    public void onItemSwipeToStart(int position) {
        mEventItemActionListener.onItemSwiped(mEvents.get(position).getId());
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final MaterialCardView mCard;
        private final TextView mTitle;
        private final TextView mSource;
        private final TextView mDaysNumber;
        private final ImageView mImage;

        private ViewHolder(View itemView) {
            super(itemView);
            mCard = itemView.findViewById(R.id.event_card);
            mTitle = itemView.findViewById(R.id.event_title);
            mSource = itemView.findViewById(R.id.event_source);
            mDaysNumber = itemView.findViewById(R.id.event_days_number);
            mImage = itemView.findViewById(R.id.event_image);
        }

        private void bind(Event event) {
            mTitle.setText(event.getTitle());

            if (TextUtils.isEmpty(event.getSource())) {
                mSource.setVisibility(View.GONE);
            } else {
                mSource.setVisibility(View.VISIBLE);
                mSource.setText(event.getSource());
            }

            long remainingMs = event.getEndDate() - System.currentTimeMillis();
            long days = Math.max(0L, TimeUnit.MILLISECONDS.toDays(remainingMs));
            mDaysNumber.setText(String.valueOf(days));

            if (TextUtils.isEmpty(event.getImageUri())) {
                mImage.setImageResource(R.mipmap.ic_launcher_round);
            } else {
                Glide.with(mImage).load(event.getImageUri()).centerCrop().into(mImage);
            }
        }
    }

    EventsAdapter(Context context) {
        mInflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = mInflater.inflate(R.layout.item_event, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        if (mEvents == null) {
            return;
        }
        final Event current = mEvents.get(position);
        holder.bind(current);

        if (mEventItemActionListener != null) {
            holder.mCard.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mEventItemActionListener.onItemClicked(current.getId());
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        if (mEvents != null) {
            return mEvents.size();
        } else {
            return 0;
        }
    }

    public void setEvents(List<Event> events) {
        mEvents = events;
        notifyDataSetChanged();
    }

    public void setEventItemActionListener(EventItemActionListener listener) {
        mEventItemActionListener = listener;
    }
}

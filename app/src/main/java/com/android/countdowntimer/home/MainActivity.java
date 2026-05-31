package com.android.countdowntimer.home;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.android.countdowntimer.R;
import com.android.countdowntimer.completedevents.CompletedEventsActivity;
import com.android.countdowntimer.utils.DateTimeUtils;
import com.android.countdowntimer.utils.NotificationUtils;
import com.android.countdowntimer.utils.RemindType;
import com.android.countdowntimer.utils.ReminderUtils;
import com.android.countdowntimer.utils.Utils;
import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.paperdb.Paper;

public class MainActivity extends AppCompatActivity {

    public static final String EVENT_NOTIFICATION_ID = "EVENT_NOTIFICATION";
    private static final int NOW_BAR_NOTIFICATION_ID = 2026;
    private static final String DIALOG_DATE = "DATE";
    private static final String BOOK_KEY_LEGACY_EVENTS = "events";
    private static final String BOOK_KEY_APP_EVENTS = "app_events";
    private static final String BOOK_KEY_COMPLETED_EVENTS = "completed_events";
    private static final String BOOK_KEY_SELECTED_CALENDARS = "selected_calendars";
    private static final String BOOK_KEY_EXTERNAL_OVERRIDES = "external_overrides";

    private EventsAdapter mEventsAdapter;
    private final List<Event> appEvents = new ArrayList<>();
    private final List<Event> displayEvents = new ArrayList<>();
    private final Map<String, Event> externalOverrides = new HashMap<>();
    private Set<String> selectedCalendarIds = new HashSet<>();

    private String pendingName;
    private String pendingSource;
    private String pendingImageUri;
    private String pendingDateEditEventId;
    private ImageView addEventImagePreview;

    private final ActivityResultLauncher<PickVisualMediaRequest> pickMediaLauncher =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri == null) {
                    return;
                }
                pendingImageUri = uri.toString();
                if (addEventImagePreview != null) {
                    Glide.with(addEventImagePreview).load(uri).centerCrop().into(addEventImagePreview);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_main);
        Toolbar toolbar = findViewById(R.id.tasks_toolbar);
        setSupportActionBar(toolbar);
        Utils.setupSystemUI(this);
        ensureNotificationChannel();
        setupEventList();
        loadData();
        refreshAllEvents();

        FloatingActionButton fab = findViewById(R.id.fab_add);
        fab.setOnClickListener(view -> showAddEventDialog());
    }

    private void ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(EVENT_NOTIFICATION_ID, getString(R.string.event_notification), NotificationManager.IMPORTANCE_HIGH);
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.createNotificationChannel(channel);
    }

    private void setupEventList() {
        RecyclerView recyclerView = findViewById(R.id.list_events);
        mEventsAdapter = new EventsAdapter(this);
        recyclerView.setAdapter(mEventsAdapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(new EventTouchHelperCallback(mEventsAdapter));
        touchHelper.attachToRecyclerView(recyclerView);
        mEventsAdapter.setEventItemActionListener(new EventItemActionListener() {
            @Override
            public void onItemSwiped(String eventId) {
                deleteEvent(eventId);
                refreshAllEvents();
            }

            @Override
            public void onItemClicked(String eventId) {
                showEventOptions(eventId);
            }
        });
    }

    private void loadData() {
        List<Event> storedAppEvents = Paper.book().read(BOOK_KEY_APP_EVENTS);
        if (storedAppEvents == null) {
            storedAppEvents = Paper.book().read(BOOK_KEY_LEGACY_EVENTS);
        }
        if (storedAppEvents != null) {
            appEvents.addAll(storedAppEvents);
        }
        Map<String, Event> storedOverrides = Paper.book().read(BOOK_KEY_EXTERNAL_OVERRIDES);
        if (storedOverrides != null) {
            externalOverrides.putAll(storedOverrides);
        }
        Set<String> storedCalendars = Paper.book().read(BOOK_KEY_SELECTED_CALENDARS);
        if (storedCalendars != null) {
            selectedCalendarIds = storedCalendars;
        }
    }

    private void showAddEventDialog() {
        pendingName = null;
        pendingSource = null;
        pendingImageUri = null;

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_event, null);
        EditText nameInput = dialogView.findViewById(R.id.event_name_input);
        EditText sourceInput = dialogView.findViewById(R.id.event_source_input);
        addEventImagePreview = dialogView.findViewById(R.id.event_image_preview);
        dialogView.findViewById(R.id.event_pick_image).setOnClickListener(v ->
                pickMediaLauncher.launch(new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build()));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.add_event)
                .setView(dialogView)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        dialog.setOnShowListener(dialogInterface -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = nameInput.getText() == null ? "" : nameInput.getText().toString().trim();
            if (TextUtils.isEmpty(name)) {
                Toast.makeText(getApplicationContext(), R.string.invalid_event_name, Toast.LENGTH_LONG).show();
                return;
            }
            pendingName = name;
            pendingSource = sourceInput.getText() == null ? "" : sourceInput.getText().toString().trim();
            FragmentManager fm = getSupportFragmentManager();
            DateDialogFragment dialogFragment = DateDialogFragment.newInstance(DateTimeUtils.getCurrentTimeWithoutSec(), false);
            dialogFragment.show(fm, DIALOG_DATE);
            dialog.dismiss();
        }));
        dialog.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK || data == null) {
            return;
        }
        long endDate = data.getLongExtra(DateDialogFragment.EXTRA_DATE, 0);
        if (endDate - System.currentTimeMillis() < 0) {
            Toast.makeText(getApplicationContext(), R.string.end_date_in_past, Toast.LENGTH_LONG).show();
            return;
        }

        if (!TextUtils.isEmpty(pendingDateEditEventId)) {
            updateEventDate(pendingDateEditEventId, endDate);
            pendingDateEditEventId = null;
            return;
        }

        String eventId = UUID.randomUUID().toString();
        Event event = new Event(
                pendingName,
                "",
                System.currentTimeMillis(),
                endDate,
                StateType.ONGOING,
                false,
                0,
                eventId,
                0,
                "",
                System.currentTimeMillis(),
                pendingImageUri,
                TextUtils.isEmpty(pendingSource) ? null : pendingSource,
                null,
                null,
                false,
                false
        );
        appEvents.add(event);
        NotificationUtils.buildNormalReminder(getApplication(), endDate, pendingName, RemindType.SINGLE_DUE_DATE, ReminderUtils.getSingleRemindInterval(RemindType.SINGLE_DUE_DATE), eventId);
        saveAppEvents();
        refreshAllEvents();
    }

    private void updateEventDate(String eventId, long endDate) {
        Event event = findDisplayEventById(eventId);
        if (event == null) {
            return;
        }
        event.setEndDate(endDate);
        persistEventChange(event);
        refreshAllEvents();
    }

    private void showEventOptions(String eventId) {
        final Event event = findDisplayEventById(eventId);
        if (event == null) {
            return;
        }
        List<String> options = new ArrayList<>();
        options.add(getString(R.string.edit_event));
        options.add(event.isNowBarEnabled() ? getString(R.string.disable_now_bar) : getString(R.string.enable_now_bar));
        if (event.isExternalEvent()) {
            options.add(getString(R.string.reset_external_changes));
        }

        new AlertDialog.Builder(this)
                .setTitle(event.getTitle())
                .setItems(options.toArray(new CharSequence[0]), (dialog, which) -> {
                    String selected = options.get(which);
                    if (selected.equals(getString(R.string.edit_event))) {
                        showEditEventDialog(event);
                    } else if (selected.equals(getString(R.string.enable_now_bar)) || selected.equals(getString(R.string.disable_now_bar))) {
                        event.setNowBarEnabled(!event.isNowBarEnabled());
                        persistEventChange(event);
                        refreshAllEvents();
                    } else if (selected.equals(getString(R.string.reset_external_changes))) {
                        if (!TextUtils.isEmpty(event.getSourceEventId())) {
                            externalOverrides.remove(event.getSourceEventId());
                            saveExternalOverrides();
                            refreshAllEvents();
                        }
                    }
                })
                .show();
    }

    private void showEditEventDialog(Event event) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_event, null);
        EditText nameInput = dialogView.findViewById(R.id.event_name_input);
        EditText sourceInput = dialogView.findViewById(R.id.event_source_input);
        addEventImagePreview = dialogView.findViewById(R.id.event_image_preview);
        nameInput.setText(event.getTitle());
        sourceInput.setText(event.getSource());
        if (!TextUtils.isEmpty(event.getImageUri())) {
            Glide.with(addEventImagePreview).load(event.getImageUri()).centerCrop().into(addEventImagePreview);
        }

        pendingImageUri = event.getImageUri();
        dialogView.findViewById(R.id.event_pick_image).setOnClickListener(v ->
                pickMediaLauncher.launch(new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build()));

        new AlertDialog.Builder(this)
                .setTitle(R.string.edit_event)
                .setView(dialogView)
                .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                    String name = nameInput.getText() == null ? "" : nameInput.getText().toString().trim();
                    if (TextUtils.isEmpty(name)) {
                        Toast.makeText(this, R.string.invalid_event_name, Toast.LENGTH_LONG).show();
                        return;
                    }
                    event.setTitle(name);
                    event.setSource(TextUtils.isEmpty(sourceInput.getText()) ? null : sourceInput.getText().toString().trim());
                    event.setImageUri(pendingImageUri);
                    persistEventChange(event);
                    pendingDateEditEventId = event.getId();
                    DateDialogFragment.newInstance(event.getEndDate(), false).show(getSupportFragmentManager(), DIALOG_DATE);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void deleteEvent(String id) {
        Event event = findDisplayEventById(id);
        if (event == null) {
            return;
        }
        if (event.isExternalEvent()) {
            Toast.makeText(this, R.string.external_delete_not_supported, Toast.LENGTH_SHORT).show();
            return;
        }
        Iterator<Event> iterator = appEvents.iterator();
        while (iterator.hasNext()) {
            Event appEvent = iterator.next();
            if (appEvent.getId().equals(id)) {
                addToCompletedEvents(appEvent);
                iterator.remove();
                saveAppEvents();
                return;
            }
        }
    }

    private void addToCompletedEvents(Event event) {
        List<Event> completedEvents = Paper.book().read(BOOK_KEY_COMPLETED_EVENTS);
        if (completedEvents == null) {
            completedEvents = new ArrayList<>();
        }
        completedEvents.add(event);
        Paper.book().write(BOOK_KEY_COMPLETED_EVENTS, completedEvents);
    }

    private void saveAppEvents() {
        Paper.book().write(BOOK_KEY_APP_EVENTS, appEvents);
    }

    private void saveExternalOverrides() {
        Paper.book().write(BOOK_KEY_EXTERNAL_OVERRIDES, externalOverrides);
    }

    private void persistEventChange(Event event) {
        if (event.isExternalEvent()) {
            if (!TextUtils.isEmpty(event.getSourceEventId())) {
                externalOverrides.put(event.getSourceEventId(), event);
                saveExternalOverrides();
            }
            return;
        }
        for (int i = 0; i < appEvents.size(); i++) {
            if (appEvents.get(i).getId().equals(event.getId())) {
                appEvents.set(i, event);
                saveAppEvents();
                return;
            }
        }
    }

    private void refreshAllEvents() {
        filterCompletedEvents();
        displayEvents.clear();
        displayEvents.addAll(appEvents);
        displayEvents.addAll(loadExternalEvents());
        Collections.sort(displayEvents, Comparator.comparingLong(Event::getEndDate));
        mEventsAdapter.setEvents(displayEvents);
        updateEmptyState();
        updateNowBarNotification();
    }

    private void filterCompletedEvents() {
        Iterator<Event> iterator = appEvents.iterator();
        boolean changed = false;
        while (iterator.hasNext()) {
            Event event = iterator.next();
            if (event.getEndDate() < System.currentTimeMillis()) {
                addToCompletedEvents(event);
                iterator.remove();
                changed = true;
            }
        }
        if (changed) {
            saveAppEvents();
        }
    }

    private void updateEmptyState() {
        View emptyView = findViewById(R.id.empty_view);
        emptyView.setVisibility(displayEvents.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private Event findDisplayEventById(String eventId) {
        for (Event event : displayEvents) {
            if (event.getId().equals(eventId)) {
                return event;
            }
        }
        return null;
    }

    private List<Event> loadExternalEvents() {
        if (!hasCalendarPermission() || selectedCalendarIds == null || selectedCalendarIds.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, String> calendarNames = getCalendarNameMap();
        List<Event> externalEvents = new ArrayList<>();
        ContentResolver resolver = getContentResolver();
        String selection = CalendarContract.Events.CALENDAR_ID + " IN (" + makeSelectionPlaceholders(selectedCalendarIds.size()) + ")";
        String[] selectionArgs = selectedCalendarIds.toArray(new String[0]);

        Cursor cursor = resolver.query(
                CalendarContract.Events.CONTENT_URI,
                new String[]{
                        CalendarContract.Events._ID,
                        CalendarContract.Events.CALENDAR_ID,
                        CalendarContract.Events.TITLE,
                        CalendarContract.Events.DTSTART,
                        CalendarContract.Events.DTEND
                },
                selection,
                selectionArgs,
                CalendarContract.Events.DTSTART + " ASC"
        );

        if (cursor == null) {
            return externalEvents;
        }

        try {
            while (cursor.moveToNext()) {
                String eventId = cursor.getString(0);
                String calendarId = cursor.getString(1);
                String title = cursor.getString(2);
                long start = cursor.getLong(3);
                long end = cursor.getLong(4);
                if (end <= 0) {
                    end = start;
                }
                if (end < System.currentTimeMillis()) {
                    continue;
                }
                Event event = new Event(
                        title,
                        "",
                        start,
                        end,
                        StateType.ONGOING,
                        false,
                        0,
                        "ext_" + eventId,
                        0,
                        "",
                        start,
                        null,
                        calendarNames.get(calendarId),
                        calendarId,
                        eventId,
                        true,
                        false
                );
                applyExternalOverride(event);
                externalEvents.add(event);
            }
        } finally {
            cursor.close();
        }

        return externalEvents;
    }

    private void applyExternalOverride(Event event) {
        if (TextUtils.isEmpty(event.getSourceEventId())) {
            return;
        }
        Event override = externalOverrides.get(event.getSourceEventId());
        if (override == null) {
            return;
        }
        event.setTitle(override.getTitle());
        event.setEndDate(override.getEndDate());
        event.setImageUri(override.getImageUri());
        event.setSource(override.getSource());
        event.setNowBarEnabled(override.isNowBarEnabled());
    }

    private Map<String, String> getCalendarNameMap() {
        Map<String, String> names = new HashMap<>();
        if (!hasCalendarPermission()) {
            return names;
        }
        Cursor cursor = getContentResolver().query(
                CalendarContract.Calendars.CONTENT_URI,
                new String[]{CalendarContract.Calendars._ID, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME},
                null,
                null,
                null
        );
        if (cursor == null) {
            return names;
        }
        try {
            while (cursor.moveToNext()) {
                names.put(cursor.getString(0), cursor.getString(1));
            }
        } finally {
            cursor.close();
        }
        return names;
    }

    private String makeSelectionPlaceholders(int count) {
        if (count <= 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder("?");
        for (int i = 1; i < count; i++) {
            builder.append(",?");
        }
        return builder.toString();
    }

    private boolean hasCalendarPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCalendarPermissionThenShowPicker() {
        if (hasCalendarPermission()) {
            showCalendarSelectionDialog();
            return;
        }
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CALENDAR}, 2200);
    }

    private void showCalendarSelectionDialog() {
        List<String> ids = new ArrayList<>();
        List<String> titles = new ArrayList<>();
        Cursor cursor = getContentResolver().query(
                CalendarContract.Calendars.CONTENT_URI,
                new String[]{CalendarContract.Calendars._ID, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME},
                null,
                null,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME + " ASC"
        );
        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    ids.add(cursor.getString(0));
                    titles.add(cursor.getString(1));
                }
            } finally {
                cursor.close();
            }
        }
        if (ids.isEmpty()) {
            Toast.makeText(this, R.string.no_calendars_found, Toast.LENGTH_LONG).show();
            return;
        }
        boolean[] checked = new boolean[ids.size()];
        for (int i = 0; i < ids.size(); i++) {
            checked[i] = selectedCalendarIds.contains(ids.get(i));
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.select_sync_calendars)
                .setMultiChoiceItems(titles.toArray(new CharSequence[0]), checked, (dialog, which, isChecked) -> checked[which] = isChecked)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    selectedCalendarIds = new HashSet<>();
                    for (int i = 0; i < ids.size(); i++) {
                        if (checked[i]) {
                            selectedCalendarIds.add(ids.get(i));
                        }
                    }
                    Paper.book().write(BOOK_KEY_SELECTED_CALENDARS, selectedCalendarIds);
                    refreshAllEvents();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 2200 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            showCalendarSelectionDialog();
            return;
        }
        if (requestCode == 2200) {
            Toast.makeText(this, R.string.calendar_permission_needed, Toast.LENGTH_LONG).show();
        }
    }

    private void updateNowBarNotification() {
        Event nowBarEvent = null;
        for (Event event : displayEvents) {
            if (!event.isNowBarEnabled()) {
                continue;
            }
            if (nowBarEvent == null || event.getEndDate() < nowBarEvent.getEndDate()) {
                nowBarEvent = event;
            }
        }
        NotificationManagerCompat manager = NotificationManagerCompat.from(this);
        if (nowBarEvent == null) {
            manager.cancel(NOW_BAR_NOTIFICATION_ID);
            return;
        }
        long days = Math.max(0, TimeUnit.MILLISECONDS.toDays(nowBarEvent.getEndDate() - System.currentTimeMillis()));
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, EVENT_NOTIFICATION_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(nowBarEvent.getTitle())
                .setContentText(getString(R.string.now_bar_days_left, days))
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            manager.notify(NOW_BAR_NOTIFICATION_ID, builder.build());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_show_completed_events) {
            showCompletedEvents();
            return true;
        }
        if (itemId == R.id.action_export_events) {
            exportEventsToJson();
            return true;
        }
        if (itemId == R.id.action_sync_calendars) {
            requestCalendarPermissionThenShowPicker();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void exportEventsToJson() {
        try {
            Gson gson = new Gson();
            String userJson = gson.toJson(appEvents);
            File folder = getExternalFilesDir(null);
            if (folder == null) {
                Toast.makeText(getApplicationContext(), R.string.export_failed, Toast.LENGTH_LONG).show();
                return;
            }
            File file = new File(folder, "CountdownTimer.json");
            FileOutputStream outputStream = new FileOutputStream(file, false);
            outputStream.write(userJson.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
            outputStream.close();
            Toast.makeText(getApplicationContext(), getString(R.string.export_success, file.getAbsolutePath()), Toast.LENGTH_LONG).show();
        } catch (Exception exception) {
            Toast.makeText(getApplicationContext(), R.string.export_failed, Toast.LENGTH_LONG).show();
        }
    }

    private void showCompletedEvents() {
        Intent intent = new Intent(MainActivity.this, CompletedEventsActivity.class);
        startActivity(intent);
    }
}

/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wdullaer.materialdatetimepicker.date;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import android.view.ViewGroup;
import android.widget.AbsListView.LayoutParams;

import com.wdullaer.materialdatetimepicker.date.MonthAdapter.MonthViewHolder;
import com.wdullaer.materialdatetimepicker.date.MonthView.OnDayClickListener;

import java.util.Calendar;
import java.util.HashSet;
import java.util.TimeZone;

/**
 * An adapter for a list of {@link MonthView} items.
 */
@SuppressWarnings("WeakerAccess")
public abstract class MonthAdapter extends RecyclerView.Adapter<MonthViewHolder> implements OnDayClickListener {

    protected final DatePickerController mController;

    private HashSet<CalendarDay> mSelectedDays;

    protected static final int MONTHS_IN_YEAR = 12;

    /**
     * A convenience class to represent a specific date.
     */
    public static class CalendarDay {
        private Calendar calendar;
        int year;
        int month;
        int day;
        TimeZone mTimeZone;

        public CalendarDay(TimeZone timeZone) {
            mTimeZone = timeZone;
            setTime(System.currentTimeMillis());
        }

        public CalendarDay(long timeInMillis, TimeZone timeZone) {
            mTimeZone = timeZone;
            setTime(timeInMillis);
        }

        public CalendarDay(Calendar calendar, TimeZone timeZone) {
            mTimeZone = timeZone;
            year = calendar.get(Calendar.YEAR);
            month = calendar.get(Calendar.MONTH);
            day = calendar.get(Calendar.DAY_OF_MONTH);
        }

        @SuppressWarnings("unused")
        public CalendarDay(int year, int month, int day) {
            setDay(year, month, day);
        }

        public CalendarDay(int year, int month, int day, TimeZone timezone) {
            mTimeZone = timezone;
            setDay(year, month, day);
        }

        public void set(CalendarDay date) {
            year = date.year;
            month = date.month;
            day = date.day;
        }

        public void setDay(int year, int month, int day) {
            this.year = year;
            this.month = month;
            this.day = day;
        }

        private void setTime(long timeInMillis) {
            if (calendar == null) {
                calendar = Calendar.getInstance(mTimeZone);
            }
            calendar.setTimeInMillis(timeInMillis);
            month = calendar.get(Calendar.MONTH);
            year = calendar.get(Calendar.YEAR);
            day = calendar.get(Calendar.DAY_OF_MONTH);
        }

        public int getYear() {
            return year;
        }

        public int getMonth() {
            return month;
        }

        public int getDay() {
            return day;
        }

        @Override
        public boolean equals(@Nullable Object other) {
            if (other == null) return false;
            if (other == this) return true;
            if (!(other instanceof CalendarDay)) return false;
            CalendarDay otherParsed = (CalendarDay) other;
            return otherParsed.day == day && otherParsed.month == month && otherParsed.year == year;
        }

        @Override
        public int hashCode() {
            int result = 17;
            result = 31 * result + year;
            result = 31 * result + month;
            result = 31 * result + day;
            return result;
        }
    }

    public MonthAdapter(DatePickerController controller) {
        mController = controller;
        init();
        addSelectedDays(mController.getSelectedDays());
        setHasStableIds(true);
    }

    /**
     * Updates the selected day and related parameters.
     *
     * @param day The days to add to the highlights
     */
    public void toggleSelectedDay(CalendarDay day) {
        if (mController.allowMultipleSelection()) {
            if (mSelectedDays.contains(day)) {
                mSelectedDays.remove(day);
            } else {
                mSelectedDays.add(day);
            }
        } else {
            mSelectedDays.clear();
            mSelectedDays.add(day);
        }
        notifyDataSetChanged();
    }

    public void addSelectedDays(HashSet<CalendarDay> days) {
        mSelectedDays.addAll(days);
        notifyDataSetChanged();
    }

    @SuppressWarnings("unused")
    public HashSet<CalendarDay> getSelectedDays() {
        return mSelectedDays;
    }

    /**
     * Set up the gesture detector and selected time
     */
    protected void init() {
        mSelectedDays = new HashSet<>();
        mSelectedDays.add(new CalendarDay(System.currentTimeMillis(), mController.getTimeZone()));
    }

    @Override
    @NonNull
    public MonthViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        MonthView v = createMonthView(parent.getContext());
        // Set up the new view
        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        v.setLayoutParams(params);
        v.setClickable(true);
        v.setOnDayClickListener(this);

        return new MonthViewHolder(v);
    }

    @Override public void onBindViewHolder(@NonNull MonthViewHolder holder, int position) {
        holder.bind(position, mController, mSelectedDays);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override public int getItemCount() {
        Calendar endDate = mController.getEndDate();
        Calendar startDate = mController.getStartDate();
        int endMonth = endDate.get(Calendar.YEAR) * MONTHS_IN_YEAR + endDate.get(Calendar.MONTH);
        int startMonth = startDate.get(Calendar.YEAR) * MONTHS_IN_YEAR + startDate.get(Calendar.MONTH);
        return endMonth - startMonth + 1;
    }

    public abstract MonthView createMonthView(Context context);

    @Override
    public void onDayClick(MonthView view, CalendarDay day) {
        if (day != null) {
            onDayTapped(day);
        }
    }

    /**
     * Maintains the same hour/min/sec but moves the day to the tapped day.
     *
     * @param day The day that was tapped
     */
    protected void onDayTapped(CalendarDay day) {
        mController.tryVibrate();
        mController.onDayOfMonthSelected(day);
        toggleSelectedDay(day);
    }

    static class MonthViewHolder extends RecyclerView.ViewHolder {

        public MonthViewHolder(MonthView itemView) {
            super(itemView);

        }

        void bind(int position, DatePickerController mController, HashSet<CalendarDay> selectedDays) {
            final int month = (position + mController.getStartDate().get(Calendar.MONTH)) % MONTHS_IN_YEAR;
            final int year = (position + mController.getStartDate().get(Calendar.MONTH)) / MONTHS_IN_YEAR + mController.getMinYear();

            HashSet<Integer> selectedDaysInMonth = getSelectedDaysInMonth(selectedDays, year, month);

            ((MonthView) itemView).setMonthParams(selectedDaysInMonth, year, month, mController.getFirstDayOfWeek());
            this.itemView.invalidate();
        }

        private HashSet<Integer> getSelectedDaysInMonth(HashSet<CalendarDay> selectedDays, int year, int month) {
            HashSet<Integer> result = new HashSet<>();
            for (CalendarDay day: selectedDays) {
                if (day.year == year && day.month == month) result.add(day.day);
            }
            return result;
        }
    }
}

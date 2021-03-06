/*
 * Copyright (C) 2016 - present  Instructure, Inc.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.instructure.candroid.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.instructure.candroid.R;
import com.instructure.candroid.binders.AssignmentBinder;
import com.instructure.candroid.binders.EmptyBinder;
import com.instructure.candroid.binders.ExpandableHeaderBinder;
import com.instructure.candroid.holders.AssignmentViewHolder;
import com.instructure.candroid.holders.EmptyViewHolder;
import com.instructure.candroid.holders.ExpandableViewHolder;
import com.instructure.candroid.interfaces.AdapterToAssignmentsCallback;
import com.instructure.candroid.interfaces.GradingPeriodsCallback;
import com.instructure.canvasapi.api.AssignmentAPI;
import com.instructure.canvasapi.api.CourseAPI;
import com.instructure.canvasapi.model.Assignment;
import com.instructure.canvasapi.model.AssignmentGroup;
import com.instructure.canvasapi.model.CanvasContext;
import com.instructure.canvasapi.model.Course;
import com.instructure.canvasapi.model.Enrollment;
import com.instructure.canvasapi.model.GradingPeriod;
import com.instructure.canvasapi.model.GradingPeriodResponse;
import com.instructure.canvasapi.utilities.CanvasCallback;
import com.instructure.canvasapi.utilities.LinkHeaders;
import com.instructure.pandarecycler.util.GroupSortedList;
import com.instructure.pandarecycler.util.Types;
import com.instructure.pandautils.utils.CanvasContextColor;

import retrofit.RetrofitError;
import retrofit.client.Response;

public class AssignmentGroupListRecyclerAdapter extends ExpandableRecyclerAdapter<AssignmentGroup, Assignment, RecyclerView.ViewHolder> implements GradingPeriodsCallback {

    private CanvasContext mCanvasContext;
    private AdapterToAssignmentsCallback mAdapterToAssignmentsCallback;
    private CanvasCallback<GradingPeriodResponse> mGradingPeriodsCallback;
    private CanvasCallback<AssignmentGroup[]> mAssignmentGroupCallback;
    private GradingPeriod mCurrentGradingPeriod;


    /* For testing purposes only */
    protected AssignmentGroupListRecyclerAdapter(Context context){
        super(context, AssignmentGroup.class, Assignment.class);
    }

    public AssignmentGroupListRecyclerAdapter(Context context, CanvasContext canvasContext,
        CanvasCallback<GradingPeriodResponse> gradingPeriodsCallback,
        AdapterToAssignmentsCallback adapterToAssignmentsCallback) {
        super(context, AssignmentGroup.class, Assignment.class);

        mCanvasContext = canvasContext;
        mGradingPeriodsCallback = gradingPeriodsCallback;
        mAdapterToAssignmentsCallback = adapterToAssignmentsCallback;
        setExpandedByDefault(true);
        setDisplayEmptyCell(true);

        loadData();
    }

    @Override
    public void setupCallbacks() {
        mAssignmentGroupCallback = new CanvasCallback<AssignmentGroup[]>(this) {
            @Override
            public void firstPage(AssignmentGroup[] assignmentGroups, LinkHeaders linkHeaders, Response response) {
                for (AssignmentGroup assignmentGroup : assignmentGroups) {
                    addOrUpdateAllItems(assignmentGroup, assignmentGroup.getAssignments());
                }
                mAdapterToAssignmentsCallback.onRefreshFinished();
                mAdapterToAssignmentsCallback.setTermSpinnerState(true);
                setAllPagesLoaded(true);
            }

            @Override
            public boolean onFailure(RetrofitError retrofitError) {
                mAdapterToAssignmentsCallback.setTermSpinnerState(true);
                return super.onFailure(retrofitError);
            }
        };
    }

    @Override
    public RecyclerView.ViewHolder createViewHolder(View v, int viewType) {
        if (viewType == Types.TYPE_HEADER) {
            return new ExpandableViewHolder(v);
        } else if (viewType == Types.TYPE_EMPTY_CELL) {
            return new EmptyViewHolder(v);
        } else {
            return new AssignmentViewHolder(v);
        }
    }

    @Override
    public int itemLayoutResId(int viewType) {
        if (viewType == Types.TYPE_HEADER) {
            return ExpandableViewHolder.holderResId();
        }  else if (viewType == Types.TYPE_EMPTY_CELL) {
            return EmptyViewHolder.holderResId();
        } else {
            return AssignmentViewHolder.holderResId();
        }
    }

    @Override
    public void contextReady() {

    }

    @Override
    public void onBindChildHolder(RecyclerView.ViewHolder holder, AssignmentGroup assignmentGroup, Assignment assignment) {
        AssignmentBinder.bind(getContext(), (AssignmentViewHolder) holder, assignment, CanvasContextColor.getCachedColor(getContext(),
                mCanvasContext), mAdapterToAssignmentsCallback);
    }

    @Override
    public void onBindEmptyHolder(RecyclerView.ViewHolder holder, AssignmentGroup assignmentGroup) {
        EmptyBinder.bind((EmptyViewHolder) holder, getContext().getResources().getString(R.string.noAssignmentsInGroup));
    }

    @Override
    public void onBindHeaderHolder(RecyclerView.ViewHolder holder, AssignmentGroup assignmentGroup, boolean isExpanded) {
        ExpandableHeaderBinder.bind(getContext(), mCanvasContext, (ExpandableViewHolder) holder, assignmentGroup, assignmentGroup.getName(), isExpanded, getViewHolderHeaderClicked());
    }

    @Override
    public void loadData() {
        /*Logic regarding MGP is similar here as it is in both assignment recycler adapters,
            if changes are made here, check if they are needed in the other recycler adapters.*/
        Course course = (Course)mCanvasContext;

        //This check is for the "all grading periods" option
        if (mCurrentGradingPeriod != null && mCurrentGradingPeriod.getTitle() != null
                && mCurrentGradingPeriod.getTitle().equals(getContext().getString(R.string.allGradingPeriods))) {
            loadAssignment();
            return;
        }

        for (Enrollment enrollment : course.getEnrollments()) {
            //Group list is for teachers but mgp == true won't show on teacher enrollments,
            //so we'll check the first student enrollment they have for the course
            if (enrollment.isStudent() && enrollment.isMultipleGradingPeriodsEnabled()) {
                if(mCurrentGradingPeriod == null || mCurrentGradingPeriod.getTitle() == null) {
                    //we load current term by setting up the current GP
                    mCurrentGradingPeriod = new GradingPeriod();
                    mCurrentGradingPeriod.setId(enrollment.getCurrentGradingPeriodId());
                    mCurrentGradingPeriod.setTitle(enrollment.getCurrentGradingPeriodTitle());
                    //request the grading period objects and make the assignment calls
                    //This callback is fulfilled in the grade list fragment.
                    CourseAPI.getGradingPeriodsForCourse(course.getId(), mGradingPeriodsCallback);
                    //Then we go ahead and load up the assignments for the current period
                    loadAssignmentsForGradingPeriod(mCurrentGradingPeriod.getId(), false);
                    return;
                } else {
                    //Otherwise we load the info from the currently selected grading period
                    loadAssignmentsForGradingPeriod(mCurrentGradingPeriod.getId(), true);
                    return;
                }
            }
        }

        //If we made it this far, MGP is disabled so we just go forward with the standard
        loadAssignment();
    }

    @Override
    public void loadAssignmentsForGradingPeriod (long gradingPeriodID, boolean refreshFirst) {
        /*Logic regarding MGP is similar here as it is in both assignment recycler adapters,
            if changes are made here, check if they are needed in the other recycler adapters.*/
        if(refreshFirst){
            resetData();
        }
        //TODO: Add filter boolean when its real
        AssignmentAPI.getAssignmentGroupsListWithAssignmentsAndSubmissionsForGradingPeriod(mCanvasContext.getId(), gradingPeriodID, mAssignmentGroupCallback);
    }


    @Override
    public void loadAssignment () {
        AssignmentAPI.getAssignmentGroupsListWithAssignmentsAndSubmissions(mCanvasContext.getId(), mAssignmentGroupCallback);
    }

    @Override
    public GradingPeriod getCurrentGradingPeriod() {
        return mCurrentGradingPeriod;
    }

    @Override
    public void setCurrentGradingPeriod(GradingPeriod gradingPeriod) {
        mCurrentGradingPeriod = gradingPeriod;
    }

    // region Expandable callbacks
    @Override
    public GroupSortedList.GroupComparatorCallback<AssignmentGroup> createGroupCallback() {
        return new GroupSortedList.GroupComparatorCallback<AssignmentGroup>() {
            @Override
            public int compare(AssignmentGroup o1, AssignmentGroup o2) {
                return o1.getPosition() - o2.getPosition();
            }

            @Override
            public boolean areContentsTheSame(AssignmentGroup oldGroup, AssignmentGroup newGroup) {
                return oldGroup.getName().equals(newGroup.getName());
            }

            @Override
            public boolean areItemsTheSame(AssignmentGroup group1, AssignmentGroup group2) {
                return group1.getId() == group2.getId();
            }

            @Override
            public int getGroupType(AssignmentGroup group) {
                return Types.TYPE_HEADER;
            }

            @Override
            public long getUniqueGroupId(AssignmentGroup group) {
                return group.getId();
            }
        };
    }

    @Override
    public GroupSortedList.ItemComparatorCallback<AssignmentGroup, Assignment> createItemCallback() {
        return new GroupSortedList.ItemComparatorCallback<AssignmentGroup, Assignment>() {
            @Override
            public int compare(AssignmentGroup group, Assignment o1, Assignment o2) {
                return o1.getPosition() - o2.getPosition();
            }

            @Override
            public boolean areContentsTheSame(Assignment oldItem, Assignment newItem) {
                boolean isSameName = oldItem.getName().equals(newItem.getName());
                if (oldItem.getDueDate() != null && newItem.getDueDate() != null) {
                    return isSameName && oldItem.getDueDate().equals(newItem.getDueDate());
                } else if (oldItem.getDueDate() == null && newItem.getDueDate() != null) {
                    return false;
                } else if (oldItem.getDueDate() != null && newItem.getDueDate() == null) {
                    return false;
                }
                return isSameName;
            }

            @Override
            public boolean areItemsTheSame(Assignment item1, Assignment item2) {
                return item1.getId() == item2.getId();
            }

            @Override
            public int getChildType(AssignmentGroup group, Assignment item) {
                return Types.TYPE_ITEM;
            }

            @Override
            public long getUniqueItemId(Assignment item) {
                return item.getId();
            }
        };
    }

    // endregion
}

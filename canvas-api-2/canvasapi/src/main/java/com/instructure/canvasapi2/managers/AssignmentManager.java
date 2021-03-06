/*
 * Copyright (C) 2016 - present Instructure, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.instructure.canvasapi2.managers;

import android.support.annotation.NonNull;

import com.instructure.canvasapi2.StatusCallback;
import com.instructure.canvasapi2.apis.AssignmentAPI;
import com.instructure.canvasapi2.builders.RestBuilder;
import com.instructure.canvasapi2.builders.RestParams;
import com.instructure.canvasapi2.models.Assignment;
import com.instructure.canvasapi2.models.AssignmentGroup;
import com.instructure.canvasapi2.models.GradeableStudent;
import com.instructure.canvasapi2.models.Submission;
import com.instructure.canvasapi2.models.post_models.AssignmentPostBody;
import com.instructure.canvasapi2.models.post_models.AssignmentPostBodyWrapper;
import com.instructure.canvasapi2.tests.AssignmentManager_Test;
import com.instructure.canvasapi2.utils.DepaginatedCallback;

import java.util.List;
import java.util.Map;

public class AssignmentManager extends BaseManager {

    public static boolean mTesting = false;

    public static void getAssignment(long assignmentId, long courseId, boolean forceNetwork, StatusCallback<Assignment> callback) {
        if (isTesting() || mTesting) {
            AssignmentManager_Test.getAssignment(assignmentId, courseId, callback);
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withPerPageQueryParam(false)
                    .withShouldIgnoreToken(false)
                    .withForceReadFromNetwork(forceNetwork)
                    .build();
            AssignmentAPI.getAssignment(courseId, assignmentId, adapter, callback, params);
        }
    }

    public static void getAssignmentGroupsWithAssignments(long courseId, boolean forceNetwork, StatusCallback<List<AssignmentGroup>> callback) {
        if (isTesting() || mTesting) {
            AssignmentManager_Test.getAssignmentGroupsWithAssignments(courseId, callback);
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withPerPageQueryParam(true)
                    .withShouldIgnoreToken(false)
                    .withForceReadFromNetwork(forceNetwork)
                    .build();
            AssignmentAPI.getAssignmentGroupsWithAssignments(courseId, adapter, callback, params);
        }
    }

    public static void getAssignmentGroupsWithAssignmentsForGradingPeriod(long courseId, boolean forceNetwork, StatusCallback<List<AssignmentGroup>> callback, long gradingPeriodId) {
        if (isTesting() || mTesting) {
            AssignmentManager_Test.getAssignmentGroupsWithAssignmentsForGradingPeriod(courseId, callback, gradingPeriodId);
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withPerPageQueryParam(true)
                    .withShouldIgnoreToken(false)
                    .withForceReadFromNetwork(forceNetwork)
                    .build();
            AssignmentAPI.getAssignmentGroupsWithAssignmentsForGradingPeriod(courseId, adapter, callback, params, gradingPeriodId);
        }
    }

    public static void getAssignmentGroup(long courseId, long assignmentGroupId, boolean forceNetwork, StatusCallback<AssignmentGroup> callback) {
        if (isTesting() || mTesting) {
            AssignmentManager_Test.getAssignmentGroup(courseId, assignmentGroupId, callback);
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withPerPageQueryParam(true)
                    .withShouldIgnoreToken(false)
                    .withForceReadFromNetwork(forceNetwork)
                    .build();
            AssignmentAPI.getAssignmentGroup(courseId, assignmentGroupId, adapter, callback, params);
        }

    }

    public static void deleteAssignment(long courseId, Assignment assignment, final StatusCallback<Assignment> callback){

        if (isTesting() || mTesting) {
            AssignmentManager_Test.deleteAssignment(assignment, callback);
        } else {

            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withPerPageQueryParam(true)
                    .withShouldIgnoreToken(false)
                    .build();

            AssignmentAPI.deleteAssignment(courseId, assignment.getId(), adapter, callback, params);
        }
    }

    public static void editAssignment(long courseId, long assignmentId, AssignmentPostBody body, final StatusCallback<Assignment> callback){

        if (isTesting() || mTesting) {
            AssignmentManager_Test.editAssignment(body, callback);
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withPerPageQueryParam(false)
                    .withShouldIgnoreToken(false)
                    .build();

            AssignmentPostBodyWrapper bodyWrapper = new AssignmentPostBodyWrapper();
            bodyWrapper.setAssignment(body);
            AssignmentAPI.editAssignment(courseId, assignmentId, bodyWrapper, adapter, callback, params);
        }
    }

    public static void editAssignmentAllowNullValues(long courseId, long assignmentId, AssignmentPostBody body, final StatusCallback<Assignment> callback){

        if (isTesting() || mTesting) {
            AssignmentManager_Test.editAssignment(body, callback);
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withPerPageQueryParam(false)
                    .withShouldIgnoreToken(false)
                    .build();

            AssignmentPostBodyWrapper bodyWrapper = new AssignmentPostBodyWrapper();
            bodyWrapper.setAssignment(body);
            AssignmentAPI.editAssignmentAllowNullValues(courseId, assignmentId, bodyWrapper, adapter, callback, params);
        }
    }

    /*
   *Converts a SUBMISSION_TYPE[] to a queryString for the API
    */
    private static String submissionTypeArrayToAPIQueryString(Map<String, String> map, Assignment.SUBMISSION_TYPE[] submissionTypes){
        if(submissionTypes == null || submissionTypes.length == 0){
            return null;
        }
        String submissionTypesQueryString =  "";

        for(int i =0; i < submissionTypes.length; i++){
            submissionTypesQueryString +=  Assignment.submissionTypeToAPIString(submissionTypes[i]);
            map.put("assignment[submission_types][]", Assignment.submissionTypeToAPIString(submissionTypes[i]));
            if(i < submissionTypes.length -1){
                submissionTypesQueryString += "&assignment[submission_types][]=";
            }
        }

        return submissionTypesQueryString;
    }

    public static void getAllGradeableStudentsForAssignment(long courseId, long assignmentId, final boolean forceNetwork, StatusCallback<List<GradeableStudent>> callback) {
        if (isTesting() || mTesting) {
            AssignmentManager_Test.getAllGradeableStudentsForAssignment(courseId, assignmentId, callback);
        } else {
            final RestBuilder adapter = new RestBuilder(callback);
            StatusCallback<List<GradeableStudent>> depaginatedCallback = new DepaginatedCallback<>(callback, new DepaginatedCallback.PageRequestCallback<GradeableStudent>() {
                @Override
                public void getNextPage(DepaginatedCallback<GradeableStudent> callback, String nextUrl, boolean isCached) {
                    AssignmentAPI.getNextPageGradeableStudents(forceNetwork, nextUrl, adapter, callback);
                }
            });
            adapter.setStatusCallback(depaginatedCallback);
            AssignmentAPI.getFirstPageGradeableStudentsForAssignment(courseId, assignmentId, adapter, depaginatedCallback);
        }
    }

    public static void getAllSubmissionsForAssignment(long courseId, long assignmentId, final boolean forceNetwork, StatusCallback<List<Submission>> callback) {
        if (isTesting() || mTesting) {
            AssignmentManager_Test.getAllSubmissionsForAssignment(courseId, assignmentId, callback);
        } else {
            final RestBuilder adapter = new RestBuilder(callback);
            StatusCallback<List<Submission>> depaginatedCallback = new DepaginatedCallback<>(callback, new DepaginatedCallback.PageRequestCallback<Submission>() {
                @Override
                public void getNextPage(DepaginatedCallback<Submission> callback, String nextUrl, boolean isCached) {
                    AssignmentAPI.getNextPageSubmissions(nextUrl, adapter, forceNetwork, callback);
                }
            });
            adapter.setStatusCallback(depaginatedCallback);
            AssignmentAPI.getFirstPageSubmissionsForAssignment(courseId, assignmentId, forceNetwork, adapter, depaginatedCallback);
        }
    }

    public static void getAllAssignments(long courseId, final boolean forceNetwork, StatusCallback<List<Assignment>> callback) {
        if (isTesting() || mTesting) {
            AssignmentManager_Test.getAllAssignments(courseId, callback);
        } else {
            final RestBuilder adapter = new RestBuilder(callback);
            StatusCallback<List<Assignment>> depaginatedCallback = new DepaginatedCallback<>(callback, new DepaginatedCallback.PageRequestCallback<Assignment>() {
                @Override
                public void getNextPage(DepaginatedCallback<Assignment> callback, String nextUrl, boolean isCached) {
                    AssignmentAPI.getNextPageAssignments(nextUrl, adapter, forceNetwork, callback);
                }
            });
            adapter.setStatusCallback(depaginatedCallback);
            AssignmentAPI.getFirstPageAssignments(courseId, forceNetwork, adapter, depaginatedCallback);
        }
    }

    public static void getAssignmentAirwolf(
            @NonNull String airwolfDomain,
            @NonNull String parentId,
            @NonNull String studentId,
            @NonNull String courseId,
            @NonNull String assignmentId,
            @NonNull StatusCallback<Assignment> callback) {

        RestBuilder adapter = new RestBuilder(callback);
        RestParams params = new RestParams.Builder()
                .withShouldIgnoreToken(false)
                .withPerPageQueryParam(false)
                .withDomain(airwolfDomain)
                .withAPIVersion("")
                .build();

        AssignmentAPI.getAssignmentAirwolf(parentId, studentId, courseId, assignmentId, adapter, callback, params);
    }
}

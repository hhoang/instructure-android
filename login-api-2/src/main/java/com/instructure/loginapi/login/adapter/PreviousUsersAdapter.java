/*
 * Copyright (C) 2017 - present Instructure, Inc.
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
package com.instructure.loginapi.login.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.instructure.loginapi.login.R;
import com.instructure.loginapi.login.model.SignedInUser;
import com.instructure.pandautils.utils.ProfileUtils;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

public class PreviousUsersAdapter extends RecyclerView.Adapter<PreviousUsersAdapter.ViewHolder> {

    public interface PreviousUsersEvents {
        void onPreviousUserClick(SignedInUser user);
        void onRemovePreviousUserClick(SignedInUser user, int position);
        void onNowEmpty();
    }

    private Context mContext;
    private PreviousUsersEvents mCallback;
    private ArrayList<SignedInUser> mPreviousUsers = new ArrayList<>();

    public PreviousUsersAdapter(@NonNull ArrayList<SignedInUser> previousUsers, PreviousUsersEvents callback) {
        mPreviousUsers = previousUsers;
        mCallback = callback;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        mContext = parent.getContext();
        return new PreviousUsersAdapter.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_previous_users, parent, false));
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        SignedInUser user = mPreviousUsers.get(position);
        ProfileUtils.configureAvatarView(mContext, user.user, holder.userAvatar);
        holder.userName.setText(user.user.getName());
        holder.schoolDomain.setText(user.domain);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mCallback != null) {
                    SignedInUser user = mPreviousUsers.get(holder.getAdapterPosition());
                    mCallback.onPreviousUserClick(user);
                }
            }
        });

        holder.removeUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mCallback != null) {
                    final int position = holder.getAdapterPosition();
                    SignedInUser user = mPreviousUsers.get(position);
                    mCallback.onRemovePreviousUserClick(user, position);
                    mPreviousUsers.remove(position);
                    notifyItemRemoved(position);

                    if(mPreviousUsers.size() == 0) {
                        mCallback.onNowEmpty();
                    }
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mPreviousUsers.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        CircleImageView userAvatar;
        TextView userName, schoolDomain;
        View removeUser;

        public ViewHolder(View itemView) {
            super(itemView);
            userAvatar = (CircleImageView) itemView.findViewById(R.id.usersAvatar);
            userName = (TextView) itemView.findViewById(R.id.userName);
            schoolDomain = (TextView) itemView.findViewById(R.id.schoolDomain);
            removeUser = itemView.findViewById(R.id.removePreviousUser);
        }
    }
}

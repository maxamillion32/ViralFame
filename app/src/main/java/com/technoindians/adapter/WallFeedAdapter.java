package com.technoindians.adapter;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.dataappsinfo.viralfame.LoginFragment;
import com.dataappsinfo.viralfame.R;
import com.dataappsinfo.viralfame.ViralFame;
import com.squareup.picasso.Picasso;
import com.technoindians.constants.Actions_;
import com.technoindians.constants.Constants;
import com.technoindians.database.RetrieveOperation;
import com.technoindians.database.TableList;
import com.technoindians.database.UpdateOperations;
import com.technoindians.library.FileCheck;
import com.technoindians.library.TimeConverter;
import com.technoindians.network.JsonArrays_;
import com.technoindians.network.MakeCall;
import com.technoindians.network.Urls;
import com.technoindians.peoples.UserPortfolioActivity;
import com.technoindians.pops.ShowToast;
import com.technoindians.portfolio.FeedDetailsActivity;
import com.technoindians.preferences.Preferences;
import com.technoindians.validation.LoginValidation_;
import com.technoindians.views.CircleTransformMain;
import com.technoindians.views.NetworkImageView;
import com.technoindians.wall.WallCommentDialogFragment;
import com.technoindians.wall.WallOperations_;

import org.json.JSONObject;

import java.util.HashMap;

import hani.momanii.supernova_emoji_library.Helper.EmojiconTextView;
import okhttp3.FormBody;
import okhttp3.RequestBody;

/**
 * Created by Girish on 11-01-2017.
 */

public class WallFeedAdapter extends CursorAdapter {

    private static final String TAG = WallFeedAdapter.class.getSimpleName();
    private Context context;
    private Activity activity;
    private ImageLoader imageLoader = ViralFame.getInstance().getImageLoader();
    private RetrieveOperation retrieveOperation;
    private UpdateOperations updateOperations;

    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Bundle nextAnimation = ActivityOptions.makeCustomAnimation
                    (context, R.anim.animation_one, R.anim.animation_two).toBundle();
            String id = (String) v.getTag();
            Cursor selectedCursor = retrieveOperation.fetchSingleFeed("*", id);
            if (selectedCursor == null) {
                return;
            }
            selectedCursor.moveToFirst();
            switch (v.getId()) {
                case R.id.wall_feed_item_menu:
                    showStatusPopup(selectedCursor, v);
                    break;
                case R.id.wall_feed_item_comment:
                    openCommentDialog(id);
                    break;
                case R.id.wall_feed_item_like:
                    new Operations().execute(id);
                    break;
                case R.id.wall_feed_item_image:
                    Intent imageIntent = new Intent(context, FeedDetailsActivity.class);
                    imageIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    imageIntent.putExtra(Constants.MEDIA_FILE, selectedCursor.getString(selectedCursor.getColumnIndex(Constants.MEDIA_FILE)));
                    imageIntent.putExtra(Constants.WALL_ID, id);
                    imageIntent.putExtra(Constants.MEDIA_TYPE, Constants.TYPE_IMAGE);
                    imageIntent.putExtra(Constants.USER_ID, selectedCursor.getString(selectedCursor.getColumnIndex(Constants.USER_ID)));
                    activity.startActivity(imageIntent, nextAnimation);
                    break;
                case R.id.wall_post_media_audio:
                    int media_type = Integer.parseInt(selectedCursor.getString(selectedCursor.getColumnIndex(Constants.MEDIA_TYPE)));
                    if (media_type == Constants.INTENT_AUDIO) {
                        Intent audioIntent = new Intent(context, FeedDetailsActivity.class);
                        audioIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        audioIntent.putExtra(Constants.MEDIA_FILE, selectedCursor.getString(selectedCursor.getColumnIndex(Constants.MEDIA_FILE)));
                        audioIntent.putExtra(Constants.WALL_ID, id);
                        audioIntent.putExtra(Constants.MEDIA_TYPE, Constants.TYPE_AUDIO);
                        audioIntent.putExtra(Constants.USER_ID, selectedCursor.getString(selectedCursor.getColumnIndex(Constants.USER_ID)));
                        activity.startActivity(audioIntent, nextAnimation);
                    } else if (media_type == Constants.INTENT_VIDEO) {
                        Intent videoIntent = new Intent(context, FeedDetailsActivity.class);
                        videoIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        videoIntent.putExtra(Constants.MEDIA_FILE, selectedCursor.getString(selectedCursor.getColumnIndex(Constants.MEDIA_FILE)));
                        videoIntent.putExtra(Constants.WALL_ID, id);
                        videoIntent.putExtra(Constants.MEDIA_TYPE, Constants.TYPE_VIDEO);
                        videoIntent.putExtra(Constants.USER_ID, selectedCursor.getString(selectedCursor.getColumnIndex(Constants.USER_ID)));
                        activity.startActivity(videoIntent, nextAnimation);
                    }
                    break;
                case R.id.wall_feed_item_photo:

                    Intent profileIntent = new Intent(context, UserPortfolioActivity.class);
                    profileIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    profileIntent.putExtra(Constants.USER_ID, selectedCursor.getString(selectedCursor.getColumnIndex(Constants.USER_ID)));
                    profileIntent.putExtra(Constants.PROFILE_PIC, selectedCursor.getString(selectedCursor.getColumnIndex(Constants.PROFILE_PIC)));
                    profileIntent.putExtra(Constants.NAME, selectedCursor.getString(selectedCursor.getColumnIndex(Constants.NAME)));
                    profileIntent.putExtra(Constants.SKILL, selectedCursor.getString(selectedCursor.getColumnIndex(Constants.SKILL)));
                    profileIntent.putExtra(Constants.IS_FOLLOW, selectedCursor.getString(selectedCursor.getColumnIndex(Constants.IS_FOLLOW)));
                    activity.startActivity(profileIntent, nextAnimation);
                    break;
            }
        }
    };

    public WallFeedAdapter(Activity activity, Cursor cursor) {
        super(activity, cursor);
        this.activity = activity;
        this.context = activity.getApplicationContext();
        this.retrieveOperation = new RetrieveOperation(context);
        updateOperations = new UpdateOperations(context);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.wall_feed_item_layout, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        TextView likeText = (TextView) view.findViewById(R.id.wall_feed_item_like);
        TextView commentText = (TextView) view.findViewById(R.id.wall_feed_item_comment);
        TextView timeText = (TextView) view.findViewById(R.id.wall_feed_item_time);
        TextView nameText = (TextView) view.findViewById(R.id.wall_feed_item_name);
        TextView audioName = (TextView) view.findViewById(R.id.wall_post_media_audio_name);
        TextView audioSize = (TextView) view.findViewById(R.id.wall_post_media_audio_size);
        TextView audioTime = (TextView) view.findViewById(R.id.wall_post_media_audio_duration);
        TextView skillText = (TextView) view.findViewById(R.id.wall_feed_item_role);

        EmojiconTextView postText = (EmojiconTextView) view.findViewById(R.id.wall_feed_item_post_text);
        EmojiconTextView audioText = (EmojiconTextView) view.findViewById(R.id.wall_post_media_post_text);
        EmojiconTextView feedText = (EmojiconTextView) view.findViewById(R.id.wall_feed_item_feed_text);

        NetworkImageView postImage = (NetworkImageView) view.findViewById(R.id.wall_feed_item_image);

        ImageView profilePhoto = (ImageView) view.findViewById(R.id.wall_feed_item_photo);
        ImageView mediaIcon = (ImageView) view.findViewById(R.id.wall_post_media_audio_icon);
        RelativeLayout menuIcon = (RelativeLayout) view.findViewById(R.id.wall_feed_item_menu);

        LinearLayout audioLayout = (LinearLayout) view.findViewById(R.id.wall_feed_item_audio_layout);
        LinearLayout audioIconLayout = (LinearLayout) view.findViewById(R.id.wall_post_media_audio);
        RelativeLayout imageLayout = (RelativeLayout) view.findViewById(R.id.wall_feed_item_image_layout);

        View topView = view.findViewById(R.id.wall_feed_item_top_view);
        View bottomView = view.findViewById(R.id.wall_feed_item_bottom_view);

        if (cursor.isClosed()) {
            // Log.e(TAG, "cursor:closed ");
            return;
        }
        String name = cursor.getString(cursor.getColumnIndex(Constants.NAME));
        String id = cursor.getString(cursor.getColumnIndex(Constants._ID));
        String media_type = cursor.getString(cursor.getColumnIndex(Constants.MEDIA_TYPE));
        String media_file = cursor.getString(cursor.getColumnIndex(Constants.MEDIA_FILE));
        String total_comments = cursor.getString(cursor.getColumnIndex(Constants.TOTAL_COMMENTS));
        String total_likes = cursor.getString(cursor.getColumnIndex(Constants.TOTAL_LIKES));
        String is_like = cursor.getString(cursor.getColumnIndex(Constants.IS_LIKE));
        String last_updated = cursor.getString(cursor.getColumnIndex(Constants.LAST_UPDATED));
        String profile_pic = cursor.getString(cursor.getColumnIndex(Constants.PROFILE_PIC));
        String skills = cursor.getString(cursor.getColumnIndex(Constants.SKILL));

        skillText.setText(skills);
        likeText.setText(total_likes);
        commentText.setText(total_comments);
        nameText.setText(name);

        menuIcon.setTag(id);
        postImage.setTag(id);
        postImage.setOnClickListener(onClickListener);
        menuIcon.setOnClickListener(onClickListener);

        String time = TimeConverter.getWallTime(Long.parseLong(last_updated));
        if (time == null || time.length() <= 0) {
            time = "now";
        } else {
            if (time.contains(",")) {
                time = time.substring(0, time.lastIndexOf(","));
            }
        }

        timeText.setText(time);

        profilePhoto.setTag(id);
        profilePhoto.setOnClickListener(onClickListener);

        commentText.setTag(id);
        commentText.setOnClickListener(onClickListener);

        likeText.setTag(id);
        likeText.setOnClickListener(onClickListener);

        audioIconLayout.setTag(id);
        audioIconLayout.setOnClickListener(onClickListener);

        if (Integer.parseInt(is_like) == 1) {
            likeText.setTextColor(context.getResources().getColor(R.color.colorPrimary));
            likeText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_like_p, 0, 0, 0);
        } else {
            likeText.setTextColor(context.getResources().getColor(R.color.black_65));
            likeText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_like_g, 0, 0, 0);
        }

        if (Integer.parseInt(total_comments) > 0) {
            commentText.setTextColor(context.getResources().getColor(R.color.colorPrimary));
            commentText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_comment_p, 0, 0, 0);
        } else {
            commentText.setTextColor(context.getResources()
                    .getColor(R.color.black_65));
            commentText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_comment_g, 0, 0, 0);
        }

        Picasso.with(context)
                .load(Urls.DOMAIN + profile_pic)
                .resize(100, 100)
                .onlyScaleDown()
                .transform(new CircleTransformMain())
                .placeholder(R.drawable.ic_avatar)
                .error(R.drawable.ic_avatar)
                .into(profilePhoto);

        switch (Integer.parseInt(media_type)) {
            case 0:
                feedText.setVisibility(View.VISIBLE);
                audioLayout.setVisibility(View.GONE);
                imageLayout.setVisibility(View.GONE);
                setMessage(feedText, topView, bottomView, cursor);
                break;
            case 1:
                feedText.setVisibility(View.GONE);
                audioLayout.setVisibility(View.GONE);
                imageLayout.setVisibility(View.VISIBLE);
                setImage(postText, topView, bottomView, cursor);

                postImage.setDefaultImageResId(R.drawable.ic_image_place_holder);
                postImage.setImageUrl(Urls.DOMAIN + media_file, imageLoader);
                break;
            case 2:
                feedText.setVisibility(View.GONE);
                audioLayout.setVisibility(View.VISIBLE);
                imageLayout.setVisibility(View.GONE);
                setVideo(audioText, mediaIcon, audioSize, audioName, audioTime, topView, bottomView, cursor);
                break;
            case 3:
                feedText.setVisibility(View.GONE);
                audioLayout.setVisibility(View.VISIBLE);
                imageLayout.setVisibility(View.GONE);
                setAudio(audioText, mediaIcon, audioSize, audioName, audioTime, topView, bottomView, cursor);
                break;
        }
    }

    private void showStatusPopup(Cursor selectedCursor, View view) {
        final PopupMenu popup = new PopupMenu(activity, view);
        if (selectedCursor == null) {
            getCursor().requery();
            notifyDataSetChanged();
            return;
        }

        String user_id = selectedCursor.getString(selectedCursor.getColumnIndex(Constants.USER_ID));
        Log.d(TAG, "friend_id: " + user_id + "\nuser_id: " + Preferences.get(Constants.USER_ID));
        final String _id = selectedCursor.getString(selectedCursor.getColumnIndex(Constants._ID));
        if (user_id.equalsIgnoreCase(Preferences.get(Constants.USER_ID))) {
            popup.getMenuInflater().inflate(R.menu.feed_menu_admin, popup.getMenu());
        } else {
            popup.getMenuInflater().inflate(R.menu.feed_menu_user, popup.getMenu());
        }

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.feed_menu_delete:
                        deleteConfirmation(1, _id);
                        break;
                    case R.id.feed_menu_spam:
                        deleteConfirmation(3, _id);
                        break;
                    case R.id.feed_menu_remove:
                        deleteConfirmation(2, _id);
                        break;
                }
                popup.dismiss();
                return true;
            }
        });
        popup.show();
    }

    private void deleteConfirmation(final int type, final String _id) {
        //1-delete;2-remove;3-spam
        final UpdateOperations updateOperations = new UpdateOperations(activity.getApplicationContext());
        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.confirmation_dialog);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        TextView title, warning;
        Button fwdAccept, fwdDecline;
        title = (TextView) dialog.findViewById(R.id.confirmation_dialog_title);
        if (type == 1) {
            title.setText("Delete Timeline Post!");
        }
        if (type == 2) {
            title.setText("Remove Timeline Post!");
        }
        if (type == 3) {
            title.setText("Spam Timeline Post!");
        }
        warning = (TextView) dialog.findViewById(R.id.confirmation_dialog_description);
        if (type == 1) {
            warning.setText("Are you sure to delete timeline post?");
        }
        if (type == 2) {
            warning.setText("Are you sure to remove timeline post?");
        }
        if (type == 3) {
            warning.setText("Are you sure to spam timeline post?");
        }

        fwdAccept = (Button) dialog.findViewById(R.id.confirmation_dialog_ok_button);
        final EditText numberBox = (EditText) dialog.findViewById(R.id.confirmation_dialog_number_box);
        numberBox.setVisibility(View.GONE);

        fwdAccept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (type == 1) {
                    int result = WallOperations_.delete(_id);
                    if (result == 1) {
                        updateOperations.deleteRecord(TableList.TABLE_WALL_FEED, Constants._ID, _id);
                        getCursor().requery();
                        notifyDataSetChanged();
                    }
                }
                if (type == 2) {
                    WallOperations_.remove(_id);
                    updateOperations.deleteRecord(TableList.TABLE_WALL_FEED, Constants._ID, _id);
                    getCursor().requery();
                    notifyDataSetChanged();
                }
                if (type == 3) {
                    int spam_result = WallOperations_.spam(_id);
                    if (spam_result == 1) {
                        updateOperations.deleteRecord(TableList.TABLE_WALL_FEED, Constants._ID, _id);
                        getCursor().requery();
                        notifyDataSetChanged();
                    }
                }
                dialog.dismiss();

            }
        });

        fwdDecline = (Button) dialog.findViewById(R.id.confirmation_dialog_cancel_button);
        fwdDecline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.setCancelable(true);
        dialog.show();
    }


    private void setMessage(EmojiconTextView feedText, View topView, View bottomView, Cursor cursor) {
        feedText.setText(cursor.getString(cursor.getColumnIndex(Constants.POST_TEXT)));
        topView.setVisibility(View.GONE);
        bottomView.setVisibility(View.VISIBLE);
    }

    private void setImage(EmojiconTextView postText, View topView, View bottomView, Cursor cursor) {
        postText.setText(cursor.getString(cursor.getColumnIndex(Constants.POST_TEXT)));
        topView.setVisibility(View.GONE);
        bottomView.setVisibility(View.GONE);
    }

    private void setAudio(EmojiconTextView audioText, ImageView mediaIcon, TextView audioSize,
                          TextView audioName, TextView audioTime, View topView, View bottomView, Cursor cursor) {
        topView.setVisibility(View.GONE);
        bottomView.setVisibility(View.GONE);

        mediaIcon.setImageResource(R.drawable.ic_audio_y);
        audioText.setText(cursor.getString(cursor.getColumnIndex(Constants.POST_TEXT)));
        audioSize.setText(FileCheck.getFileSize(Long.parseLong(cursor.getString(cursor.getColumnIndex(Constants.MEDIA_SIZE)))));
        audioName.setText(FileCheck.getFileName(cursor.getString(cursor.getColumnIndex(Constants.MEDIA_FILE))));
        String time = retrieveOperation.getFeedItem(Constants.MEDIA_DURATION, cursor.getString(cursor.getColumnIndex(Constants._ID)));
        if (time.equalsIgnoreCase("0") || time.length() <= 0) {
            time = "";
        }
        audioTime.setText(time);
    }

    private void setVideo(EmojiconTextView audioText, ImageView mediaIcon, TextView audioSize,
                          TextView audioName, TextView audioTime, View topView, View bottomView, Cursor cursor) {
        topView.setVisibility(View.GONE);
        bottomView.setVisibility(View.GONE);

        mediaIcon.setImageResource(R.drawable.ic_video_play);
        audioText.setText(cursor.getString(cursor.getColumnIndex(Constants.POST_TEXT)));
        audioSize.setText(FileCheck.getFileSize(Long.parseLong(cursor.getString(cursor.getColumnIndex(Constants.MEDIA_SIZE)))));
        audioName.setText(FileCheck.getFileName(cursor.getString(cursor.getColumnIndex(Constants.MEDIA_FILE))));
        String time = retrieveOperation.getFeedItem(Constants.MEDIA_DURATION, cursor.getString(cursor.getColumnIndex(Constants._ID)));
        if (time.equalsIgnoreCase("0")) {
            time = "";
        }
        audioTime.setText(time);
    }

    private boolean removeCommentDialog() {
        FragmentManager fragmentManager = activity.getFragmentManager();
        Fragment fragment = fragmentManager.findFragmentByTag(Constants.COMMENT);
        if (fragment != null) {
            fragmentManager.beginTransaction().remove(fragment).commit();
            return true;
        }
        return false;
    }

    private void openCommentDialog(String _id) {
        removeCommentDialog();
        FragmentManager fragmentManager = activity.getFragmentManager();
        WallCommentDialogFragment detailsFragment = new WallCommentDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putString(Constants.ID, _id);
        detailsFragment.setArguments(bundle);
        detailsFragment.show(fragmentManager, Constants.COMMENT);
    }

    private void toggleLike(String _id) {
        Cursor likeCursor = retrieveOperation.fetchSingleFeed("*", _id);
        if (likeCursor == null) {
            return;
        }
        likeCursor.moveToFirst();
        String is_like = likeCursor.getString(likeCursor.getColumnIndex(Constants.IS_LIKE));
        int total_like = Integer.parseInt(likeCursor.getString(likeCursor.getColumnIndex(Constants.TOTAL_LIKES)));
        if (Integer.parseInt(is_like) == 1) {
            is_like = "0";
            if (total_like > 0) {
                total_like = total_like - 1;
            }
        } else {
            is_like = "1";
            total_like = total_like + 1;
        }
        updateOperations.updateFeed(_id, Constants.IS_LIKE, is_like);
        updateOperations.updateFeed(_id, Constants.TOTAL_LIKES, "" + total_like);
        getCursor().requery();
        notifyDataSetChanged();
    }

    private class Operations extends AsyncTask<String, Void, Integer> {
        int result = 11;
        String _id;

        @Override
        protected Integer doInBackground(String... params) {
            _id = params[0];
            RequestBody requestBody = new FormBody.Builder()
                    .add(Constants.USER_ID, Preferences.get(Constants.USER_ID))
                    .add(Constants.ID, _id)
                    .add(Constants.ACTION, Actions_.LIKE_UNLIKE)
                    .build();
            try {
                String response = MakeCall.post(Urls.DOMAIN + Urls.POST_OPERATIONS_URL, requestBody, TAG);
                if (response != null) {
                    JSONObject jsonObject = new JSONObject(response);
                    if (jsonObject.has(JsonArrays_.COMMENT)) {
                        JSONObject responseObject = jsonObject.getJSONObject(JsonArrays_.COMMENT);
                        result = responseObject.getInt(Constants.STATUS);
                    } else {
                        result = 12;
                    }
                } else {
                    result = 12;
                }
            } catch (Exception e) {
                e.printStackTrace();
                result = 11;
            }
            return result;
        }

        @Override
        protected void onPostExecute(Integer integer) {
            super.onPostExecute(integer);
            switch (integer) {
                case 0:
                    ShowToast.actionFailed(context);
                    break;
                case 1:
                    toggleLike(_id);
                    break;
                case 2:
                    ShowToast.actionFailed(context);
                    break;
                case 11:
                    ShowToast.networkProblemToast(context);
                    break;
                case 12:
                    ShowToast.internalErrorToast(context);
                    break;
            }
        }
    }
}

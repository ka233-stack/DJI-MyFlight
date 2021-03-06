package com.dji.myFlight;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SlidingDrawer;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJICameraError;
import dji.common.error.DJIError;
import dji.common.product.Model;
import dji.common.util.CommonCallbacks;
import dji.log.DJILog;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.media.DownloadListener;
import dji.sdk.media.FetchMediaTask;
import dji.sdk.media.FetchMediaTaskContent;
import dji.sdk.media.FetchMediaTaskScheduler;
import dji.sdk.media.MediaFile;
import dji.sdk.media.MediaManager;


public class GalleryActivity extends Activity implements View.OnClickListener {

    private static final String TAG = GalleryActivity.class.getName();

    private Button mBackBtn, mDeleteBtn, mReloadBtn, mDownloadBtn, mStatusBtn;
    private Button mPlayBtn, mResumeBtn, mPauseBtn, mStopBtn;
    private RecyclerView listView;
    private FileListAdapter mListAdapter;
    private List<MediaFile> mediaFileList = new ArrayList<MediaFile>();
    private MediaManager mediaManager;
    private MediaManager.FileListState currentFileListState = MediaManager.FileListState.UNKNOWN;
    private FetchMediaTaskScheduler scheduler;
    private ProgressDialog mLoadingDialog;
    private ProgressDialog downloadDialog;
    private SlidingDrawer mPushDrawerSd;
    File destDir = null;
    String dirPath;
    private int currentProgress = -1;
    private ImageView mDisplayImageView;
    private int lastClickViewIndex = -1;
    private View lastClickView;
    private TextView mPushTv;
    private Camera camera;
    private SettingsDefinitions.StorageLocation mediaStorageLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);
        initUI();
        initData();
    }

    private void initData() {
        camera = DemoApplication.getCameraInstance();
        mediaManager = camera.getMediaManager();
        dirPath = Environment.getExternalStorageDirectory().getPath() + "/MyFlight/";
    }

    @Override
    protected void onResume() {
        super.onResume();
        initMediaManager();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        lastClickView = null;
        if (mediaManager != null) {
            mediaManager.stop(null);
            mediaManager.removeFileListStateCallback(this.updateFileListStateListener);
            mediaManager.removeMediaUpdatedVideoPlaybackStateListener(updatedVideoPlaybackStateListener);
            mediaManager.exitMediaDownloading();
            if (scheduler != null) {
                scheduler.removeAllTasks();
            }
        }
        if (isMavicAir2() || isM300()) {
            if (camera != null) {
                camera.exitPlayback(djiError -> {
                    if (djiError != null) {
                        camera.setFlatMode(SettingsDefinitions.FlatCameraMode.PHOTO_SINGLE, djiError1 -> {
                            if (djiError1 != null) {
                                setResultToToast("Set PHOTO_SINGLE Mode Failed. " + djiError1.getDescription());
                            }
                        });
                    }
                });
            } else {
                camera.setMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO, djiError -> {
                    if (djiError != null) {
                        setResultToToast("Set SHOOT_PHOTO Mode Failed. " + djiError.getDescription());
                    }
                });
            }
        }
        if (mediaFileList != null) {
            mediaFileList.clear();
        }
        super.onDestroy();
    }

    void initUI() {
        //Init RecyclerView
        listView = (RecyclerView) findViewById(R.id.filelistView);
        LinearLayoutManager layoutManager = new LinearLayoutManager(GalleryActivity.this, RecyclerView.VERTICAL, false);
        listView.setLayoutManager(layoutManager);

        //Init FileListAdapter
        mListAdapter = new FileListAdapter();
        listView.setAdapter(mListAdapter);

        //Init Loading Dialog
        mLoadingDialog = new ProgressDialog(GalleryActivity.this);
        mLoadingDialog.setMessage("?????????");
        mLoadingDialog.setCanceledOnTouchOutside(false);
        mLoadingDialog.setCancelable(false);

        // ???????????????????????????
        downloadDialog = new ProgressDialog(GalleryActivity.this);
        downloadDialog.setTitle("????????????");
        downloadDialog.setIcon(android.R.drawable.ic_dialog_info);
        downloadDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        downloadDialog.setCanceledOnTouchOutside(false);
        downloadDialog.setCancelable(true);
        downloadDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if (mediaManager != null) {
                    // ??????????????????(?????????????????????)
                    // ??????????????????MEDIA_DOWNLOAD??????, ??????SHOOT_PHOTO??????
                    mediaManager.exitMediaDownloading();
                }
            }
        });

        mPushDrawerSd = (SlidingDrawer) findViewById(R.id.pointing_drawer_sd);
        mPushTv = (TextView) findViewById(R.id.pointing_push_tv);
        mBackBtn = (Button) findViewById(R.id.back_btn);
        mDeleteBtn = (Button) findViewById(R.id.delete_btn);
        mDownloadBtn = (Button) findViewById(R.id.download_btn);
        mReloadBtn = (Button) findViewById(R.id.reload_btn);
        mStatusBtn = (Button) findViewById(R.id.status_btn);
        mPlayBtn = (Button) findViewById(R.id.play_btn);
        mResumeBtn = (Button) findViewById(R.id.resume_btn);
        mPauseBtn = (Button) findViewById(R.id.pause_btn);
        mStopBtn = (Button) findViewById(R.id.stop_btn);
        mDisplayImageView = (ImageView) findViewById(R.id.imageView);
        mDisplayImageView.setVisibility(View.VISIBLE);

        mBackBtn.setOnClickListener(this);
        mDeleteBtn.setOnClickListener(this);
        mDownloadBtn.setOnClickListener(this);
        mReloadBtn.setOnClickListener(this);
        mStatusBtn.setOnClickListener(this);
        mPlayBtn.setOnClickListener(this);
        mResumeBtn.setOnClickListener(this);
        mPauseBtn.setOnClickListener(this);
        mStopBtn.setOnClickListener(this);
    }

    /**
     * ??????mLoadingDialog?????????
     */
    private void showProgressDialog() {
        runOnUiThread(() -> {
            if (mLoadingDialog != null) {
                mLoadingDialog.show();
            }
        });
    }

    /**
     * ??????mLoadingDialog?????????
     */
    private void hideProgressDialog() {
        runOnUiThread(() -> {
            if (null != mLoadingDialog && mLoadingDialog.isShowing()) {
                mLoadingDialog.dismiss();
            }
        });
    }

    /**
     * ???????????????????????????
     */
    private void ShowDownloadProgressDialog() {
        if (downloadDialog != null) {
            runOnUiThread(() -> {
                downloadDialog.incrementProgressBy(-downloadDialog.getProgress());
                downloadDialog.show();
            });
        }
    }

    /**
     * ???????????????????????????
     */
    private void HideDownloadProgressDialog() {
        if (null != downloadDialog && downloadDialog.isShowing()) {
            runOnUiThread(() -> downloadDialog.dismiss());
        }
    }

    private void setResultToToast(final String result) {
        runOnUiThread(() -> Toast.makeText(GalleryActivity.this, result, Toast.LENGTH_SHORT).show());
    }

    private void setResultToText(final String string) {
        if (mPushTv == null) {
            setResultToToast("Push info tv has not be init...");
        }
        GalleryActivity.this.runOnUiThread(() -> mPushTv.setText(string));
    }

    private void initMediaManager() {
        if (camera == null) {
            mediaFileList.clear();
            mListAdapter.notifyDataSetChanged();
            DJILog.e(TAG, "?????????????????????");
        } else if (!camera.isMediaDownloadModeSupported()) {
            setResultToToast("???????????????????????????");
        } else if (mediaManager != null) {
            // ??????????????????
            camera.getStorageLocation(new CommonCallbacks.CompletionCallbackWith<SettingsDefinitions.StorageLocation>() {
                @Override
                public void onSuccess(SettingsDefinitions.StorageLocation storageLocation) {
                    DJILog.e(TAG, "????????????????????????");
                    mediaStorageLocation = storageLocation;
                }

                @Override
                public void onFailure(DJIError djiError) {
                    setResultToToast("????????????????????????");
                }
            });

            mediaManager.addUpdateFileListStateListener(this.updateFileListStateListener);
            mediaManager.addMediaUpdatedVideoPlaybackStateListener(this.updatedVideoPlaybackStateListener);

            if (isMavicAir2() || isM300()) {
                camera.enterPlayback(djiError -> {
                    if (djiError == null) {
                        DJILog.e(TAG, "????????????????????????");
                        showProgressDialog();
                        getFileList();
                    } else {
                        setResultToToast("????????????????????????");
                    }
                });
            } else {
                camera.setMode(SettingsDefinitions.CameraMode.MEDIA_DOWNLOAD, error -> {
                    if (error == null) {
                        DJILog.e(TAG, "????????????????????????");
                        showProgressDialog();
                        getFileList();
                    } else {
                        setResultToToast("????????????????????????");
                    }
                });
            }

            // ????????????????????????
            if (mediaManager.isVideoPlaybackSupported()) {
                DJILog.e(TAG, "????????????????????????!");
            } else {
                setResultToToast("???????????????????????????!");
            }
            scheduler = mediaManager.getScheduler();
        }
    }

    /**
     * ??????????????????
     */
    private void getFileList() {
        if (mediaManager != null) {
            if ((currentFileListState == MediaManager.FileListState.SYNCING) || (currentFileListState == MediaManager.FileListState.DELETING)) {
                DJILog.e(TAG, "?????????????????????");
            } else {
                // ???????????????????????????(??????????????????)
                mediaManager.refreshFileListOfStorageLocation(mediaStorageLocation, djiError -> {
                    if (djiError == null) {
                        hideProgressDialog();
                        // Reset data
                        if (currentFileListState != MediaManager.FileListState.INCOMPLETE) {
                            mediaFileList.clear();
                            lastClickViewIndex = -1;
                            lastClickView = null;
                        }
                        // ????????????
                        if (mediaStorageLocation == SettingsDefinitions.StorageLocation.SDCARD) {
                            mediaFileList = mediaManager.getSDCardFileListSnapshot();
                        } else if (mediaStorageLocation == SettingsDefinitions.StorageLocation.INTERNAL_STORAGE) {
                            mediaFileList = mediaManager.getInternalStorageFileListSnapshot();
                        }
                        if (mediaFileList != null) {
                            // ????????????
                            Collections.sort(mediaFileList, (lhs, rhs) -> {
                                if (lhs.getTimeCreated() < rhs.getTimeCreated()) {
                                    return 1;
                                } else if (lhs.getTimeCreated() > rhs.getTimeCreated()) {
                                    return -1;
                                }
                                return 0;
                            });
                            scheduler.resume(error -> {
                                if (error == null) {
                                    getThumbnails();
                                }
                            });
                        }
                    } else {
                        hideProgressDialog();
                        setResultToToast("??????????????????????????????: " + djiError.getDescription());
                    }
                });
            }
        }
    }

    private void getThumbnails() {
        if (mediaFileList.size() <= 0) {
            setResultToToast("No File info for downloading thumbnails");
            return;
        }
        for (int i = 0; i < mediaFileList.size(); i++) {
            getThumbnailByIndex(i);
        }
    }

    private FetchMediaTask.Callback taskCallback = new FetchMediaTask.Callback() {
        @Override
        public void onUpdate(MediaFile file, FetchMediaTaskContent option, DJIError error) {
            if (null == error) {
                if (option == FetchMediaTaskContent.PREVIEW) {
                    runOnUiThread(() -> mListAdapter.notifyDataSetChanged());
                }
                if (option == FetchMediaTaskContent.THUMBNAIL) {
                    runOnUiThread(() -> mListAdapter.notifyDataSetChanged());
                }
            } else {
                DJILog.e(TAG, "????????????????????????: " + error.getDescription());
            }
        }
    };

    private void getThumbnailByIndex(final int index) {
        FetchMediaTask task = new FetchMediaTask(mediaFileList.get(index), FetchMediaTaskContent.THUMBNAIL, taskCallback);
        scheduler.moveTaskToEnd(task);
    }

    private static class ItemHolder extends RecyclerView.ViewHolder {
        ImageView thumbnail_img;
        TextView file_name;
        TextView file_type;
        TextView file_size;
        TextView file_time;

        public ItemHolder(View itemView) {
            super(itemView);
            this.thumbnail_img = (ImageView) itemView.findViewById(R.id.filethumbnail);
            this.file_name = (TextView) itemView.findViewById(R.id.filename);
            this.file_type = (TextView) itemView.findViewById(R.id.filetype);
            this.file_size = (TextView) itemView.findViewById(R.id.fileSize);
            this.file_time = (TextView) itemView.findViewById(R.id.filetime);
        }
    }

    private class FileListAdapter extends RecyclerView.Adapter<ItemHolder> {
        @Override
        public int getItemCount() {
            if (mediaFileList != null) {
                return mediaFileList.size();
            }
            return 0;
        }

        @Override
        public ItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.media_info_item, parent, false);
            return new ItemHolder(view);
        }

        @Override
        public void onBindViewHolder(ItemHolder mItemHolder, final int index) {

            final MediaFile mediaFile = mediaFileList.get(index);
            if (mediaFile != null) {
                if (mediaFile.getMediaType() != MediaFile.MediaType.MOV && mediaFile.getMediaType() != MediaFile.MediaType.MP4) {
                    mItemHolder.file_time.setVisibility(View.GONE);
                } else {
                    mItemHolder.file_time.setVisibility(View.VISIBLE);
                    mItemHolder.file_time.setText(mediaFile.getDurationInSeconds() + " s");
                }
                mItemHolder.file_name.setText(mediaFile.getFileName());
                mItemHolder.file_type.setText(mediaFile.getMediaType().name());
                mItemHolder.file_size.setText(mediaFile.getFileSize() + " Bytes");
                mItemHolder.thumbnail_img.setImageBitmap(mediaFile.getThumbnail());
                mItemHolder.thumbnail_img.setOnClickListener(ImgOnClickListener);
                mItemHolder.thumbnail_img.setTag(mediaFile);
                mItemHolder.itemView.setTag(index);

                if (lastClickViewIndex == index) {
                    mItemHolder.itemView.setSelected(true);
                } else {
                    mItemHolder.itemView.setSelected(false);
                }
                mItemHolder.itemView.setOnClickListener(itemViewOnClickListener);

            }
        }

    }

    private View.OnClickListener itemViewOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            lastClickViewIndex = (int) (v.getTag());

            if (lastClickView != null && lastClickView != v) {
                lastClickView.setSelected(false);
            }
            v.setSelected(true);
            lastClickView = v;
        }
    };

    private View.OnClickListener ImgOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            MediaFile selectedMedia = (MediaFile) v.getTag();
            if (selectedMedia != null && mediaManager != null) {
                addMediaTask(selectedMedia);
            }
        }
    };

    private void addMediaTask(final MediaFile mediaFile) {
        final FetchMediaTaskScheduler scheduler = mediaManager.getScheduler();
        final FetchMediaTask task =
                new FetchMediaTask(mediaFile, FetchMediaTaskContent.PREVIEW, new FetchMediaTask.Callback() {
                    @Override
                    public void onUpdate(final MediaFile mediaFile, FetchMediaTaskContent fetchMediaTaskContent, DJIError error) {
                        if (null == error) {
                            if (mediaFile.getPreview() != null) {
                                runOnUiThread(() -> {
                                    final Bitmap previewBitmap = mediaFile.getPreview();
                                    mDisplayImageView.setVisibility(View.VISIBLE);
                                    mDisplayImageView.setImageBitmap(previewBitmap);
                                });
                            } else {
                                setResultToToast("null bitmap!");
                            }
                        } else {
                            setResultToToast("fetch preview image failed: " + error.getDescription());
                        }
                    }
                });

        scheduler.resume(error -> {
            if (error == null) {
                scheduler.moveTaskToNext(task);
            } else {
                setResultToToast("resume scheduler failed: " + error.getDescription());
            }
        });
    }

    //Listeners
    private MediaManager.FileListStateListener updateFileListStateListener = state -> currentFileListState = state;

    private MediaManager.VideoPlaybackStateListener updatedVideoPlaybackStateListener =
            new MediaManager.VideoPlaybackStateListener() {
                @Override
                public void onUpdate(MediaManager.VideoPlaybackState videoPlaybackState) {
                    updateStatusTextView(videoPlaybackState);
                }
            };

    private void updateStatusTextView(MediaManager.VideoPlaybackState videoPlaybackState) {
        final StringBuffer pushInfo = new StringBuffer();

        addLineToSB(pushInfo, "??????????????????", null);
        if (videoPlaybackState != null) {
            if (videoPlaybackState.getPlayingMediaFile() != null) {
                addLineToSB(pushInfo, "media index", videoPlaybackState.getPlayingMediaFile().getIndex());
                addLineToSB(pushInfo, "media size", videoPlaybackState.getPlayingMediaFile().getFileSize());
                addLineToSB(pushInfo,
                        "media duration",
                        videoPlaybackState.getPlayingMediaFile().getDurationInSeconds());
                addLineToSB(pushInfo, "????????????", videoPlaybackState.getPlayingMediaFile().getDateCreated());
                addLineToSB(pushInfo,
                        "media orientation",
                        videoPlaybackState.getPlayingMediaFile().getVideoOrientation());
            } else {
                addLineToSB(pushInfo, "media index", "None");
            }
            addLineToSB(pushInfo, "????????????", videoPlaybackState.getPlayingPosition());
            addLineToSB(pushInfo, "????????????", videoPlaybackState.getPlaybackStatus());
            addLineToSB(pushInfo, "????????????", videoPlaybackState.getCachedPercentage());
            addLineToSB(pushInfo, "????????????", videoPlaybackState.getCachedPosition());
            pushInfo.append("\n");
            setResultToText(pushInfo.toString());
        }
    }

    /**
     * @param sb
     * @param name
     * @param value
     */
    private void addLineToSB(StringBuffer sb, String name, Object value) {
        if (sb == null) return;
        sb.append((name == null || name.equals("")) ? "" : name + ": ")
                .append(value == null ? "\n" : value + "\n");
    }

    private void downloadFileByIndex(final int index) {
        MediaFile mediaFile = mediaFileList.get(index);
        MediaFile.MediaType mediaType = mediaFile.getMediaType();
        if (mediaType == MediaFile.MediaType.PANORAMA || mediaType == MediaFile.MediaType.SHALLOW_FOCUS) {
            return;
        }
        // ???????????????
        if (destDir == null) {
            // destDir = new File(dirPath + "/" + mediaType);
            destDir = new File(dirPath + "/camera");
            runOnUiThread(() -> Toast.makeText(getApplicationContext(), "?????????" + destDir.getPath(), Toast.LENGTH_LONG).show());
            if (!destDir.exists()) {
                runOnUiThread(() -> Toast.makeText(getApplicationContext(), "?????????????????????", Toast.LENGTH_LONG).show());
                destDir.mkdirs();
            } else {
                runOnUiThread(() -> Toast.makeText(getApplicationContext(), "??????????????????", Toast.LENGTH_LONG).show());
            }
        }


        mediaFile.fetchFileData(destDir, null, new DownloadListener<String>() {
            @Override
            public void onFailure(DJIError error) {
                HideDownloadProgressDialog();
                setResultToToast("??????????????????: " + error.getDescription());
                currentProgress = -1;
            }

            @Override
            public void onProgress(long total, long current) {
            }

            @Override
            public void onRateUpdate(long total, long current, long perSize) {
                int tmpProgress = (int) (1.0 * current / total * 100);
                if (tmpProgress != currentProgress) {
                    downloadDialog.setProgress(tmpProgress);
                    currentProgress = tmpProgress;
                }
            }

            @Override
            public void onRealtimeDataUpdate(byte[] bytes, long l, boolean b) {

            }

            @Override
            public void onStart() {
                currentProgress = -1;
                ShowDownloadProgressDialog();
            }

            @Override
            public void onSuccess(String filePath) {
                HideDownloadProgressDialog();
                setResultToToast("??????????????????: " + filePath);
                currentProgress = -1;
            }
        });
    }

    private void deleteFileByIndex(final int index) {
        ArrayList<MediaFile> fileToDelete = new ArrayList<>();
        if (mediaFileList.size() > index) {
            fileToDelete.add(mediaFileList.get(index));
            mediaManager.deleteFiles(fileToDelete, new CommonCallbacks.CompletionCallbackWithTwoParam<List<MediaFile>, DJICameraError>() {
                @Override
                public void onSuccess(List<MediaFile> x, DJICameraError y) {
                    DJILog.e(TAG, "??????????????????");
                    runOnUiThread(() -> {
                        mediaFileList.remove(index);

                        // Reset select view
                        lastClickViewIndex = -1;
                        lastClickView = null;

                        // Update recyclerView
                        mListAdapter.notifyItemRemoved(index);
                    });
                }

                @Override
                public void onFailure(DJIError error) {
                    setResultToToast("??????????????????");
                }
            });
        }
    }

    private void playVideo() {
        mDisplayImageView.setVisibility(View.INVISIBLE);
        MediaFile selectedMediaFile = mediaFileList.get(lastClickViewIndex);
        if ((selectedMediaFile.getMediaType() == MediaFile.MediaType.MOV) || (selectedMediaFile.getMediaType() == MediaFile.MediaType.MP4)) {
            mediaManager.playVideoMediaFile(selectedMediaFile, error -> {
                if (null != error) {
                    setResultToToast("??????????????????: " + error.getDescription());
                } else {
                    DJILog.e(TAG, "??????????????????");
                }
            });
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.back_btn) {
            this.finish();
        } else if (id == R.id.delete_btn) {
            deleteFileByIndex(lastClickViewIndex);
        } else if (id == R.id.reload_btn) {
            getFileList();
        } else if (id == R.id.download_btn) {
            downloadFileByIndex(lastClickViewIndex);
        } else if (id == R.id.status_btn) {
            if (mPushDrawerSd.isOpened()) {
                mPushDrawerSd.animateClose();
            } else {
                mPushDrawerSd.animateOpen();
            }
        } else if (id == R.id.play_btn) {
            playVideo();
        } else if (id == R.id.resume_btn) {
            mediaManager.resume(error -> {
                if (null != error) {
                    setResultToToast("Resume Video Failed" + error.getDescription());
                } else {
                    DJILog.e(TAG, "Resume Video Success");
                }
            });
        } else if (id == R.id.pause_btn) {
            mediaManager.pause(error -> {
                if (null != error) {
                    setResultToToast("??????????????????: " + error.getDescription());
                } else {
                    DJILog.e(TAG, "??????????????????");
                }
            });
        } else if (id == R.id.stop_btn) {
            mediaManager.stop(error -> {
                if (null != error) {
                    setResultToToast("??????????????????" + error.getDescription());
                } else {
                    DJILog.e(TAG, "??????????????????");
                }
            });
        }
    }

    private boolean isMavicAir2() {
        BaseProduct baseProduct = DemoApplication.getProductInstance();
        if (baseProduct != null) {
            return baseProduct.getModel() == Model.MAVIC_AIR_2;
        }
        return false;
    }

    private boolean isM300() {
        BaseProduct baseProduct = DemoApplication.getProductInstance();
        if (baseProduct != null) {
            return baseProduct.getModel() == Model.MATRICE_300_RTK;
        }
        return false;
    }
}

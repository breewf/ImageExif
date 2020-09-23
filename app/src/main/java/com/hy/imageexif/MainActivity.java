package com.hy.imageexif;

import android.Manifest;
import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.blankj.utilcode.util.ConvertUtils;
import com.bumptech.glide.Glide;
import com.hy.imageexif.listener.SimpleAnimatorListener;
import com.hy.imageexif.utils.UriUtils;

import net.sourceforge.jheader.App1Header;
import net.sourceforge.jheader.JpegHeaders;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.exifinterface.media.ExifInterface;
import kr.co.namee.permissiongen.PermissionFail;
import kr.co.namee.permissiongen.PermissionGen;
import kr.co.namee.permissiongen.PermissionSuccess;


/**
 * @author hy
 * @date 2020/9/22
 * ClassDesc:MainActivity.
 **/
public class MainActivity extends AppCompatActivity {

    public static final int REQUEST_ALBUM = 1001;

    private static final String SUFFIX_TEMP_FILE = ".rewrite-exif";

    private ImageView mImageView;
    private TextView mInfoTv;
    private TextView mClearGpsTv;
    private TextView mClearTimeTv;
    private TextView mClearDeviceTv;
    private LinearLayout mBtnLayout;

    private String mImagePath;

    private boolean mIsBtnShow;

    private int mBtnHideX = 52;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.tv_select_pic).setOnClickListener(view -> selectPic());
        mImageView = findViewById(R.id.iv_pic);
        mClearGpsTv = findViewById(R.id.tv_clear_gps);
        mClearTimeTv = findViewById(R.id.tv_clear_time);
        mClearDeviceTv = findViewById(R.id.tv_clear_device);
        mInfoTv = findViewById(R.id.tv_info);
        mBtnLayout = findViewById(R.id.btn_layout);

        mImageView.setOnClickListener(view -> selectPic());

        mClearGpsTv.setOnClickListener(view -> {
            // 清除照片gps信息
            clearImageGpsInfo();
        });

        mClearTimeTv.setOnClickListener(view -> {
            // 清除照片时间信息
            clearImageTimeInfo();
        });

        mClearDeviceTv.setOnClickListener(view -> {
            // 清除照片设备信息
            clearImageDeviceInfo();
        });

        mInfoTv.setOnClickListener(view -> hideBtn());

        mInfoTv.setOnLongClickListener(view -> {
            String info = mInfoTv.getText().toString();
            if (TextUtils.isEmpty(info)) {
                return true;
            }
            if (!info.startsWith(getString(R.string.str_image_info_flag))) {
                return true;
            }
            showBtn();
            return true;
        });
    }

    private void showBtn() {
        if (mIsBtnShow) {
            return;
        }
        if (mBtnLayout == null) {
            return;
        }
        mIsBtnShow = true;
        int delayTime = 0;
        for (int i = mBtnLayout.getChildCount() - 1; i >= 0; i--) {
            View view = mBtnLayout.getChildAt(i);
            delayTime += 100;
            ObjectAnimator animator = ObjectAnimator.ofFloat(view,
                    "translationX", ConvertUtils.dp2px(mBtnHideX), 0);
            animator.setDuration(300);
            animator.addListener(new SimpleAnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                    super.onAnimationStart(animation);
                    view.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                }
            });
            animator.setInterpolator(new DecelerateInterpolator());
            animator.setStartDelay(delayTime);
            animator.start();
        }
    }

    private void hideBtn() {
        if (!mIsBtnShow) {
            return;
        }
        mIsBtnShow = false;
        int delayTime = 0;
        for (int i = mBtnLayout.getChildCount() - 1; i >= 0; i--) {
            View view = mBtnLayout.getChildAt(i);
            delayTime += 100;
            ObjectAnimator animator = ObjectAnimator.ofFloat(view,
                    "translationX", 0, ConvertUtils.dp2px(mBtnHideX));
            animator.setDuration(300);
            animator.addListener(new SimpleAnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                    super.onAnimationStart(animation);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                }
            });
            animator.setInterpolator(new DecelerateInterpolator());
            animator.setStartDelay(delayTime);
            animator.start();
        }
    }

    private void selectPic() {
        requestStoragePermission();
    }

    private void requestStoragePermission() {
        PermissionGen.with(MainActivity.this)
                .addRequestCode(100)
                .permissions(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .request();
    }

    @PermissionSuccess(requestCode = 100)
    public void permissionSuccess() {
        openAlbum();
    }

    @PermissionFail(requestCode = 100)
    public void permissionFail() {
        Toast.makeText(this, R.string.str_storage_permission, Toast.LENGTH_SHORT).show();
    }

    /**
     * 打开相册
     */
    private void openAlbum() {
        Intent albumIntent = new Intent(Intent.ACTION_PICK);
        albumIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        startActivityForResult(albumIntent, REQUEST_ALBUM);
    }

    /**
     * 读取照片或者擦除信息出错
     */
    private void error() {
        hideBtn();
    }

    /**
     * 读取图片信息
     *
     * @param imagePath imagePath
     */
    private void readImageInfo(String imagePath) {
        ExifInterface mExifInterface;
        try {
            mExifInterface = new ExifInterface(imagePath);
        } catch (Exception e) {
            e.printStackTrace();
            mInfoTv.setText(e.getMessage());
            error();
            return;
        }

        // 设备型号，整形表示，在ExifInterface中有常量对应表示
        String model = mExifInterface.getAttribute(ExifInterface.TAG_MODEL);
        // 设备品牌
        String make = mExifInterface.getAttribute(ExifInterface.TAG_MAKE);
        // 软件
        String software = mExifInterface.getAttribute(ExifInterface.TAG_SOFTWARE);

        // 纬度
        String gpsLat = mExifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
        // 纬度名（N or S）
        String gpsLatRef = mExifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF);
        // 经度
        String gpsLon = mExifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);
        // 经度名（E or W）
        String gpsLonRef = mExifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF);
        String gpsDate = mExifInterface.getAttribute(ExifInterface.TAG_GPS_DATESTAMP);

        // 作者
        String artist = mExifInterface.getAttribute(ExifInterface.TAG_ARTIST);
        // 作者标记、说明、记录
        String makerNote = mExifInterface.getAttribute(ExifInterface.TAG_MAKER_NOTE);
        // 时间
        String dataTime = mExifInterface.getAttribute(ExifInterface.TAG_DATETIME);
        String dataTimeOriginal = mExifInterface.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL);
        String dataTimeDigitized = mExifInterface.getAttribute(ExifInterface.TAG_DATETIME_DIGITIZED);

        // 闪光灯
        String flash = mExifInterface.getAttribute(ExifInterface.TAG_FLASH);
        // 曝光时间
        String exposureTime = mExifInterface.getAttribute(ExifInterface.TAG_EXPOSURE_TIME);
        // 曝光补偿
        String exposureValue = mExifInterface.getAttribute(ExifInterface.TAG_EXPOSURE_BIAS_VALUE);
        // iso
        String isoSpeed = mExifInterface.getAttribute(ExifInterface.TAG_ISO_SPEED);
        String isoSpeedRatings = mExifInterface.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS);
        // 光圈值
        String aperture = mExifInterface.getAttribute(ExifInterface.TAG_APERTURE_VALUE);
        // 焦距
        String focalLength = mExifInterface.getAttribute(ExifInterface.TAG_FOCAL_LENGTH);
        // 白平衡
        String whiteBalance = mExifInterface.getAttribute(ExifInterface.TAG_WHITE_BALANCE);
        // 压缩比
        String compression = mExifInterface.getAttribute(ExifInterface.TAG_COMPRESSION);

        // 宽
        String width = mExifInterface.getAttribute(ExifInterface.TAG_IMAGE_WIDTH);
        // 高
        String length = mExifInterface.getAttribute(ExifInterface.TAG_IMAGE_LENGTH);
        // 方向
        String orientation = mExifInterface.getAttribute(ExifInterface.TAG_ORIENTATION);
        // exif版本
        String exifVersion = mExifInterface.getAttribute(ExifInterface.TAG_EXIF_VERSION);

        String textInfo;

        textInfo = getString(R.string.str_image_info_flag) + "\n"
                + "①长按文本显示操作按钮 ^_^" + "\n"
                + "②点击文本隐藏操作按钮 (*^__^*)" + "\n"
                + "------华丽的分割线------" + "\n"

                + "model：" + model + "\n"
                + "make：" + make + "\n"
                + "software：" + software + "\n"

                + "gpsLatRef：" + gpsLatRef + "\n"
                + "gpsLat：" + gpsLat + "\n"
                + "gpsLonRef：" + gpsLonRef + "\n"
                + "gpsLon：" + gpsLon + "\n"
                + "gpsDate：" + gpsDate + "\n"

                + "artist：" + artist + "\n"
                + "makerNote：" + makerNote + "\n"
                + "dataTime：" + dataTime + "\n"
                + "dataTimeOriginal：" + dataTimeOriginal + "\n"
                + "dataTimeDigitized：" + dataTimeDigitized + "\n"

                + "flash：" + flash + "\n"
                + "exposureTime：" + exposureTime + "\n"
                + "exposureValue：" + exposureValue + "\n"
                + "isoSpeed：" + isoSpeed + "\n"
                + "isoSpeedRatings：" + isoSpeedRatings + "\n"
                + "aperture：" + aperture + "\n"
                + "focalLength：" + focalLength + "\n"
                + "whiteBalance：" + whiteBalance + "\n"
                + "compression：" + compression + "\n"
                + "width：" + width + "\n"
                + "length：" + length + "\n"
                + "orientation：" + orientation + "\n"
                + "exifVersion：" + exifVersion + "\n"
                + "\n";

        mInfoTv.setText(textInfo);
    }

    /**
     * 清除照片GPS信息
     */
    private void clearImageGpsInfo() {
        ExifInterface exif;
        try {
            exif = new ExifInterface(mImagePath);

            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "null");
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, "0/0,0/0,0/0");

            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "null");
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, "0/0,0/0,0/0");

            exif.setAttribute(ExifInterface.TAG_GPS_DATESTAMP, "null");

            exif.saveAttributes();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.str_clear_gps_error, Toast.LENGTH_SHORT).show();
            mInfoTv.setText(e.getMessage());
            error();

            return;
        }

        // 清除成功，重新刷新图片信息
        readImageInfo(mImagePath);
        Toast.makeText(this, R.string.str_clear_gps_success, Toast.LENGTH_SHORT).show();
    }

    /**
     * 清除照片GPS信息
     */
    private void clearImageGpsInfo2() {
        try {
            JpegHeaders jpegHeaders = new JpegHeaders(mImagePath);
            JpegHeaders.preheat();
            jpegHeaders.convertToExif();
            App1Header exifHeader = jpegHeaders.getApp1Header();
            exifHeader.setValue(App1Header.Tag.GPSINFO, "0");
            jpegHeaders.save(true);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.str_clear_gps_error, Toast.LENGTH_SHORT).show();
            mInfoTv.setText(e.getMessage());
            error();

            return;
        }

        // 清除成功，重新刷新图片信息
        readImageInfo(mImagePath);
        Toast.makeText(this, R.string.str_clear_gps_success, Toast.LENGTH_SHORT).show();
    }

    /**
     * 清除照片时间信息
     */
    private void clearImageTimeInfo() {
        ExifInterface exif;
        try {
            exif = new ExifInterface(mImagePath);

            exif.setAttribute(ExifInterface.TAG_GPS_DATESTAMP, "null");
            exif.setAttribute(ExifInterface.TAG_DATETIME, "null");
            exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, "null");
            exif.setAttribute(ExifInterface.TAG_DATETIME_DIGITIZED, "null");

            exif.saveAttributes();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.str_clear_time_error, Toast.LENGTH_SHORT).show();
            mInfoTv.setText(e.getMessage());
            error();

            return;
        }

        // 清除成功，重新刷新图片信息
        readImageInfo(mImagePath);
        Toast.makeText(this, R.string.str_clear_time_success, Toast.LENGTH_SHORT).show();
    }

    /**
     * 清除照片设备信息
     */
    private void clearImageDeviceInfo() {
        ExifInterface exif;
        try {
            exif = new ExifInterface(mImagePath);

            exif.setAttribute(ExifInterface.TAG_MODEL, "null");
            exif.setAttribute(ExifInterface.TAG_MAKE, "null");
            exif.setAttribute(ExifInterface.TAG_SOFTWARE, "null");

            exif.saveAttributes();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.str_clear_device_error, Toast.LENGTH_SHORT).show();
            mInfoTv.setText(e.getMessage());
            error();

            return;
        }

        // 清除成功，重新刷新图片信息
        readImageInfo(mImagePath);
        Toast.makeText(this, R.string.str_clear_device_success, Toast.LENGTH_SHORT).show();
    }

    /**
     * 获取exif
     *
     * @param filepath filepath
     * @return ExifInterface
     */
    public static ExifInterface getExif(String filepath) {
        ExifInterface exif = null;
        try {
            exif = new ExifInterface(filepath);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return exif;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        PermissionGen.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data == null) {
            return;
        }
        if (resultCode == RESULT_OK && requestCode == REQUEST_ALBUM) {
            Uri sourceUri = data.getData();
            if (sourceUri == null) {
                return;
            }
            mImagePath = UriUtils.getPathFromUri(this, sourceUri);
            Glide.with(this).load(sourceUri).into(mImageView);

            readImageInfo(mImagePath);
        }
    }
}
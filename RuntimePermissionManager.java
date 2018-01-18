package cn.sanfast.zhuoer.student.manager;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.PermissionChecker;
import android.view.View;

import cn.sanfast.xmutils.dialog.CustomDialog;
import cn.sanfast.xmutils.utils.StringUtil;

/**
 * 运行时权限管理
 * Created by wzd on 2016/7/6.
 */
public class RuntimePermissionManager {

    private final String TAG = RuntimePermissionManager.class.getSimpleName();
    private final int REQUEST_CODE = 200;
    private OnPermissionCallback mCallback;
    private Activity mActivity;
    private String[] mPermissions;
    private String mRationale;

    public RuntimePermissionManager(Activity activity, String[] permissions, String rationale) {
        this.mActivity = activity;
        this.mPermissions = permissions;
        this.mRationale = rationale;
    }

    public void setOnPermissionCallback(OnPermissionCallback callback) {
        mCallback = callback;
    }

    /**
     * 申请权限
     */
    public void applyPermissions() {
        if (mPermissions == null || mPermissions.length == 0) {
            return;
        }
        // 检测
        if (!checkSelfPermission()) {
            // 无权限，需要申请
            requestPermission();
        } else {
            // 已拥有该权限
            if (mCallback != null) {
                mCallback.onAlreadyGranted();
            }
        }
    }

    /**
     * 检测是否已授权
     *
     * @return boolean
     */
    private boolean checkSelfPermission() {
        int targetSdkVersion;
        PackageInfo info = null;
        try {
            info = mActivity.getPackageManager().getPackageInfo(mActivity.getPackageName(), 0);
            targetSdkVersion = info.applicationInfo.targetSdkVersion;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            targetSdkVersion = 24;
        }

        // 检测权限
        boolean result = false;
        for (String permission : mPermissions) {
            if (!StringUtil.isEmpty(permission)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (targetSdkVersion >= Build.VERSION_CODES.M) {
                        result = ContextCompat.checkSelfPermission(mActivity, permission) == PackageManager.PERMISSION_GRANTED;
                        if(!result){
                            return result;
                        }
                    } else {
                        result = PermissionChecker.checkSelfPermission(mActivity, permission) == PackageManager.PERMISSION_GRANTED;
                        if(!result){
                            return result;
                        }
                    }
                } else {
                    result = true;
                }
            }
        }
        return result;
    }

    /**
     * 申请权限
     */
    private void requestPermission() {
        if (shouldShowRationale()) {
            // 先展示原因，再申请
            CustomDialog dialog = new CustomDialog(mActivity, CustomDialog.DIALOG_THEME_NO_TITLE);
            dialog.setMessage(mRationale);
            dialog.setButton(
                    "确定", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            for (String str : mPermissions) {
                            }
                            ActivityCompat.requestPermissions(mActivity, mPermissions, REQUEST_CODE);
                        }
                    },
                    "取消", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // 用户取消申请权限
                            if (mCallback != null) {
                                mCallback.onCancel();
                            }
                        }
                    }).show();
        } else {
            // 不展示原因，直接申请
            for (String str : mPermissions) {
            }
            ActivityCompat.requestPermissions(mActivity, mPermissions, REQUEST_CODE);
        }
    }

    /**
     * 是否需要展示申请原因
     *
     * @return boolean
     */
    private boolean shouldShowRationale() {
        for (String permission : mPermissions) {
            if (!StringUtil.isEmpty(permission)) {
                if (!ActivityCompat.shouldShowRequestPermissionRationale(mActivity, permission)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 检测是否申请成功
     *
     * @param grantResults int[]
     * @return boolean
     */
    private boolean verifyPermissions(int[] grantResults) {
        if (grantResults.length < 1) {
            return false;
        }
        // 检测每一个权限，都成功才成功
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * 系统申请回调
     */
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE) {
            if (verifyPermissions(grantResults)) {
                if (mCallback != null) {
                    mCallback.onGranted();
                }
            } else {
                if (mCallback != null) {
                    mCallback.onDenied();
                }
            }
        } else {
            if (mCallback != null) {
                mCallback.onDenied();
            }
        }
    }

    public interface OnPermissionCallback {

        public void onAlreadyGranted();

        public void onCancel();

        public void onGranted();

        public void onDenied();
    }
}

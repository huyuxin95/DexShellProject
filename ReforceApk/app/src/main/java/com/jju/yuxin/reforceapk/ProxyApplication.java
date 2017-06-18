package com.jju.yuxin.reforceapk;

import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.ArrayMap;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import dalvik.system.DexClassLoader;

/**
 * =============================================================================
 * Copyright (c) 2017 yuxin All rights reserved.
 * Packname com.jju.yuxin.reforceapk
 * Created by yuxin.
 * Created time 2017/6/18 0018 下午 5:03.
 * Version   1.0;
 * Describe :
 * History:
 * ==============================================================================
 */
public class ProxyApplication extends Application{

    private static final String appkey = "APPLICATION_CLASS_NAME";
    private  static final String TAG=ProxyApplication.class.getSimpleName();
    private String srcApkFilePath;
    private String odexPath;
    private String libPath;
    //以下是加载资源
    protected AssetManager mAssetManager;
    protected Resources mResources;
    protected Resources.Theme mTheme;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        Log.d(TAG,"----------onCreate");
        try {

            File odex = this.getDir("payload_odex", MODE_PRIVATE);
            File libs = this.getDir("payload_lib", MODE_PRIVATE);
            //用于存放源apk释放出来的dex
            odexPath = odex.getAbsolutePath();
            //用于存放源Apk用到的so文件
            libPath = libs.getAbsolutePath();
            //用于存放解密后的apk
            srcApkFilePath = odex.getAbsolutePath() + "/payload.apk";

            File srcApkFile = new File(srcApkFilePath);
            Log.i("demo", "apk size:"+srcApkFile.length());

            //第一次加载
            if (!srcApkFile.exists())
            {
                Log.i("demo", "isFirstLoading");
                srcApkFile.createNewFile();
                //拿到dex文件
                byte[] dexdata = this.readDexFileFromApk();
                //取出源APK解密后放置在/payload.apk，及其so文件放置在payload_lib/下
                this.splitPayLoadFromDex(dexdata);
            }

            // 配置动态加载环境
            //反射获取主线程对象，并从中获取所有已加载的package信息，并中找到当前的LoadApk对象的弱引用
            Object currentActivityThread = RefInvoke.invokeStaticMethod(
                    "android.app.ActivityThread", "currentActivityThread",
                    new Class[] {}, new Object[] {});
            String packageName = this.getPackageName();
            ArrayMap mPackages = (ArrayMap) RefInvoke.getFieldOjbect(
                    "android.app.ActivityThread", currentActivityThread,
                    "mPackages");
            WeakReference wr = (WeakReference) mPackages.get(packageName);

            //创建一个新的DexClassLoader用于加载源Apk，
            // 传入apk路径，dex释放路径，so路径，及父节点的DexClassLoader使其遵循双亲委托模型
            DexClassLoader dLoader = new DexClassLoader(srcApkFilePath, odexPath,
                    libPath, (ClassLoader) RefInvoke.getFieldOjbect(
                    "android.app.LoadedApk", wr.get(), "mClassLoader"));

            //getClassLoader()等同于 (ClassLoader) RefInvoke.getFieldOjbect()
            //但是为了替换掉父节点我们需要通过反射来获取并修改其值
            Log.i(TAG,"父classloader:"+(ClassLoader) RefInvoke.getFieldOjbect(
                    "android.app.LoadedApk", wr.get(), "mClassLoader"));
            //将父节点DexClassLoader替换
            RefInvoke.setFieldOjbect("android.app.LoadedApk", "mClassLoader",
                    wr.get(), dLoader);

            Log.i(TAG,"子classloader:"+dLoader);

            try{
                //尝试加载源Apk的MainActivity
                Object actObj = dLoader.loadClass("com.jju.yuxin.sourceproject.MainActivity");

                Log.i(TAG, "SrcApk_MainActivity:"+actObj);
            }catch(Exception e){
                Log.i(TAG, "LoadSrcActivityErr:"+Log.getStackTraceString(e));
            }


        } catch (Exception e) {
            Log.i(TAG, "error:"+Log.getStackTraceString(e));
            e.printStackTrace();
        }
    }


    public void onCreate() {

            //加载源apk资源
            //loadResources(srcApkFilePath);

            Log.i(TAG, "--------onCreate");
            //获取配置在清单文件的源Apk的Application路劲
            String appClassName = null;
            try {
                ApplicationInfo ai = this.getPackageManager()
                        .getApplicationInfo(this.getPackageName(),
                                PackageManager.GET_META_DATA);
                Bundle bundle = ai.metaData;
                if (bundle != null && bundle.containsKey("APPLICATION_CLASS_NAME")) {
                    appClassName = bundle.getString("APPLICATION_CLASS_NAME");//className 是配置在xml文件中的。
                } else {
                    Log.i(TAG, "have no application class name");
                    return;
                }
            } catch (PackageManager.NameNotFoundException e) {
                Log.i(TAG, "error:"+Log.getStackTraceString(e));
                e.printStackTrace();
            }

            //获取当前壳Apk的ApplicationInfo
            Object currentActivityThread = RefInvoke.invokeStaticMethod(
                    "android.app.ActivityThread", "currentActivityThread",
                    new Class[] {}, new Object[] {});
            Object mBoundApplication = RefInvoke.getFieldOjbect(
                    "android.app.ActivityThread", currentActivityThread,
                    "mBoundApplication");
            Object loadedApkInfo = RefInvoke.getFieldOjbect(
                    "android.app.ActivityThread$AppBindData",
                    mBoundApplication, "info");
            //将LoadedApk中的ApplicationInfo设置为null
            RefInvoke.setFieldOjbect("android.app.LoadedApk", "mApplication",
                    loadedApkInfo, null);

            //获取currentActivityThread中注册的Application
            Object oldApplication = RefInvoke.getFieldOjbect(
                    "android.app.ActivityThread", currentActivityThread,
                    "mInitialApplication");

            //获取ActivityThread中所有已注册的Application，并将当前壳Apk的Application从中移除
            ArrayList<Application> mAllApplications = (ArrayList<Application>) RefInvoke
                    .getFieldOjbect("android.app.ActivityThread",
                            currentActivityThread, "mAllApplications");
            mAllApplications.remove(oldApplication);

            ApplicationInfo appinfo_In_LoadedApk = (ApplicationInfo) RefInvoke
                    .getFieldOjbect("android.app.LoadedApk", loadedApkInfo,
                            "mApplicationInfo");

            ApplicationInfo appinfo_In_AppBindData = (ApplicationInfo) RefInvoke
                    .getFieldOjbect("android.app.ActivityThread$AppBindData",
                            mBoundApplication, "appInfo");
            //替换原来的Application
            appinfo_In_LoadedApk.className = appClassName;
            appinfo_In_AppBindData.className = appClassName;

            //注册Application
            Application app = (Application) RefInvoke.invokeMethod(
                    "android.app.LoadedApk", "makeApplication", loadedApkInfo,
                    new Class[] { boolean.class, Instrumentation.class },
                    new Object[] { false, null });

            //替换ActivityThread中的Application
            RefInvoke.setFieldOjbect("android.app.ActivityThread",
                    "mInitialApplication", currentActivityThread, app);

            ArrayMap mProviderMap = (ArrayMap) RefInvoke.getFieldOjbect(
                    "android.app.ActivityThread", currentActivityThread,
                    "mProviderMap");

            Iterator it = mProviderMap.values().iterator();
            while (it.hasNext()) {
                Object providerClientRecord = it.next();
                Object localProvider = RefInvoke.getFieldOjbect(
                        "android.app.ActivityThread$ProviderClientRecord",
                        providerClientRecord, "mLocalProvider");
                RefInvoke.setFieldOjbect("android.content.ContentProvider",
                        "mContext", localProvider, app);
            }

            Log.i(TAG, "Srcapp:"+app);

            app.onCreate();

    }


    private void splitPayLoadFromDex(byte[] shelldexdata) throws IOException {
        int sdlen = shelldexdata.length;
        //取被加壳apk的长度
        byte[] dexlen = new byte[4];
        System.arraycopy(shelldexdata, sdlen - 4, dexlen, 0, 4);
        ByteArrayInputStream bais = new ByteArrayInputStream(dexlen);
        DataInputStream in = new DataInputStream(bais);
        int readInt = in.readInt();
        Log.d(TAG,"Integer.toHexString(readInt):"+Integer.toHexString(readInt));

        //取出apk
        byte[] ensrcapk = new byte[readInt];
        System.arraycopy(shelldexdata, sdlen - 4 - readInt, ensrcapk, 0, readInt);

        //对源程序Apk进行解密
        byte[]  srcapk = decrypt(ensrcapk);

        //写入源apk文件
        File file = new File(srcApkFilePath);
        try {
            FileOutputStream localFileOutputStream = new FileOutputStream(file);
            localFileOutputStream.write(srcapk);
            localFileOutputStream.close();
        } catch (IOException localIOException) {
            throw new RuntimeException(localIOException);
        }

        //分析源apk文件
        ZipInputStream localZipInputStream = new ZipInputStream(
                new BufferedInputStream(new FileInputStream(file)));

        while (true) {
            ZipEntry localZipEntry = localZipInputStream.getNextEntry();
            if (localZipEntry == null) {
                localZipInputStream.close();
                break;
            }
            //依次取出被加壳apk用到的so文件，放到 libPath中（data/data/包名/payload_lib)
            String name = localZipEntry.getName();
            if (name.startsWith("lib/") && name.endsWith(".so")) {
                File storeFile = new File(libPath + "/"
                        + name.substring(name.lastIndexOf('/')));
                storeFile.createNewFile();
                FileOutputStream fos = new FileOutputStream(storeFile);
                byte[] arrayOfByte = new byte[1024];
                while (true) {
                    int i = localZipInputStream.read(arrayOfByte);
                    if (i == -1)
                        break;
                    fos.write(arrayOfByte, 0, i);
                }
                fos.flush();
                fos.close();
            }
            localZipInputStream.closeEntry();
        }
        localZipInputStream.close();
    }


    /**
     * 拿到自己apk文件中的dex文件
     * @return
     * @throws IOException
     */
    private byte[] readDexFileFromApk() throws IOException {
        ByteArrayOutputStream dexByteArrayOutputStream = new ByteArrayOutputStream();

        ZipInputStream localZipInputStream = new ZipInputStream(
                new BufferedInputStream(new FileInputStream(
                        this.getApplicationInfo().sourceDir)));

        while (true) {
            ZipEntry localZipEntry = localZipInputStream.getNextEntry();
            if (localZipEntry == null) {
                localZipInputStream.close();
                break;
            }
            //拿到dex文件
            if (localZipEntry.getName().equals("classes.dex")) {
                byte[] arrayOfByte = new byte[1024];
                while (true) {
                    int i = localZipInputStream.read(arrayOfByte);
                    if (i == -1)
                        break;
                    dexByteArrayOutputStream.write(arrayOfByte, 0, i);
                }
            }
            localZipInputStream.closeEntry();
        }
        localZipInputStream.close();
        return dexByteArrayOutputStream.toByteArray();
    }


    // //直接返回数据，读者可以添加自己解密方法
    private byte[] decrypt(byte[] srcdata) {
        for(int i=0;i<srcdata.length;i++){
            srcdata[i] = (byte)(0xFF ^ srcdata[i]);
        }
        return srcdata;
    }


    protected void loadResources(String srcApkPath) {
        //创建一个AssetManager放置源apk的资源
        try {
            AssetManager assetManager = AssetManager.class.newInstance();
            Method addAssetPath = assetManager.getClass().getMethod("addAssetPath", String.class);
            addAssetPath.invoke(assetManager, srcApkPath);
            mAssetManager = assetManager;
        } catch (Exception e) {
            Log.i(TAG, "inject:loadResource error:"+Log.getStackTraceString(e));
            e.printStackTrace();
        }
        Resources superRes = super.getResources();
        superRes.getDisplayMetrics();
        superRes.getConfiguration();
        mResources = new Resources(mAssetManager, superRes.getDisplayMetrics(),superRes.getConfiguration());
        mTheme = mResources.newTheme();
        mTheme.setTo(super.getTheme());
    }

    @Override
    public AssetManager getAssets() {
        return mAssetManager == null ? super.getAssets() : mAssetManager;
    }

    @Override
    public Resources getResources() {
        return mResources == null ? super.getResources() : mResources;
    }

    @Override
    public Resources.Theme getTheme() {
        return mTheme == null ? super.getTheme() : mTheme;
    }

}

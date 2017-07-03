package jp.ogapee.onscripter;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.graphics.Point;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.PowerManager;
import android.provider.DocumentsContract;
import android.provider.Settings.Secure;
import android.support.v4.provider.DocumentFile;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.vending.licensing.AESObfuscator;
import com.google.android.vending.licensing.APKExpansionPolicy;
import com.google.android.vending.licensing.LicenseChecker;
import com.google.android.vending.licensing.LicenseCheckerCallback;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Comparator;

public class ONScripter
        extends Activity
        implements AdapterView.OnItemClickListener, Runnable
{
    public static final byte[] SALT = new byte[]{2, 42, -12, -1, 54, 98,
            -100, -12, 43, 1, -8, -4, 9, 5, -106, -107, -33, 45, -2, 84
    };
    
    // Launcher contributed by katane-san
    
    private File mCurrentDirectory = null;
    private File mOldCurrentDirectory = null;
    private File[] mDirectoryFiles = null;
    private ListView listView = null;
    private int num_file = 0;
    private byte[] buf = null;
    private int screen_w, screen_h;
    private int top_left_x, top_left_y;
    private Button[] btn = new Button[9];
    private FrameLayout frame_layout = null;
    private LinearLayout layout1 = null;
    private LinearLayout layout1_1 = null;
    private LinearLayout layout1_2 = null;
    private LinearLayout layout2 = null;
    private LinearLayout layout2_1 = null;
    private LinearLayout layout2_2 = null;
    private int mButtonAlpha = 1;
    private static final int num_alpha = 4;
    private Uri ons_uri = null;
    
    static class FileSort
            implements Comparator<File>
    {
        public int compare(File src, File target)
        {
            return src.getName().compareTo(target.getName());
        }
    }
    
    private void setupDirectorySelector()
    {
        mDirectoryFiles = mCurrentDirectory.listFiles(new FileFilter()
        {
            public boolean accept(File file)
            {
                return (!file.isHidden() && file.isDirectory());
            }
        });
        
        Arrays.sort(mDirectoryFiles, new FileSort());
        
        int length = mDirectoryFiles.length;
        if (mCurrentDirectory.getParent() != null)
        {
            length++;
        }
        String[] names = new String[length];
        
        int j = 0;
        if (mCurrentDirectory.getParent() != null)
        {
            names[j++] = "..";
        }
        for (int i = 0; i < mDirectoryFiles.length; i++)
        {
            names[j++] = mDirectoryFiles[i].getName();
        }
        
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, names);
        
        listView.setAdapter(arrayAdapter);
        listView.setOnItemClickListener(this);
    }
    
    private void runLauncher()
    {
        boolean intent_flag = false;
        
        if (Build.VERSION.SDK_INT >= 21)
        {
            intent_flag = true;
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            try
            {
                startActivityForResult(i, 1);
            }
            catch (Exception e)
            {
                intent_flag = false;
            }
        }
        
        if (intent_flag == false)
        {
            mCurrentDirectory = new File(gCurrentDirectoryPath);
            if (mCurrentDirectory.exists() == false)
            {
                gCurrentDirectoryPath = Environment.getExternalStorageDirectory().getPath();
                mCurrentDirectory = new File(gCurrentDirectoryPath);
                
                if (mCurrentDirectory.exists() == false)
                {
                    showErrorDialog("Could not find SD card.");
                }
            }
            
            listView = new ListView(this);
            
            LinearLayout layoutH = new LinearLayout(this);
            
            checkRFO = new CheckBox(this);
            checkRFO.setText("Render Font Outline");
            checkRFO.setBackgroundColor(Color.rgb(244, 244, 255));
            checkRFO.setTextColor(Color.BLACK);
            checkRFO.setChecked(gRenderFontOutline);
            checkRFO.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
            {
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
                {
                    Editor e = getSharedPreferences("pref", MODE_PRIVATE).edit();
                    e.putBoolean("render_font_outline", isChecked);
                    e.commit();
                }
            });
            
            layoutH.addView(checkRFO, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,
                    LinearLayout.LayoutParams.FILL_PARENT, 1.0f));
            listView.addHeaderView(layoutH, null, false);
            setupDirectorySelector();
            setContentView(listView);
        }
    }
    
    public void onActivityResult(int requestCode, int resultCode, Intent resultData)
    {
        if (requestCode == 1 && resultCode == RESULT_OK)
        {
            gCurrentDirectoryPath = null;
            ons_uri = resultData.getData();
            
            if (ons_uri.getAuthority().equals("com.android.externalstorage.documents"))
            {
                String docId = DocumentsContract.getTreeDocumentId(ons_uri);
                String split[] = docId.split(":");
                if (split[0].equals("primary"))
                {
                    gCurrentDirectoryPath = Environment.getExternalStorageDirectory().getPath();
                    if (split.length > 1)
                    {
                        gCurrentDirectoryPath += "/" + split[1];
                    }
                }
                else
                {
                    File[] dirs = getExternalFilesDirs(null);
                    for (File dir : dirs)
                    {
                        String path = dir.getPath().substring(0, dir.getPath().indexOf("/Android/data"));
                        if (path.equals(Environment.getExternalStorageDirectory().getPath()))
                        {
                            continue;
                        }
                        if (split.length > 1)
                        {
                            path += "/" + split[1];
                        }
                        File file = new File(path);
                        if (file.exists() == false)
                        {
                            continue;
                        }
                        gCurrentDirectoryPath = path;
                        break;
                    }
                    if (gCurrentDirectoryPath == null)
                    {
                        gCurrentDirectoryPath = Environment.getExternalStorageDirectory().getPath();
                        if (split.length > 1)
                        {
                            gCurrentDirectoryPath += "/" + split[1];
                        }
                    }
                }
            }
            
            if (gCurrentDirectoryPath.isEmpty() == false)
            {
                mCurrentDirectory = new File(gCurrentDirectoryPath);
                runSDLApp();
            }
        }
    }
    
    public void onItemClick(AdapterView<?> parent, View v, int position, long id)
    {
        position--; // for header
        
        TextView textView = (TextView) v;
        mOldCurrentDirectory = mCurrentDirectory;
        
        if (textView.getText().equals(".."))
        {
            mCurrentDirectory = new File(mCurrentDirectory.getParent());
            gCurrentDirectoryPath = mCurrentDirectory.getPath();
        }
        else
        {
            if (mCurrentDirectory.getParent() != null)
            {
                position--;
            }
            gCurrentDirectoryPath = mDirectoryFiles[position].getPath();
            mCurrentDirectory = new File(gCurrentDirectoryPath);
        }
        
        mDirectoryFiles = mCurrentDirectory.listFiles(new FileFilter()
        {
            public boolean accept(File file)
            {
                return (file.isFile() &&
                        (file.getName().equals("0.txt") ||
                                file.getName().equals("00.txt") ||
                                file.getName().equals("nscr_sec.dat") ||
                                file.getName().equals("nscript.___") ||
                                file.getName().equals("nscript.dat")));
            }
        });
        
        if (mDirectoryFiles.length == 0)
        {
            setupDirectorySelector();
        }
        else
        {
            mDirectoryFiles = mCurrentDirectory.listFiles(new FileFilter()
            {
                public boolean accept(File file)
                {
                    return (file.isFile() &&
                            (file.getName().equals("default.ttf")));
                }
            });
            
            if (mDirectoryFiles.length == 0)
            {
                alertDialogBuilder.setTitle(getString(R.string.app_name));
                alertDialogBuilder.setMessage("default.ttf is missing.");
                alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int whichButton)
                    {
                        setResult(RESULT_OK);
                    }
                });
                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
                
                mCurrentDirectory = mOldCurrentDirectory;
                setupDirectorySelector();
            }
            else
            {
                gRenderFontOutline = checkRFO.isChecked();
                runSDLApp();
            }
        }
    }
    
    private void runDownloader()
    {
        String version_filename = getResources().getString(R.string.download_version);
        File file = new File(gCurrentDirectoryPath + "/" + version_filename);
        if (file.exists() == false)
        {
            progDialog = new ProgressDialog(this);
            progDialog.setCanceledOnTouchOutside(false);
            progDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progDialog.setMessage("Downloading archives from Internet:");
            progDialog.setOnKeyListener(new OnKeyListener()
            {
                public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event)
                {
                    if (KeyEvent.KEYCODE_SEARCH == keyCode || KeyEvent.KEYCODE_BACK == keyCode)
                    {
                        return true;
                    }
                    return false;
                }
            });
            progDialog.show();
            
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "ONScripter");
            wakeLock.acquire();
            
            runDownloaderSub();
        }
        else
        {
            runSDLApp();
        }
    }
    
    private void runDownloaderSub()
    {
        final String version_filename = getResources().getString(R.string.download_version);
        String url = getResources().getString(R.string.download_url);
        
        if (url.length() == 0)
        {
            String deviceId = Secure.getString(this.getContentResolver(), Secure.ANDROID_ID);
            
            final APKExpansionPolicy aep = new APKExpansionPolicy(this,
                    new AESObfuscator(SALT, this.getPackageName(), deviceId));
            
            aep.resetPolicy();
            
            // let's try and get the OBB file from LVL first
            // Construct the LicenseChecker with a Policy.
            mChecker = new LicenseChecker(this, aep, getResources().getString(R.string.public_key));
            mChecker.checkAccess(new LicenseCheckerCallback()
            {
                @Override
                public void allow(int reason)
                {
                    int count = aep.getExpansionURLCount();
                    if (count > 0)
                    {
                        String zip_dir = Environment.getExternalStorageDirectory() + "/Android/obb/" +
                                getApplicationContext().getPackageName();
                        String zip_filename = aep.getExpansionFileName(APKExpansionPolicy.MAIN_FILE_URL_INDEX);
                        String url = aep.getExpansionURL(APKExpansionPolicy.MAIN_FILE_URL_INDEX);
                        long file_size = aep.getExpansionFileSize(APKExpansionPolicy.MAIN_FILE_URL_INDEX);
                        
                        downloader = new jp.ogapee.onscripter.DataDownloader(zip_dir, zip_filename,
                                gCurrentDirectoryPath, version_filename, url, file_size, handler);
                    }
                }
                
                @Override
                public void dontAllow(int reason)
                {
                    Log.e("ONS", "runDownloaderSub: checkAccess dontAllow.");
                }
                
                @Override
                public void applicationError(int errorCode)
                {
                    Log.e("ONS", "runDownloaderSub: checkAccess applicationError.");
                }
            });
        }
        else
        {
            String zip_dir = gCurrentDirectoryPath;
            String zip_filename = url.substring(url.lastIndexOf("/") + 1);
            
            downloader = new jp.ogapee.onscripter.DataDownloader(zip_dir, zip_filename, gCurrentDirectoryPath,
                    version_filename, url, -1, handler);
        }
    }
    
    private void runCopier()
    {
        File file = new File(gCurrentDirectoryPath + "/" + getResources().getString(R.string.download_version));
        if (file.exists() == false)
        {
            progDialog = new ProgressDialog(this);
            progDialog.setCanceledOnTouchOutside(false);
            progDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progDialog.setMessage("Copying archives: ");
            progDialog.setOnKeyListener(new OnKeyListener()
            {
                public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event)
                {
                    if (KeyEvent.KEYCODE_SEARCH == keyCode || KeyEvent.KEYCODE_BACK == keyCode)
                    {
                        return true;
                    }
                    return false;
                }
            });
            progDialog.show();
            
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "ONScripter");
            wakeLock.acquire();
            
            new Thread(this).start();
        }
        else
        {
            runSDLApp();
        }
    }
    
    @Override
    public void run()
    {
        num_file = 0;
        buf = new byte[8192 * 2];
        
        copyRecursive("");
        
        File file = new File(gCurrentDirectoryPath + "/" + getResources().getString(R.string.download_version));
        try
        {
            file.createNewFile();
        }
        catch (Exception e)
        {
            sendMessage(-2, 0, "Failed to create version file: " + e.toString());
        }
        ;
        
        sendMessage(-1, 0, null);
    }
    
    private void copyRecursive(String path)
    {
        AssetManager as = getResources().getAssets();
        try
        {
            File file = new File(gCurrentDirectoryPath + "/" + path);
            if (!file.exists())
            {
                file.mkdir();
            }
            
            String[] file_list = as.list(path);
            for (String str : file_list)
            {
                InputStream is = null;
                String path2 = path;
                if (!path.equals(""))
                {
                    path2 += "/";
                }
                path2 += str;
                
                int total_size = 0;
                try
                {
                    is = as.open(path2);
                    AssetFileDescriptor afd = as.openFd(path2);
                    total_size = (int) afd.getLength();
                    afd.close();
                }
                catch (Exception e)
                {
                    copyRecursive(path2);
                    is = null;
                }
                if (is == null)
                {
                    continue;
                }
                
                File dst_file = new File(gCurrentDirectoryPath + "/" + path2);
                BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(dst_file));
                
                num_file++;
                int len = is.read(buf);
                int total_read = 0;
                while (len >= 0)
                {
                    if (len > 0)
                    {
                        os.write(buf, 0, len);
                    }
                    total_read += len;
                    sendMessage(total_read, total_size, "Copying archives: " + num_file);
                    
                    len = is.read(buf);
                    try
                    {
                        Thread.sleep(1);
                    }
                    catch (InterruptedException e)
                    {
                    }
                }
                os.flush();
                os.close();
                is.close();
            }
        }
        catch (Exception e)
        {
            progDialog.dismiss();
            sendMessage(-2, 0, "Failed to write: " + e.toString());
        }
    }
    
    private void runSDLApp()
    {
        nativeInitJavaCallbacks();
        
        mAudioThread = new jp.ogapee.onscripter.AudioThread(this);
        mGLView = new jp.ogapee.onscripter.DemoGLSurfaceView(this);
        mGLView.setFocusableInTouchMode(true);
        mGLView.setFocusable(true);
        mGLView.requestFocus();
        
        int game_width = nativeGetWidth();
        int game_height = nativeGetHeight();
        
        Display disp = ((WindowManager) this.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int dw = disp.getWidth();
        int dh = disp.getHeight();
        
        if (Build.VERSION.SDK_INT >= 19)
        {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View
                    .SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
            Point p = new Point(0, 0);
            disp.getRealSize(p);
            dw = p.x;
            dh = p.y;
        }
        
        frame_layout = new FrameLayout(this);
        
        // Main display
        layout1 = new LinearLayout(this);
        layout1_1 = new LinearLayout(this);
        layout1_2 = new LinearLayout(this);
        
        // Button
        layout2 = new LinearLayout(this);
        layout2_1 = new LinearLayout(this);
        layout2_2 = new LinearLayout(this);
        
        screen_w = dw;
        screen_h = dh;
        if (dw * game_height >= dh * game_width)
        {
            screen_w = (dh * game_width / game_height) & (~0x01); // to be 2 bytes aligned
        }
        else
        {
            screen_h = dw * game_height / game_width;
            layout1.setOrientation(LinearLayout.VERTICAL);
        }
        
        for (int i = 0; i < 9; i++)
        {
            btn[i] = new Button(this);
            if (i < 6)
            {
                btn[i].setMinWidth(dw / 6);
                btn[i].setMinHeight(dh / 6);
                btn[i].setWidth(dw / 6);
                btn[i].setHeight(dh / 6);
            }
            else
            {
                btn[i].setVisibility(View.INVISIBLE);
            }
        }
        
        btn[0].setText(getResources().getString(R.string.button_lclick));
        btn[0].setOnClickListener(new OnClickListener()
        {
            public void onClick(View v)
            {
                nativeKey(KeyEvent.KEYCODE_ENTER, 1);
                nativeKey(KeyEvent.KEYCODE_ENTER, 0);
            }
        });
        
        btn[1].setText(getResources().getString(R.string.button_rclick));
        btn[1].setOnClickListener(new OnClickListener()
        {
            public void onClick(View v)
            {
                nativeKey(KeyEvent.KEYCODE_BACK, 1);
                nativeKey(KeyEvent.KEYCODE_BACK, 0);
            }
        });
        
        btn[2].setText(getResources().getString(R.string.button_left));
        btn[2].setOnClickListener(new OnClickListener()
        {
            public void onClick(View v)
            {
//				nativeKey( KeyEvent.KEYCODE_DPAD_LEFT, 1 );
//				nativeKey( KeyEvent.KEYCODE_DPAD_LEFT, 0 );
                nativeKeyExtra(KeyEvent.KEYCODE_TAB, 1, 6);
                nativeKeyExtra(KeyEvent.KEYCODE_TAB, 0, 6);
            }
        });
        
        btn[3].setText(getResources().getString(R.string.button_right));
        btn[3].setOnClickListener(new OnClickListener()
        {
            public void onClick(View v)
            {
//				nativeKey( KeyEvent.KEYCODE_DPAD_RIGHT, 1 );
//				nativeKey( KeyEvent.KEYCODE_DPAD_RIGHT, 0 );
                nativeKeyExtra(KeyEvent.KEYCODE_TAB, 1, 106);
                nativeKeyExtra(KeyEvent.KEYCODE_TAB, 0, 106);
            }
        });
        
        btn[4].setText(getResources().getString(R.string.button_up));
        btn[4].setOnClickListener(new OnClickListener()
        {
            public void onClick(View v)
            {
                nativeKey(KeyEvent.KEYCODE_DPAD_UP, 1);
                nativeKey(KeyEvent.KEYCODE_DPAD_UP, 0);
            }
        });
        
        btn[5].setText(getResources().getString(R.string.button_down));
        btn[5].setOnClickListener(new OnClickListener()
        {
            public void onClick(View v)
            {
                nativeKey(KeyEvent.KEYCODE_DPAD_DOWN, 1);
                nativeKey(KeyEvent.KEYCODE_DPAD_DOWN, 0);
            }
        });
        
        layout1_1.addView(btn[6]);
        layout1_2.addView(btn[7]);
        top_left_x = (dw - screen_w) / 2;
        top_left_y = (dh - screen_h) / 2;
        layout1.addView(layout1_1, 0, new LinearLayout.LayoutParams(top_left_x, top_left_y));
        layout1.addView(mGLView, 1, new LinearLayout.LayoutParams(screen_w, screen_h));
        layout1.addView(layout1_2, 2);
        
        layout2_1.addView(btn[8]);
        for (int i = 0; i < 6; i++)
        {
            layout2_2.addView(btn[i], i);
        }
        layout2.addView(layout2_1, 0);
        layout2.addView(layout2_2, 1);
        layout2.setVisibility(View.INVISIBLE);
        
        resetLayout();
        
        frame_layout.addView(layout1);
        frame_layout.addView(layout2);
        frame_layout.addView(createMenuLayout(frame_layout));
        
        setContentView(frame_layout);
        
        gd = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener()
        {
            public boolean onFling(MotionEvent e1, MotionEvent e2, float x, float y)
            {
                float x2 = x * x, y2 = y * y;
                float thresh = 500.0f;
                if (x2 + y2 < thresh * thresh)
                {
                    return false;
                }
                
                AlphaAnimation fade;
                boolean change = true;
                if (button_state == 0)
                {
                    layout2.setVisibility(View.VISIBLE);
                    if (Math.abs(x) > Math.abs(y))
                    {
                        if (x > 0.0)
                        {
                            button_state = 1;
                        }
                        else
                        {
                            button_state = 3;
                        }
                    }
                    else
                    {
                        if (y > 0.0)
                        {
                            button_state = 2;
                        }
                        else
                        {
                            button_state = 4;
                        }
                    }
                    fade = new AlphaAnimation(0, 1);
                }
                else
                {
                    change = false;
                    if (Math.abs(x) > Math.abs(y))
                    {
                        if (x > 0.0 && button_state == 3 ||
                                x < 0.0 && button_state == 1)
                        {
                            change = true;
                        }
                    }
                    else
                    {
                        if (y > 0.0 && button_state == 4 ||
                                y < 0.0 && button_state == 2)
                        {
                            change = true;
                        }
                    }
                    fade = new AlphaAnimation(1, 0);
                    if (change)
                    {
                        layout2.setVisibility(View.INVISIBLE);
                    }
                }
                
                if (change)
                {
                    fade.setDuration(200);
                    layout2.startAnimation(fade);
                }
                
                resetLayout();
                return true;
            }
        });
        
        if (wakeLock == null)
        {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "ONScripter");
            wakeLock.acquire();
        }
    }
    
    public void resetLayout()
    {
        Display disp = ((WindowManager) this.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int dw = disp.getWidth();
        int dh = disp.getHeight();
        
        if (Build.VERSION.SDK_INT >= 19)
        {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View
                    .SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
            Point p = new Point(0, 0);
            disp.getRealSize(p);
            dw = p.x;
            dh = p.y;
        }
        
        int game_width = nativeGetWidth();
        int game_height = nativeGetHeight();
        screen_w = dw;
        screen_h = dh;
        if (dw * game_height >= dh * game_width)
        {
            screen_w = (dh * game_width / game_height) & (~0x01); // to be 2 bytes aligned
        }
        else
        {
            screen_h = dw * game_height / game_width;
        }
        
        for (int i = 0; i < 6; i++)
        {
            btn[i].setAlpha((mButtonAlpha + 1) * 0.2f);
        }
        
        if (button_state == 1 || button_state == 3)
        {
            layout2.setOrientation(LinearLayout.HORIZONTAL);
            layout2_2.setOrientation(LinearLayout.VERTICAL);
        }
        else
        {
            layout2.setOrientation(LinearLayout.VERTICAL);
            layout2_2.setOrientation(LinearLayout.HORIZONTAL);
        }
        
        if (button_state == 1 || button_state == 2)
        {
            layout2.updateViewLayout(layout2_1, new LinearLayout.LayoutParams(0, 0));
        }
        else if (button_state == 3)
        {
            layout2.updateViewLayout(layout2_1, new LinearLayout.LayoutParams(dw - dw / 6, dh));
        }
        else if (button_state == 4)
        {
            layout2.updateViewLayout(layout2_1, new LinearLayout.LayoutParams(dw, dh - dh / 6));
        }
        
        if (layout2.getVisibility() == View.INVISIBLE)
        {
            button_state = 0;
        }
    }
    
    public void playVideo(char[] filename)
    {
        try
        {
            String filename2 = "file://" + gCurrentDirectoryPath + "/" + new String(filename);
            filename2 = filename2.replace('\\', '/');
            Log.v("ONS", "playVideo: " + filename2);
            Uri uri = Uri.parse(filename2);
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(uri, "video/*");
            startActivityForResult(i, -1);
        }
        catch (Exception e)
        {
            Log.e("ONS", "playVideo error:  " + e.getClass().getName());
        }
    }
    
    public int getFD(char[] filename, int mode)
    {
        ParcelFileDescriptor pfd;
        try
        {
            if (mode == 0)
            {
                String filename2 = gCurrentDirectoryPath + "/" + new String(filename);
                filename2 = filename2.replace('\\', '/');
                pfd = ParcelFileDescriptor.open(new File(filename2), ParcelFileDescriptor.MODE_READ_ONLY);
            }
            else if (Build.VERSION.SDK_INT >= 21)
            {
                String path = new String(filename);
                String paths[] = path.split("\\/");
                
                DocumentFile df = DocumentFile.fromTreeUri(this, ons_uri);
                for (int i = 0; i < paths.length - 1; i++)
                {
                    df = df.findFile(paths[i]);
                }
                
                DocumentFile df2 = df.findFile(paths[paths.length - 1]);
                if (df2 != null && df2.exists())
                {
                    df2.delete();
                }
                
                df2 = df.createFile("application/octet-stream", paths[paths.length - 1]);
                pfd = getContentResolver().openFileDescriptor(df2.getUri(), "w");
            }
            else
            {
                String filename2 = gCurrentDirectoryPath + "/" + new String(filename);
                filename2 = filename2.replace('\\', '/');
                pfd = ParcelFileDescriptor.open(new File(filename2), ParcelFileDescriptor.MODE_WRITE_ONLY);
            }
        }
        catch (Exception e)
        {
            //Log.e("ONS", "getFD error:  " + e.getClass().getName());
            return -1;
        }
        return pfd.detachFd();
    }
    
    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        // fullscreen mode
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        gCurrentDirectoryPath = Environment.getExternalStorageDirectory() + "/Android/data/" + getApplicationContext
                ().getPackageName();
        alertDialogBuilder = new AlertDialog.Builder(this);
        
        SharedPreferences sp = getSharedPreferences("pref", MODE_PRIVATE);
        mButtonAlpha = sp.getInt("button_alpha", getResources().getInteger(R.integer.button_alpha));
        gRenderFontOutline = sp.getBoolean("render_font_outline", getResources().getBoolean(R.bool
                .render_font_outline));
        
        if (getResources().getBoolean(R.bool.use_launcher))
        {
            gCurrentDirectoryPath = Environment.getExternalStorageDirectory() + "/ons";
            runLauncher();
        }
        else if (getResources().getBoolean(R.bool.use_download))
        {
            runDownloader();
        }
        else
        {
            runCopier();
        }
    }
    
    @Override
    public boolean onTouchEvent(final MotionEvent event)
    {
        // TODO: add multitouch support (added in Android 2.0 SDK)
        int action = -1;
        if (event.getAction() == MotionEvent.ACTION_DOWN)
        {
            action = 0;
        }
        if (event.getAction() == MotionEvent.ACTION_UP)
        {
            action = 1;
        }
        if (event.getAction() == MotionEvent.ACTION_MOVE)
        {
            action = 2;
        }
        
        if (gd.onTouchEvent(event))
        {
            action = -1;
        }
        
        if (action >= 0)
        {
            nativeMouse((int) event.getX() - top_left_x, (int) event.getY() - top_left_y, action);
        }
        
        return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        super.onPrepareOptionsMenu(menu);
        
        if (mGLView != null)
        {
            menu.clear();
            menu.add(Menu.NONE, Menu.FIRST, 0, getResources().getString(R.string.menu_automode));
            menu.add(Menu.NONE, Menu.FIRST + 1, 0, getResources().getString(R.string.menu_skip));
            menu.add(Menu.NONE, Menu.FIRST + 2, 0, getResources().getString(R.string.menu_speed));
            
            SubMenu sm = menu.addSubMenu(getResources().getString(R.string.menu_settings));
            SubMenu sm_alpha = sm.addSubMenu(getResources().getString(R.string.menu_button_alpha));
            for (int i = 0; i < num_alpha; i++)
            {
                MenuItem mi = sm_alpha.add(1, Menu.FIRST + 3 + i, i, String.valueOf((i + 1) * 20) + "%");
                if (i == mButtonAlpha)
                {
                    mi.setChecked(true);
                }
            }
            sm_alpha.setGroupCheckable(1, true, true);
            
            if (gRenderFontOutline)
            {
                sm.add(Menu.NONE, Menu.FIRST + num_alpha + 3, 0, getResources().getString(R.string
                        .menu_hide_font_outline));
            }
            else
            {
                sm.add(Menu.NONE, Menu.FIRST + num_alpha + 4, 0, getResources().getString(R.string
                        .menu_show_font_outline));
            }
            
            sm.add(Menu.NONE, Menu.FIRST + num_alpha + 5, 0, getResources().getString(R.string.menu_version));
            menu.add(Menu.NONE, Menu.FIRST + num_alpha + 6, 0, getResources().getString(R.string.menu_quit));
        }
        
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == Menu.FIRST)
        {
            nativeKey(KeyEvent.KEYCODE_A, 1);
            nativeKey(KeyEvent.KEYCODE_A, 0);
        }
        else if (item.getItemId() == Menu.FIRST + 1)
        {
            nativeKey(KeyEvent.KEYCODE_S, 1);
            nativeKey(KeyEvent.KEYCODE_S, 0);
        }
        else if (item.getItemId() == Menu.FIRST + 2)
        {
            nativeKey(KeyEvent.KEYCODE_O, 1);
            nativeKey(KeyEvent.KEYCODE_O, 0);
        }
        else if (item.getItemId() >= Menu.FIRST + 3 && item.getItemId() < Menu.FIRST + 3 + num_alpha)
        {
            mButtonAlpha = item.getItemId() - (Menu.FIRST + 3);
            item.setChecked(true);
            resetLayout();
        }
        else if (item.getItemId() == Menu.FIRST + num_alpha + 3)
        {
            gRenderFontOutline = true;
        }
        else if (item.getItemId() == Menu.FIRST + num_alpha + 4)
        {
            gRenderFontOutline = false;
        }
        else if (item.getItemId() == Menu.FIRST + num_alpha + 5)
        {
            alertDialogBuilder.setTitle(getResources().getString(R.string.menu_version));
            alertDialogBuilder.setMessage(getResources().getString(R.string.version));
            alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int whichButton)
                {
                    setResult(RESULT_OK);
                }
            });
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        }
        else if (item.getItemId() == Menu.FIRST + num_alpha + 6)
        {
            nativeKey(KeyEvent.KEYCODE_MENU, 2); // send SDL_QUIT
        }
        else
        {
            return false;
        }
        
        Editor e = getSharedPreferences("pref", MODE_PRIVATE).edit();
        e.putInt("button_alpha", mButtonAlpha);
        e.putBoolean("render_font_outline", gRenderFontOutline);
        e.commit();
        
        return true;
    }
    
    @Override
    public boolean onKeyDown(int keyCode, final KeyEvent event)
    {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
        {
            AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            int volume = audio.getStreamVolume(AudioManager.STREAM_MUSIC) + (keyCode == KeyEvent.KEYCODE_VOLUME_UP ?
                    1 : (-1));
            if (volume >= 0 && volume <= audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC))
            {
                audio.setStreamVolume(AudioManager.STREAM_MUSIC, volume, AudioManager.FLAG_SHOW_UI);
            }
            return true;
        }
        
        if (keyCode == KeyEvent.KEYCODE_MENU)
        {
            super.onKeyDown(keyCode, event);
            return false;
        }
        nativeKey(keyCode, 1);
        
        return true;
    }
    
    @Override
    public boolean onKeyUp(int keyCode, final KeyEvent event)
    {
        if (keyCode == KeyEvent.KEYCODE_MENU)
        {
            super.onKeyUp(keyCode, event);
            return false;
        }
        nativeKey(keyCode, 0);
        return true;
    }
    
    @Override
    protected void onPause()
    {
        nativeKey(0, 3); // send SDL_ACTIVEEVENT
        // TODO: if application pauses it's screen is messed up
        if (wakeLock != null)
        {
            wakeLock.release();
        }
        super.onPause();
        if (mGLView != null)
        {
            mGLView.onPause();
        }
        if (mAudioThread != null)
        {
            mAudioThread.onPause();
        }
    }
    
    @Override
    protected void onResume()
    {
        if (wakeLock != null && !wakeLock.isHeld())
        {
            wakeLock.acquire();
        }
        super.onResume();
        if (mGLView != null)
        {
            mGLView.onResume();
        }
        if (mAudioThread != null)
        {
            mAudioThread.onResume();
        }
        nativeKey(0, 3); // send SDL_ACTIVEEVENT
    }
    
    @Override
    protected void onStop()
    {
        super.onStop();
        if (mGLView != null)
        {
            mGLView.onStop();
        }
    }
    
    @Override
    protected void onDestroy()
    {
        if (mGLView != null)
        {
            mGLView.exitApp();
        }
        if (mChecker != null)
        {
            mChecker.onDestroy();
        }
        super.onDestroy();
    }
    
    final Handler handler = new Handler()
    {
        public void handleMessage(Message msg)
        {
            int current = msg.getData().getInt("current");
            if (current == -1)
            {
                progDialog.dismiss();
                runSDLApp();
            }
            else if (current == -2)
            {
                progDialog.dismiss();
                showErrorDialog(msg.getData().getString("message"));
            }
            else
            {
                progDialog.setMessage(msg.getData().getString("message"));
                int total = msg.getData().getInt("total");
                if (total != progDialog.getMax())
                {
                    progDialog.setMax(total);
                }
                progDialog.setProgress(current);
            }
        }
    };
    
    private void showErrorDialog(String mes)
    {
        alertDialogBuilder.setTitle("Error");
        alertDialogBuilder.setMessage(mes);
        alertDialogBuilder.setPositiveButton("Quit", new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int whichButton)
            {
                finish();
            }
        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
    
    public void sendMessage(int current, int total, String str)
    {
        Message msg = handler.obtainMessage();
        Bundle b = new Bundle();
        b.putInt("total", total);
        b.putInt("current", current);
        b.putString("message", str);
        msg.setData(b);
        handler.sendMessage(msg);
    }
    
    public static final int[] MENU_IDS = new int[]{R.id.lookback, R.id.skip, R.id.windowearse, R.id.automode, R.id.reset, R.id.end};
    public static final int[] MENU_EVENTS = new int[]{52, 51, 53, 54, 55, 56};
    
    
    private View showMenuView;
    private View menuLayout;
    private View subMenuLayout;
    private FlowLayout saveLayout;
    private FlowLayout loadLayout;
    
    
    //添加自定义菜单
    private View createMenuLayout(ViewGroup parent)
    {
        View root = getLayoutInflater().inflate(R.layout.layout_menu, parent, false);
        
        showMenuView = root.findViewById(R.id.show_menu);
        showMenuView.setOnClickListener(onClickListener);
        
        menuLayout = root.findViewById(R.id.menu_layout);
        for(int i = 0; i < MENU_IDS.length; i ++)
        {
            View menuItemView = menuLayout.findViewById(MENU_IDS[i]);
            menuItemView.setOnClickListener(onClickMenuListener);
            menuItemView.setTag(i);
        }
        menuLayout.findViewById(R.id.save).setOnClickListener(onClickListener);
        menuLayout.findViewById(R.id.load).setOnClickListener(onClickListener);
        
        subMenuLayout = root.findViewById(R.id.sub_menu_layout);
        saveLayout = (FlowLayout) root.findViewById(R.id.save_layout);
        loadLayout = (FlowLayout) root.findViewById(R.id.load_layout);
        
        int subMenuSize = 20;
        for(int i = 0; i < subMenuSize; i ++)
        {
            Button button = new Button(this);
            button.setText("save_" + (i + 1));
            button.setTag(i);
            button.setOnClickListener(onClickSubMenuSaveListener);
            saveLayout.addView(button, new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
    
        for(int i = 0; i < subMenuSize; i ++)
        {
            Button button = new Button(this);
            button.setText("load_" + ( i + 1));
            button.setTag(i);
            button.setOnClickListener(onClickSubMenuLoadListener);
            loadLayout.addView(button, new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        
        return root;
    }
    
    private OnClickListener onClickListener = new OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            switch (v.getId())
            {
                case R.id.show_menu:
                    toggleMenu();
                    break;
                case R.id.save:
                    showSaveLayout();
                    break;
                case R.id.load:
                    showLoadLayout();
                    break;
            }
        }
    };
    
    private void toggleMenu()
    {
        if (menuLayout.getVisibility() == View.VISIBLE)
        {
            menuLayout.setVisibility(View.GONE);
            subMenuLayout.setVisibility(View.GONE);
        }
        else
        {
            menuLayout.setVisibility(View.VISIBLE);
            subMenuLayout.setVisibility(View.GONE);
        }
    
        nativeKey(KeyEvent.KEYCODE_BACK, 1);
        nativeKey(KeyEvent.KEYCODE_BACK, 0);
    }
    
    private void showLoadLayout()
    {
        if(subMenuLayout.getVisibility() == View.VISIBLE && loadLayout.getVisibility() == View.VISIBLE)
        {
            subMenuLayout.setVisibility(View.GONE);
        }
        else
        {
            subMenuLayout.setVisibility(View.VISIBLE);
            loadLayout.setVisibility(View.VISIBLE);
            saveLayout.setVisibility(View.GONE);
        }
    }
    
    private void showSaveLayout()
    {
        if (subMenuLayout.getVisibility() == View.VISIBLE && saveLayout.getVisibility() == View.VISIBLE)
        {
            subMenuLayout.setVisibility(View.GONE);
        }
        else
        {
            subMenuLayout.setVisibility(View.VISIBLE);
            saveLayout.setVisibility(View.VISIBLE);
            loadLayout.setVisibility(View.GONE);
        }
    }
    
    private void hideMenu()
    {
        menuLayout.setVisibility(View.GONE);
        subMenuLayout.setVisibility(View.GONE);
    }
    
    private OnClickListener onClickMenuListener = new OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            toggleMenu();
            
            int index = (Integer)v.getTag();
    
            nativeKeyExtra(KeyEvent.KEYCODE_TAB, 1, MENU_EVENTS[index]);
            nativeKeyExtra(KeyEvent.KEYCODE_TAB, 0, MENU_EVENTS[index]);
        }
    };
    
    
    private OnClickListener onClickSubMenuSaveListener = new OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            int index = (Integer)v.getTag();
    
            nativeKeyExtra(KeyEvent.KEYCODE_TAB, 1, index);
            nativeKeyExtra(KeyEvent.KEYCODE_TAB, 0, index);
            
            hideMenu();
        }
    };
    
    private OnClickListener onClickSubMenuLoadListener = new OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            int index = (Integer)v.getTag();
    
            nativeKeyExtra(KeyEvent.KEYCODE_TAB, 1, 100 + index);
            nativeKeyExtra(KeyEvent.KEYCODE_TAB, 0, 100 + index);
            
            hideMenu();
        }
    };
    
    
    private jp.ogapee.onscripter.DemoGLSurfaceView mGLView = null;
    private jp.ogapee.onscripter.AudioThread mAudioThread = null;
    private PowerManager.WakeLock wakeLock = null;
    public static String gCurrentDirectoryPath;
    public static boolean gRenderFontOutline;
    public static CheckBox checkRFO = null;
    
    private native int nativeInitJavaCallbacks();
    
    private native int nativeGetWidth();
    
    private native int nativeGetHeight();
    
    private native void nativeMouse(int x, int y, int action);
    
    private native void nativeKey(int keyCode, int down);
    
    private native void nativeKeyExtra(int keyCode, int down, int extra);
    
    private jp.ogapee.onscripter.DataDownloader downloader = null;
    private AlertDialog.Builder alertDialogBuilder = null;
    private ProgressDialog progDialog = null;
    private LicenseChecker mChecker = null;
    private GestureDetector gd;
    private int button_state = 0;
    
    static
    {
        System.loadLibrary("mad");
        System.loadLibrary("bz2");
        System.loadLibrary("tremor");
        System.loadLibrary("lua");
        System.loadLibrary("sdl");
        System.loadLibrary("sdl_mixer");
        System.loadLibrary("sdl_image");
        System.loadLibrary("sdl_ttf");
        System.loadLibrary("smpeg");
        System.loadLibrary("application");
        System.loadLibrary("sdl_main");
    }
}

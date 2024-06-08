package com.kenny.downloadapp;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final String MAGISK_URL = "https://github.com/HuskyDG/magisk-files/releases/download/1707294287/app-release.apk";
    private static final String MAGISK_NAME = "app-release.apk";
    private static final String[] APK_URLS = {
            MAGISK_URL,
            "https://example.com/yourapp2.apk",
            "https://example.com/yourapp3.apk",
            "https://example.com/yourapp4.apk",
            "https://example.com/yourapp5.apk"
    };
    private static final String[] APK_NAMES = {
            MAGISK_NAME,
            "yourapp2.apk",
            "yourapp3.apk",
            "yourapp4.apk",
            "yourapp5.apk"
    };

    private ProgressDialog progressDialog;
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button button1 = findViewById(R.id.button1);
        Button button2 = findViewById(R.id.button2);
        Button button3 = findViewById(R.id.button3);
        Button button4 = findViewById(R.id.button4);
        Button button5 = findViewById(R.id.button5);

        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadAndInstallAPK(APK_URLS[0], APK_NAMES[0]);
            }
        });

        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadAndInstallAPK(APK_URLS[1], APK_NAMES[1]);
            }
        });

        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadAndInstallAPK(APK_URLS[2], APK_NAMES[2]);
            }
        });

        button4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadAndInstallAPK(APK_URLS[3], APK_NAMES[3]);
            }
        });

        button5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadAndInstallAPK(APK_URLS[4], APK_NAMES[4]);
            }
        });
    }

    private boolean checkPermissions() {
        int internetPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET);
        int writePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        return internetPermission == PackageManager.PERMISSION_GRANTED && writePermission == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.INTERNET,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        }, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permissions Granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permissions Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void downloadAndInstallAPK(String apkUrl, String apkName) {
        if (!checkPermissions()) {
            requestPermissions();
            return;
        }

        handler.post(new Runnable() {
            @Override
            public void run() {
                progressDialog = new ProgressDialog(MainActivity.this);
                progressDialog.setMessage("Downloading " + apkName);
                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                progressDialog.setIndeterminate(true);
                progressDialog.setCancelable(false);
                progressDialog.show();
            }
        });

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    File file = new File(getExternalFilesDir(null), apkName);
                    downloadFile(apkUrl, file);

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog.dismiss();
                        }
                    });

                    moveFileToTmpAndInstall(file);

                } catch (IOException e) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog.dismiss();
                            Toast.makeText(MainActivity.this, "Download Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }

    private void downloadFile(String apkUrl, File file) throws IOException {
        URL url = new URL(apkUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.connect();

        int fileLength = connection.getContentLength();

        InputStream input = new BufferedInputStream(connection.getInputStream());
        FileOutputStream output = new FileOutputStream(file);

        byte[] data = new byte[1024];
        int total = 0;
        int count;
        while ((count = input.read(data)) != -1) {
            total += count;
            output.write(data, 0, count);
            final int finalTotal = total;
            handler.post(new Runnable() {
                @Override
                public void run() {
                    progressDialog.setProgress(finalTotal);
                }
            });
        }

        output.flush();
        output.close();
        input.close();
    }

    private void moveFileToTmpAndInstall(File file) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String filePath = file.getAbsolutePath();
                    Log.d("MoveFile", "Moving APK to /data/local/tmp: " + filePath);

                    String command = "mv " + filePath + " /data/local/tmp/" + file.getName();
                    Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", command});

                    // 打印输出和错误流
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

                    StringBuilder output = new StringBuilder();
                    StringBuilder errors = new StringBuilder();
                    String line;

                    // 读取标准输出流
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                    }

                    // 读取错误输出流
                    while ((line = errorReader.readLine()) != null) {
                        errors.append(line).append("\n");
                    }

                    process.waitFor();
                    int exitValue = process.exitValue();

                    Log.d("MoveFile", "Output: " + output.toString());
                    Log.d("MoveFile", "Errors: " + errors.toString());

                    if (exitValue == 0) {
                        File movedFile = new File("/data/local/tmp", file.getName());
                        installAPKWithRoot(movedFile);
                    } else {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "Move Failed with exit code " + exitValue + "\nErrors: " + errors.toString(), Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Move Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }

    private void installAPKWithRoot(File file) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String filePath = file.getAbsolutePath();
                    Log.d("InstallAPK", "Installing APK from: " + filePath);
                    if (!file.exists()) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "APK file not found at " + filePath, Toast.LENGTH_SHORT).show();
                            }
                        });
                        return;
                    }

                    String command = "pm install -r " + filePath;
                    Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", command});

                    // 打印输出和错误流
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

                    StringBuilder output = new StringBuilder();
                    StringBuilder errors = new StringBuilder();
                    String line;

                    // 读取标准输出流
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                    }

                    // 读取错误输出流
                    while ((line = errorReader.readLine()) != null) {
                        errors.append(line).append("\n");
                    }

                    int exitValue = process.waitFor();

                    Log.d("InstallAPK", "Output: " + output.toString());
                    Log.d("InstallAPK", "Errors: " + errors.toString());

                    if (exitValue == 0) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "Installation Successful", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "Installation Failed with exit code " + exitValue + "\nErrors: " + errors.toString(), Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Installation Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }
}

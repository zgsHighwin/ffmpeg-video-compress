package yunfucloud.video;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.File;

import yunfucloud.video.videocompress.CompressListener;
import yunfucloud.video.videocompress.Compressor;
import yunfucloud.video.videocompress.InitListener;

/**
 * -preset：指定编码的配置。x264编码算法有很多可供配置的参数，不同的参数值会导致编码的速度大相径庭，甚至可能影响质量。
 * 为了免去用户了解算法，然后手工配置参数的麻烦。x264提供了一些预设值，而这些预设值可以通过preset指定。
 * 这些预设值有包括：ultrafast，superfast，veryfast，faster，fast，medium，slow，slower，veryslow和placebo。ultrafast编码速度最快，
 * 但压缩率低，生成的文件更大，placebo则正好相反。x264所取的默认值为medium。需要说明的是，preset主要是影响编码的速度，
 * 并不会很大的影响编码出来的结果的质量。压缩高清电影时，我一般用slow或者slower，当你的机器性能很好时也可以使用veryslow，
 * 不过一般并不会带来很大的好处。
 * <p>
 * -crf：这是最重要的一个选项，用于指定输出视频的质量，取值范围是0-51，默认值为23，数字越小输出视频的质量越高。这个选项会直接影响到输出视频的码率。
 * 一般来说，压制480p我会用20左右，压制720p我会用16-18，1080p我没尝试过。个人觉得，一般情况下没有必要低于16。最好的办法是大家可以多尝试几个值，
 * 每个都压几分钟，看看最后的输出质量和文件大小，自己再按需选择。
 */
public class MainActivity extends AppCompatActivity {

    private String currentInputVideoPath = "";
    private String currentOutputVideoPath = getPath();
    private Compressor mCompressor;

    private String TAG = MainActivity.class.getSimpleName();

    String cmd = "-y -i " + currentInputVideoPath + " -strict -2 -vcodec libx264 -preset ultrafast " +
            "-crf 24 -acodec aac -ar 44100 -ac 2 -b:a 96k -s 640x480 -aspect 16:9 " + currentOutputVideoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mCompressor = new Compressor(this);

        mCompressor.loadBinary(new InitListener() {
            @Override
            public void onLoadSuccess() {
                Log.v(TAG, "load library succeed");
            }

            @Override
            public void onLoadFail(String reason) {
                Log.i(TAG, "load library fail:" + reason);
            }
        });
    }

    public void click(View v) {
        Intent intent = VideoUtil.startSystemCameraVideo();
        startActivityForResult(intent, 001);
    }

    public String getPath() {
        File myVideo = new File(Environment.getExternalStorageDirectory(), "MyVideo");
        if (!myVideo.isDirectory() && !myVideo.exists()) {
            myVideo.mkdir();
        }
        File file = new File(myVideo, "compress" + ".mp4");

        return file.getAbsolutePath();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 001) {
            if (resultCode == RESULT_OK) {
                String path = VideoUtil.getPath(data.getDataString());
                currentInputVideoPath = path;
                refreshCurrentPath();
            }
        }
    }

    private void refreshCurrentPath() {
        cmd = "-y -i " + currentInputVideoPath + " -strict -2 -vcodec libx264 -preset ultrafast " +
                "-crf 24 -acodec aac -ar 44100 -ac 2 -b:a 96k -s 480x320 -aspect 16:9 " + currentOutputVideoPath;
    }

    public void compress(View v) {
        String command = cmd;
        if (TextUtils.isEmpty(command)) {
            Toast.makeText(this, "请输入命令"
                    , Toast.LENGTH_SHORT).show();
        } else if (TextUtils.isEmpty(currentInputVideoPath)) {
            Toast.makeText(this, "视频文件不存在，请先录制视频", Toast.LENGTH_SHORT).show();
        } else {
            File file = new File(currentOutputVideoPath);
            if (file.exists()) {
                file.delete();
            }
            execCommand(command);
        }

    }

    private void execCommand(String command) {
        File mFile = new File(currentOutputVideoPath);
        if (mFile.exists()) {
            mFile.delete();
        }
        mCompressor.execCommand(cmd, new CompressListener() {
            @Override
            public void onExecSuccess(String message) {
                Log.i(TAG, "success " + message);
                Toast.makeText(getApplicationContext(), "压缩成功", Toast.LENGTH_SHORT).show();
                Log.d("MainActivity", "\tcurrentInputVideoPath\t" + getFileSize(currentInputVideoPath) + "\tcurrentOutputVideoPath\t" + getFileSize(currentOutputVideoPath));
            }

            @Override
            public void onExecFail(String reason) {
                Log.i(TAG, "fail " + reason);
                Log.d(TAG, "压缩失败");
            }

            @Override
            public void onExecProgress(String message) {
                Log.d(TAG, "正在压缩中");
            }
        });
    }


    /**
     * 获取文件大小
     *
     * @param path
     * @return
     */
    public static String getFileSize(String path) {
        File f = new File(path);
        if (!f.exists()) {
            return "0 MB";
        } else {
            long size = f.length();
            return (size / 1024f) / 1024f + "MB";
        }
    }
}

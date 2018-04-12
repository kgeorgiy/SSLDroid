package hu.blint.ssldroid.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Environment;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import hu.blint.ssldroid.Log;
import hu.blint.ssldroid.R;

public class FileChooser {
    private static final FilenameFilter FILE_FILTER = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String filename) {
            return !filename.startsWith(".");
        }
    };

    private final String title;
    private final Context context;
    private final Listener listener;

    public FileChooser(String title, Context context, Listener listener) {
        this.title = title;
        this.context = context;
        this.listener = listener;
    }

    //pick a file from /sdcard, courtesy of ConnectBot
    public void pickFileSimple() {
        // build list of all files in sdcard root
        final File sdcard = Environment.getExternalStorageDirectory();
        Log.d("SD Card location: "+sdcard.toString());

        // Don't show a dialog if the SD card is completely absent.
        final String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)
                && !Environment.MEDIA_MOUNTED.equals(state)) {
            show(new AlertDialog.Builder(context).setMessage(R.string.fileChooser_sdCardAbsent));
        } else {
            showFiles(sdcard, sdcard);
        }
    }

    private List<File> getSortedFiles(File url) {
        File[] files = url.listFiles(FILE_FILTER);
        if (files == null) {
            return Collections.emptyList();
        }
        Arrays.sort(files);
        return new ArrayList<File>(Arrays.asList(files));
    }

    private void showFiles(final File root, final File directory) {
        final List<File> files = getSortedFiles(directory);
        if (files.isEmpty()) {
            showDirectoryDialog(root, directory, new AlertDialog.Builder(context).setMessage(R.string.fileChooser_emptyDirectory));
            return;
        }

        final List<String> items = new ArrayList<String>();
        for (File file : files) {
            if (file.isDirectory()) {
                items.add(context.getString(R.string.fileChooser_dirName_format, file.getName()));
            } else if (file.isFile()) {
                items.add(context.getString(R.string.fileChooser_fileName_format, file.getName()));
            }
        }

        // prompt user to select any file from the sdcard root
        AlertDialog.Builder dialog = new AlertDialog.Builder(context)
                .setItems(items.toArray(new CharSequence[0]), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int pos) {
                        File file = files.get(pos);
                        if (file.isDirectory()) {
                            showFiles(root, file);
                        } else if (file.isFile()) {
                            listener.fileChosen(file);
                        }
                    }
                });
        showDirectoryDialog(root, directory, dialog);
    }

    private void showDirectoryDialog(final File root, final File directory, AlertDialog.Builder dialog) {
        if (!root.equals(directory)) {
            //create a Back button (shouldn't go above base URL)
            dialog.setNeutralButton(R.string.fileChooser_back, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int pos) {
                    showFiles(root, directory.getParentFile());
                }
            });
        }
        show(dialog);
    }

    private void show(AlertDialog.Builder dialog) {
        dialog.setTitle(title).setNegativeButton(android.R.string.cancel, null).create().show();
    }

    public interface Listener {
        void fileChosen(File file);
    }
}

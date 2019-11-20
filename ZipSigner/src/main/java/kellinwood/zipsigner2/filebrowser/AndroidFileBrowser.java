package kellinwood.zipsigner2.filebrowser;

import android.app.ListActivity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import kellinwood.zipsigner2.AlertDialogUtil;
import kellinwood.zipsigner2.R;

/** Based on the Android File Browser 2.0 tutorial and source (http://www.anddev.org/android_filebrowser__v20-t101.html)
 *  Icons obtained from the Tango Icon Gallery (http://tango.freedesktop.org/Tango_Icon_Gallery).
 */
public class AndroidFileBrowser extends ListActivity {

    public static final String DATA_KEY_START_PATH = "startPath";
    public static final String DATA_KEY_REASON = "reason";
    public static final String DATA_KEY_DIRECTORY_SELECT_MODE = "dirSelect";


    protected static final int SUB_ACTIVITY_REQUEST_CODE = 1337;

    private List<IconifiedText> directoryEntries = new ArrayList<IconifiedText>();
    private File currentDirectory = new File("/");

    private String reason = "select a file";
    private static final File[] emptyDirFiles = new File[0];
    private Pattern imagePattern;
    private Pattern audioPattern;
    private Pattern packagePattern;
    private Pattern htmlPattern;

    boolean directorySelectionMode = false;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setTheme(android.R.style.Theme_Black);

        imagePattern = Pattern.compile( getString(R.string.fileEndingImage));
        audioPattern = Pattern.compile( getString(R.string.fileEndingAudio));
        packagePattern = Pattern.compile( getString(R.string.fileEndingPackage));
        htmlPattern = Pattern.compile( getString(R.string.fileEndingWebText));

        Intent i = getIntent();
        String startPath = i.getExtras().getString(DATA_KEY_START_PATH);
        if (startPath == null) startPath = "/";
        reason = i.getExtras().getString(DATA_KEY_REASON);
        if (reason == null) reason = "";

        directorySelectionMode = i.getExtras().getBoolean(DATA_KEY_DIRECTORY_SELECT_MODE,false);



        browseTo(new File(startPath));
        if (directoryEntries.size() > 0) this.setSelection(0);

        if (directorySelectionMode) {
            ListView lv = getListView();

            lv.setOnItemLongClickListener( new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> av, View v, int pos, long id) {
                    return onLongListItemClick(v,pos,id);
                }
            });

            AlertDialogUtil.alertDialog(this,
                R.string.BrowserDirectoryModeTitle,
                R.string.BrowserDirectoryModeMessage,
                R.string.OkButtonLabel);
        }
    }

    /**
     * This function browses to the 
     * root-directory of the file-system.
     */
    private void browseToRoot() {
        browseTo(new File("/"));
    }

    /**
     * This function browses up one level 
     * according to the field: currentDirectory
     */
    private void upOneLevel(){
        if(this.currentDirectory.getParent() != null)
            this.browseTo(this.currentDirectory.getParentFile());
    }

    // Comparator that will order directory entries with folders first, then files, all sorted case-insensitively.
    static class FileSorter implements Comparator<File> {

        @Override
        public int compare(File o1, File o2) {
            if (o1.isDirectory() && !o2.isDirectory()) return -1;
            else if (!o1.isDirectory() && o2.isDirectory()) return 1;
            else return o1.getName().compareToIgnoreCase(o2.getName());
        }
        
    }
    
    private static FileSorter fileSorter = new FileSorter();
    
    private void browseTo(final File aDirectory){

        if (aDirectory.isDirectory()) 
        {
            setTitle(aDirectory.getAbsolutePath() + " :: " + getString(R.string.app_name) + " - " + reason);            
            File[] fileArray = aDirectory.listFiles();
            if (fileArray == null) fileArray = emptyDirFiles;
            this.currentDirectory = aDirectory;
            
            Set<File> files = new TreeSet<File>( fileSorter);
            for (File f : fileArray) files.add(f);
            fill( files);
        }
        else {
            openFile(aDirectory);
        }
    }

    private void openFile(File aFile)
    {
        try {
            Intent e = new Intent();
            e.setData(Uri.parse("file://"+ aFile.getAbsolutePath()));
            setResult( RESULT_OK, e);
            finish();            
        }
        catch (Exception x) {
            Toast.makeText(getBaseContext(), x.getClass().getName() + ": " + x.getMessage(), Toast.LENGTH_LONG).show(); 
        }
    }

    private void fill(Set<File> files) {
        this.directoryEntries.clear();

        String currentDirectoryName = currentDirectory.getAbsolutePath();
        if (!currentDirectoryName.equals("/") && !currentDirectoryName.endsWith("/"))
        {
            currentDirectoryName = currentDirectoryName + "/";
        }
        int currentDirectoryNameLen = currentDirectoryName.length();

        // Add the "." == "current directory"
        this.directoryEntries.add(new IconifiedText(
                getString(R.string.current_dir), 
                getResources().getDrawable(R.drawable.folder)));		
        // and the ".." == 'Up one level'
        if(this.currentDirectory.getParent() != null)
            this.directoryEntries.add(new IconifiedText(
                    getString(R.string.up_one_level), 
                    getResources().getDrawable(R.drawable.uponelevel)));

        Drawable currentIcon = null;
        for (File currentFile : files) {
            if (currentFile.getName().startsWith(".")) continue; // ignore hidden files and directories.
            if (currentFile.isDirectory()) {
                currentIcon = getResources().getDrawable(R.drawable.folder);
            }
            else if (!directorySelectionMode) {
                String fileName = currentFile.getName().toLowerCase();
                /* Determine the Icon to be used, 
                 * depending on the FileEndings defined in:
                 * res/values/fileendings.xml. */
                if (imagePattern.matcher( fileName).find()) {
                    currentIcon = getResources().getDrawable(R.drawable.image); 
                }
                else if (htmlPattern.matcher( fileName).find()) { 
                    currentIcon = getResources().getDrawable(R.drawable.webtext);
                }
                else if (packagePattern.matcher( fileName).find()) { 
                    currentIcon = getResources().getDrawable(R.drawable.packed);
                }
                else if (audioPattern.matcher( fileName).find()) {
                    currentIcon = getResources().getDrawable(R.drawable.audio);
                }
                else{
                    currentIcon = getResources().getDrawable(R.drawable.text);
                }				
            }
            else continue;

            /* Cut the current-path at the beginning */
            String currentFileName = currentFile.getAbsolutePath().substring(currentDirectoryNameLen);
            if (currentFile.isDirectory() && !currentFileName.endsWith("/"))
                currentFileName = currentFileName + "/";
            this.directoryEntries.add(new IconifiedText( currentFileName, currentIcon));

        }

        IconifiedTextListAdapter itla = new IconifiedTextListAdapter(this);
        itla.setListItems(this.directoryEntries);		
        this.setListAdapter(itla);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        int selectionRowID = position; // (int) this.getSelectedItemPosition();
        String selectedFileString = this.directoryEntries.get(selectionRowID).getText();
        if (selectedFileString.equals(getString(R.string.current_dir))) {
            // Refresh
            this.browseTo(this.currentDirectory);
        } else if(selectedFileString.equals(getString(R.string.up_one_level))){
            this.upOneLevel();
        } else {
            File clickedFile = new File( new File(this.currentDirectory.getAbsolutePath()),
                    this.directoryEntries.get(selectionRowID).getText());
            if(clickedFile != null)
                this.browseTo(clickedFile);
        }
    }

    protected boolean onLongListItemClick(View v,int position, long id) {
        int selectionRowID = position; // (int) this.getSelectedItemPosition();
        String selectedFileString = this.directoryEntries.get(selectionRowID).getText();
        if (selectedFileString.equals(getString(R.string.current_dir))) {
            openFile(this.currentDirectory);
        } else if(selectedFileString.equals(getString(R.string.up_one_level))){
            // Ignore
        } else {
            File clickedFile = new File( new File(this.currentDirectory.getAbsolutePath()),
                this.directoryEntries.get(selectionRowID).getText());
            if(clickedFile != null)
                openFile(clickedFile);
        }
        return true;
    }

    /** Checks whether checkItsEnd ends with 
     * one of the Strings from fileEndings */
    private boolean checkEndsWithInStringArray(String checkItsEnd, 
            String[] fileEndings){
        for(String aEnd : fileEndings){
            if(checkItsEnd.endsWith(aEnd))
                return true;
        }
        return false;
    }


}
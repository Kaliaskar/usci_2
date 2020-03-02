package kz.bsbnb.usci.report.model;

import kz.bsbnb.usci.model.persistence.Persistable;
import kz.bsbnb.usci.report.exception.ReportException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FileDownload extends Persistable {
    private final ArrayList<String> entryNames = new ArrayList<String>();
    private final ArrayList<File> files = new ArrayList<File>();
    private String downloadedFilename;
    private File zippedFile;
    private File REPORT_FILES_FOLDER;

    public FileDownload(String downloadedFilename, File REPORT_FILES_FOLDER) {
        this.downloadedFilename = downloadedFilename;
        this.REPORT_FILES_FOLDER = REPORT_FILES_FOLDER;
    }

    public FileDownload(String downloadedFilename, String entryName, File file,  File REPORT_FILES_FOLDER) {
        this.downloadedFilename = downloadedFilename;
        entryNames.add(entryName);
        files.add(file);
        this.REPORT_FILES_FOLDER = REPORT_FILES_FOLDER;
    }

    public void zipFile() {
        FileOutputStream fos = null;
        try {
            if (zippedFile == null) {
                final File zipFile = File.createTempFile("Records", ".zip", REPORT_FILES_FOLDER);
                fos = new FileOutputStream(zipFile);
                ZipOutputStream zos = new ZipOutputStream(fos);
                for (int i = 0; i < files.size(); i++) {
                    zos.putNextEntry(new ZipEntry(entryNames.get(i)));
                    FileInputStream fis = null;
                    try {
                        fis = new FileInputStream(files.get(i));
                        byte[] buf = new byte[10000];
                        int read = 1;
                        while (true) {
                            read = fis.read(buf);
                            if (read > 0) {
                                zos.write(buf, 0, read);
                            } else {
                                break;
                            }
                        }
                    } finally {
                        if (fis != null) {
                            fis.close();
                        }
                    }
                    zos.closeEntry();
                }
                zos.close();

                zippedFile = zipFile;
            }

        } catch (IOException ioe) {
            throw new ReportException("Ошибка" + ioe);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (Exception e) {
                }
            }
        }
    }

    public List<String> getFilenames() {
        return new ArrayList<String>(entryNames);
    }

    public File getZippedFile() {
        return zippedFile;
    }

    public List<File> getFiles() {
        return new ArrayList<File>(files);
    }

    public void addFile(String filename, File file) {
        entryNames.add(filename);
        files.add(file);
    }

    public void addAllFiles(Collection<String> filenames, Collection<File> files) {
        if (filenames.size() != files.size()) {
            throw new ReportException("Ошибка");
        }
        this.entryNames.addAll(filenames);
        this.files.addAll(files);
    }

    public String getDownloadedFileName() {
        return downloadedFilename;
    }

    public void setDownloadedFileName(String downloadedFileName) {
        this.downloadedFilename = downloadedFileName;
    }

    public ReportLoadFile getLoadedFile() {
        ReportLoadFile result = new ReportLoadFile();
        result.setFilename(getDownloadedFileName());
        result.setMimeType("application/zip");
        result.setPath(zippedFile.getAbsolutePath());
        return result;
    }
}

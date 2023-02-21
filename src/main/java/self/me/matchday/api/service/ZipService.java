package self.me.matchday.api.service;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Service
public class ZipService {

    private static final int BUFFER_SIZE = 1024;

    private static void inflateFile(ZipInputStream zis, @NotNull File file) throws IOException {

        // fix for Windows-created archives
        final File parent = file.getParentFile();
        if (!parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("Could not inflate directory: " + parent);
        }

        final byte[] buffer = new byte[BUFFER_SIZE];
        try (final FileOutputStream fos = new FileOutputStream(file)) {
            int data;
            while ((data = zis.read(buffer)) > 0) {
                fos.write(buffer, 0, data);
            }
        }
    }

    private static void zipAllFiles(@NotNull ZipOutputStream zos, File @NotNull ... files)
            throws IOException {

        for (final File file : files) {
            if (file.isDirectory()) {
                // recursively zip directory contents
                final File[] subFiles = file.listFiles();
                if (subFiles != null && subFiles.length > 0) {
                    zipAllFiles(zos, subFiles);
                }
            } else {
                addFileToZip(zos, file);
            }
        }
    }

    private static void addFileToZip(@NotNull ZipOutputStream zos, @NotNull File file)
            throws IOException {

        zos.putNextEntry(new ZipEntry(file.getPath()));
        try (final FileInputStream fis = new FileInputStream(file)) {
            final byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while((read = fis.read(buffer)) > 0) {
                zos.write(buffer,0, read);
            }
        }
    }

    public void zipFiles(@NotNull String archive, File... files) throws IOException {

        try (final FileOutputStream fos = new FileOutputStream(archive);
             final ZipOutputStream zos = new ZipOutputStream(fos)
             ) {
            zipAllFiles(zos, files);
        }
    }

    public void unzipArchive(@NotNull String archive, @NotNull String output) throws IOException {

        try (final ZipInputStream zis = new ZipInputStream(new FileInputStream(archive))) {

            final File outputDir = new File(output);
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                final File file = createFile(outputDir, zipEntry);
                if (zipEntry.isDirectory()) {
                    // create sub-dirs
                    if (!file.isDirectory() && !file.mkdirs()) {
                        throw new IOException("Could not inflate directory: " + file);
                    }
                } else {
                    inflateFile(zis, file);
                }
                zipEntry = zis.getNextEntry();
            }
        }
    }

    private @NotNull File createFile(@NotNull File output, @NotNull ZipEntry zipEntry)
            throws IOException {

        final File file = new File(output, zipEntry.getName());
        String outputPath = output.getCanonicalPath();
        String filePath = file.getCanonicalPath();
        if (!filePath.startsWith(outputPath)) {
            throw new IOException("File is not in zip path: " + filePath);
        }
        return file;
    }
}
